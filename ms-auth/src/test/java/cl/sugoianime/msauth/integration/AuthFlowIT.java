package cl.sugoianime.msauth.integration;

import cl.sugoianime.msauth.domain.Consentimiento;
import cl.sugoianime.msauth.dto.RegistroRequest;
import cl.sugoianime.msauth.dto.TokenResponse;
import cl.sugoianime.msauth.repository.ConsentimientoRepository;
import cl.sugoianime.msauth.repository.PerfilRepository;
import cl.sugoianime.msauth.repository.UsuarioRepository;
import cl.sugoianime.msauth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Flujo completo de autenticacion contra PostgreSQL real")
class AuthFlowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private AuthService authService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ConsentimientoRepository consentimientoRepository;
    @Autowired private JwtDecoder jwtDecoder;
    @Autowired private PerfilRepository perfilRepository;

    @Test
    @DisplayName("el registro crea usuario, perfil por defecto y 3 consentimientos")
    void registro_creaTodoElAgregado() {
        TokenResponse respuesta = authService.registrar(new RegistroRequest(
                "integracion@ejemplo.cl", "MiClaveSegura123", "Test Integracion",
                true, true), "10.0.0.1");

        assertThat(respuesta.accessToken()).isNotBlank();
        assertThat(respuesta.refreshToken()).isNotBlank();

        var usuario = usuarioRepository
                .findByEmailIgnoreCase("integracion@ejemplo.cl").orElseThrow();

        // La contrasena nunca se guarda en claro
        assertThat(usuario.getPasswordHash())
                .startsWith("$2a$12$")
                .isNotEqualTo("MiClaveSegura123");

        // RF-09: perfil por defecto.
        // Se consulta por repositorio en vez de navegar usuario.getPerfiles():
        // esa coleccion es LAZY y fuera de transaccion no puede inicializarse.
        assertThat(perfilRepository.findByUsuarioIdOrderByCreadoEnAsc(usuario.getId()))
                .hasSize(1);

        // Ley 19.628: tres consentimientos registrados con su version e IP
        List<Consentimiento> consentimientos =
                consentimientoRepository.findByUsuarioIdOrderByAceptadoEnDesc(usuario.getId());

        assertThat(consentimientos).hasSize(3);
        assertThat(consentimientos)
                .allSatisfy(c -> {
                    assertThat(c.getVersionTerminos()).isNotBlank();
                    assertThat(c.getIpOrigen()).isEqualTo("10.0.0.1");
                });
    }

    @Test
    @DisplayName("el access token emitido es valido y trae los claims esperados")
    void tokenEmitido_esValidoYTieneClaims() {
        TokenResponse respuesta = authService.registrar(new RegistroRequest(
                "claims@ejemplo.cl", "MiClaveSegura123", "Claims Test",
                true, false), "10.0.0.2");

        Jwt jwt = jwtDecoder.decode(respuesta.accessToken());

        assertThat(jwt.getSubject()).isNotBlank();
        assertThat(jwt.getClaimAsString("email")).isEqualTo("claims@ejemplo.cl");
        assertThat(jwt.getClaimAsStringList("roles")).contains("USUARIO");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    @DisplayName("el consentimiento opcional se registra tal como lo decidio el usuario")
    void consentimientoOpcional_respetaLaDecision() {
        authService.registrar(new RegistroRequest(
                "sinreco@ejemplo.cl", "MiClaveSegura123", "Sin Reco",
                true, false), "10.0.0.3");

        var usuario = usuarioRepository
                .findByEmailIgnoreCase("sinreco@ejemplo.cl").orElseThrow();

        var recomendaciones = consentimientoRepository
                .findByUsuarioIdOrderByAceptadoEnDesc(usuario.getId()).stream()
                .filter(c -> c.getTipo() == Consentimiento.Tipo.HISTORIAL_RECOMENDACIONES)
                .findFirst().orElseThrow();

        assertThat(recomendaciones.isAceptado()).isFalse();
    }

    @Test
    @DisplayName("el logout revoca el refresh token y este ya no sirve")
    void logout_revocaElRefreshToken() {
        TokenResponse respuesta = authService.registrar(new RegistroRequest(
                "logout@ejemplo.cl", "MiClaveSegura123", "Logout Test",
                true, true), "10.0.0.4");

        authService.logout(respuesta.refreshToken());

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> authService.refrescar(respuesta.refreshToken()))
                .hasMessageContaining("invalido");
    }
}
