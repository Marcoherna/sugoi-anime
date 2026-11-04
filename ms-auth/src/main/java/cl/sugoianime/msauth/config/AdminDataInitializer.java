package cl.sugoianime.msauth.config;

import cl.sugoianime.msauth.domain.Consentimiento;
import cl.sugoianime.msauth.domain.Perfil;
import cl.sugoianime.msauth.domain.Rol;
import cl.sugoianime.msauth.domain.Usuario;
import cl.sugoianime.msauth.repository.UsuarioRepository;
import cl.sugoianime.msauth.service.ConsentimientoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

/**
 * Inicializador de cuenta administradora por defecto.
 * Asegura que exista siempre al menos una cuenta con rol ADMIN para gestionar la plataforma.
 */
@Component
public class AdminDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminDataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final ConsentimientoService consentimientoService;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.bootstrap.email:admin@sugoianime.cl}")
    private String adminEmail;

    @Value("${admin.bootstrap.password:Admin1234!}")
    private String adminPassword;

    @Value("${admin.bootstrap.nombre:Administrador SugoiAnime}")
    private String adminNombre;

    public AdminDataInitializer(UsuarioRepository usuarioRepository,
                                ConsentimientoService consentimientoService,
                                PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.consentimientoService = consentimientoService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var existente = usuarioRepository.findByEmailIgnoreCase(adminEmail);
        if (existente.isPresent()) {
            Usuario u = existente.get();
            if (!u.getRoles().contains(Rol.ADMIN)) {
                u.getRoles().add(Rol.ADMIN);
                usuarioRepository.save(u);
                log.info("Rol ADMIN asegurado para la cuenta existente: {}", adminEmail);
            }
            return;
        }

        Usuario admin = Usuario.builder()
                .email(adminEmail.toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(adminPassword))
                .nombre(adminNombre)
                .activo(true)
                .esPremium(true)
                .roles(EnumSet.of(Rol.USUARIO, Rol.PREMIUM, Rol.ADMIN))
                .build();

        admin.agregarPerfil(Perfil.builder()
                .nombre("Admin")
                .avatar("default")
                .esInfantil(false)
                .build());

        admin = usuarioRepository.save(admin);

        consentimientoService.registrar(admin,
                Consentimiento.Tipo.TERMINOS_Y_CONDICIONES, true, "127.0.0.1");
        consentimientoService.registrar(admin,
                Consentimiento.Tipo.TRATAMIENTO_DATOS, true, "127.0.0.1");
        consentimientoService.registrar(admin,
                Consentimiento.Tipo.HISTORIAL_RECOMENDACIONES, true, "127.0.0.1");

        log.info("Cuenta administradora inicial creada: email={}", adminEmail);
    }
}

