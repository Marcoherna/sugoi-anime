package cl.sugoianime.msauth.service;

import cl.sugoianime.msauth.domain.Consentimiento;
import cl.sugoianime.msauth.domain.Perfil;
import cl.sugoianime.msauth.domain.RefreshToken;
import cl.sugoianime.msauth.domain.Usuario;
import cl.sugoianime.msauth.dto.LoginRequest;
import cl.sugoianime.msauth.dto.RegistroRequest;
import cl.sugoianime.msauth.dto.TokenResponse;
import cl.sugoianime.msauth.dto.UsuarioDTO;
import cl.sugoianime.msauth.exception.AuthException;
import cl.sugoianime.msauth.config.JwtProperties;
import cl.sugoianime.msauth.repository.RefreshTokenRepository;
import cl.sugoianime.msauth.repository.UsuarioRepository;
import cl.sugoianime.msauth.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ConsentimientoService consentimientoService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(UsuarioRepository usuarioRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       ConsentimientoService consentimientoService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.consentimientoService = consentimientoService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    // =================================================================
    // RF-08: registro de cuentas
    // =================================================================
    @Transactional
    public TokenResponse registrar(RegistroRequest request, String ipOrigen) {

        String email = request.email().toLowerCase().trim();

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw AuthException.emailYaRegistrado();
        }

        Usuario usuario = Usuario.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .nombre(request.nombre().trim())
                .build();

        // RF-09: toda cuenta nace con un perfil por defecto,
        // para que el usuario pueda usar la plataforma de inmediato.
        usuario.agregarPerfil(Perfil.builder()
                .nombre(request.nombre().trim().split(" ")[0])
                .avatar("default")
                .build());

        usuario = usuarioRepository.save(usuario);

        // ---- Ley 19.628: evidencia del consentimiento ----
        consentimientoService.registrar(usuario,
                Consentimiento.Tipo.TERMINOS_Y_CONDICIONES, true, ipOrigen);

        consentimientoService.registrar(usuario,
                Consentimiento.Tipo.TRATAMIENTO_DATOS, true, ipOrigen);

        // Este es opcional: se registra tal como lo decidio el usuario.
        consentimientoService.registrar(usuario,
                Consentimiento.Tipo.HISTORIAL_RECOMENDACIONES,
                Boolean.TRUE.equals(request.aceptaRecomendaciones()), ipOrigen);

        log.info("Cuenta registrada: id={}", usuario.getId());

        return emitirTokens(usuario);
    }

    // =================================================================
    // RF-08: inicio de sesion
    // =================================================================
    @Transactional
    public TokenResponse login(LoginRequest request) {

        Usuario usuario = usuarioRepository
                .findByEmailIgnoreCase(request.email().toLowerCase().trim())
                .orElseThrow(AuthException::credencialesInvalidas);

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            // Nunca registres el email ni la contrasena intentada en el log.
            log.warn("Intento de login fallido para el usuario id={}", usuario.getId());
            throw AuthException.credencialesInvalidas();
        }

        if (!usuario.isActivo()) {
            throw AuthException.cuentaDesactivada();
        }

        usuario.setUltimoAcceso(OffsetDateTime.now());

        return emitirTokens(usuario);
    }

    // =================================================================
    // Renovacion del access token
    // =================================================================
    @Transactional
    public TokenResponse refrescar(String tokenRecibido) {

        RefreshToken refresh = refreshTokenRepository.findByToken(tokenRecibido)
                .orElseThrow(AuthException::refreshTokenInvalido);

        if (!refresh.esValido()) {
            throw AuthException.refreshTokenInvalido();
        }

        // Rotacion: el token usado se revoca y se emite uno nuevo.
        // Si un atacante roba un refresh token y la victima lo usa
        // despues, el robado ya no sirve.
        refresh.setRevocado(true);

        return emitirTokens(refresh.getUsuario());
    }

    // =================================================================
    // RF-08: cierre de sesion
    // =================================================================
    @Transactional
    public void logout(String tokenRecibido) {
        refreshTokenRepository.findByToken(tokenRecibido)
                .ifPresent(rt -> rt.setRevocado(true));
    }

    @Transactional
    public void logoutTodosLosDispositivos(Long usuarioId) {
        int revocados = refreshTokenRepository.revocarTodosDelUsuario(usuarioId);
        log.info("Sesiones cerradas para el usuario id={}: {}", usuarioId, revocados);
    }

    // =================================================================
    // LEY 19.628: derecho de eliminacion
    // =================================================================
    @Transactional
    public void eliminarCuenta(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(AuthException::usuarioNoEncontrado);

        // ON DELETE CASCADE arrastra perfiles, consentimientos y tokens.
        usuarioRepository.delete(usuario);

        log.info("Cuenta eliminada por solicitud del titular: id={}", usuarioId);
    }

    @Transactional(readOnly = true)
    public UsuarioDTO obtenerPerfilPropio(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(UsuarioDTO::de)
                .orElseThrow(AuthException::usuarioNoEncontrado);
    }

    // =================================================================
    // Interno
    // =================================================================
    private TokenResponse emitirTokens(Usuario usuario) {
        String accessToken = jwtService.generarAccessToken(usuario);
        String refreshToken = jwtService.generarRefreshToken();

        refreshTokenRepository.save(RefreshToken.builder()
                .usuario(usuario)
                .token(refreshToken)
                .expiraEn(OffsetDateTime.now().plus(jwtProperties.refreshTokenDuracion()))
                .build());

        return TokenResponse.de(accessToken, refreshToken,
                jwtService.accessTokenSegundos(),
                UsuarioDTO.de(usuario));
    }

    /** Higiene: elimina tokens caducados y revocados cada noche. */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void limpiarTokens() {
        int eliminados = refreshTokenRepository
                .eliminarCaducadosYRevocados(OffsetDateTime.now());
        if (eliminados > 0) {
            log.info("Tokens eliminados en la limpieza nocturna: {}", eliminados);
        }
    }
}
