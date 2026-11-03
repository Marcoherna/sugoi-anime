package cl.sugoianime.msauth.service;

import cl.sugoianime.msauth.config.JwtProperties;
import cl.sugoianime.msauth.domain.Usuario;
import cl.sugoianime.msauth.dto.LoginRequest;
import cl.sugoianime.msauth.dto.RegistroRequest;
import cl.sugoianime.msauth.exception.AuthException;
import cl.sugoianime.msauth.repository.RefreshTokenRepository;
import cl.sugoianime.msauth.repository.UsuarioRepository;
import cl.sugoianime.msauth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService - reglas de autenticacion (RF-08)")
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private ConsentimientoService consentimientoService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthService service;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(
                "no-usado-en-este-test", "sugoianime-auth",
                Duration.ofMinutes(15), Duration.ofDays(7));

        service = new AuthService(usuarioRepository, refreshTokenRepository,
                consentimientoService, passwordEncoder,
                jwtService, props);
    }

    @Test
    @DisplayName("rechaza el registro si el email ya existe")
    void registrar_emailDuplicado_lanza409() {
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        RegistroRequest req = new RegistroRequest(
                "ana@ejemplo.cl", "MiClaveSegura123", "Ana", true, true);

        assertThatThrownBy(() -> service.registrar(req, "127.0.0.1"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Ya existe una cuenta");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("nunca almacena la contrasena en claro")
    void registrar_hasheaLaPassword() {
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode("MiClaveSegura123")).thenReturn("$2a$12$hashfalso");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(inv -> {
                    Usuario u = inv.getArgument(0);
                    u.setId(1L);
                    return u;
                });
        when(jwtService.generarAccessToken(any())).thenReturn("jwt-falso");
        when(jwtService.generarRefreshToken()).thenReturn("refresh-falso");

        service.registrar(new RegistroRequest(
                "ana@ejemplo.cl", "MiClaveSegura123", "Ana Perez", true, true), "127.0.0.1");

        verify(passwordEncoder).encode("MiClaveSegura123");
    }

    @Test
    @DisplayName("el login con email inexistente devuelve el mismo error que con clave mala")
    void login_emailInexistente_mensajeAmbiguo() {
        when(usuarioRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.login(new LoginRequest("noexiste@ejemplo.cl", "loquesea")))
                .isInstanceOf(AuthException.class)
                // Ambiguo a proposito: evita la enumeracion de usuarios
                .hasMessage("Email o contrasena incorrectos");
    }

    @Test
    @DisplayName("el login con contrasena incorrecta devuelve 401")
    void login_passwordIncorrecta_lanza401() {
        Usuario usuario = Usuario.builder()
                .id(1L).email("ana@ejemplo.cl")
                .passwordHash("$2a$12$hash").nombre("Ana").activo(true).build();

        when(usuarioRepository.findByEmailIgnoreCase("ana@ejemplo.cl"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala", "$2a$12$hash")).thenReturn(false);

        assertThatThrownBy(() ->
                service.login(new LoginRequest("ana@ejemplo.cl", "mala")))
                .isInstanceOf(AuthException.class)
                .hasMessage("Email o contrasena incorrectos");
    }

    @Test
    @DisplayName("una cuenta desactivada no puede iniciar sesion")
    void login_cuentaInactiva_lanza403() {
        Usuario usuario = Usuario.builder()
                .id(1L).email("ana@ejemplo.cl")
                .passwordHash("$2a$12$hash").nombre("Ana").activo(false).build();

        when(usuarioRepository.findByEmailIgnoreCase("ana@ejemplo.cl"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() ->
                service.login(new LoginRequest("ana@ejemplo.cl", "MiClaveSegura123")))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("desactivada");
    }
}
