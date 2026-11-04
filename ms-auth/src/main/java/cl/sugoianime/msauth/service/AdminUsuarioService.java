package cl.sugoianime.msauth.service;

import cl.sugoianime.msauth.domain.Rol;
import cl.sugoianime.msauth.domain.Usuario;
import cl.sugoianime.msauth.dto.ActualizarUsuarioAdminRequest;
import cl.sugoianime.msauth.dto.AdminUsuarioDTO;
import cl.sugoianime.msauth.exception.AuthException;
import cl.sugoianime.msauth.repository.PerfilRepository;
import cl.sugoianime.msauth.repository.RefreshTokenRepository;
import cl.sugoianime.msauth.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminUsuarioService {

    private static final Logger log = LoggerFactory.getLogger(AdminUsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AdminUsuarioService(UsuarioRepository usuarioRepository,
                               PerfilRepository perfilRepository,
                               RefreshTokenRepository refreshTokenRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUsuarioDTO> listarTodos() {
        return usuarioRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(u -> AdminUsuarioDTO.de(u, (int) perfilRepository.countByUsuarioId(u.getId())))
                .toList();
    }

    @Transactional
    public AdminUsuarioDTO actualizar(Long adminActualId, Long objetivoId,
                                      ActualizarUsuarioAdminRequest request) {
        Usuario usuario = usuarioRepository.findById(objetivoId)
                .orElseThrow(AuthException::usuarioNoEncontrado);

        boolean esMismoAdmin = adminActualId != null && adminActualId.equals(objetivoId);

        if (request.activo() != null) {
            if (esMismoAdmin && !request.activo()) {
                throw new AuthException(
                        "No puedes desactivar tu propia cuenta de administrador",
                        HttpStatus.CONFLICT);
            }
            usuario.setActivo(request.activo());
            if (!request.activo()) {
                refreshTokenRepository.revocarTodosDelUsuario(objetivoId);
            }
        }

        if (request.esPremium() != null) {
            usuario.setEsPremium(request.esPremium());
        }

        if (request.roles() != null && !request.roles().isEmpty()) {
            Set<Rol> nuevosRoles = EnumSet.noneOf(Rol.class);
            for (String r : request.roles()) {
                try {
                    nuevosRoles.add(Rol.valueOf(r.trim().toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    throw new AuthException("Rol no valido: " + r, HttpStatus.BAD_REQUEST);
                }
            }
            nuevosRoles.add(Rol.USUARIO);

            if (esMismoAdmin && !nuevosRoles.contains(Rol.ADMIN)) {
                throw new AuthException(
                        "No puedes quitarte el rol ADMIN a ti mismo",
                        HttpStatus.CONFLICT);
            }
            usuario.setRoles(nuevosRoles);
        }

        usuario = usuarioRepository.save(usuario);
        log.info("Admin {} actualizo la cuenta id={} (activo={}, premium={}, roles={})",
                adminActualId, objetivoId, usuario.isActivo(), usuario.isEsPremium(), usuario.getRoles());

        return AdminUsuarioDTO.de(usuario, (int) perfilRepository.countByUsuarioId(usuario.getId()));
    }

    @Transactional
    public void eliminar(Long adminActualId, Long objetivoId) {
        if (adminActualId != null && adminActualId.equals(objetivoId)) {
            throw new AuthException(
                    "No puedes eliminar tu propia cuenta de administrador desde el panel",
                    HttpStatus.CONFLICT);
        }

        Usuario usuario = usuarioRepository.findById(objetivoId)
                .orElseThrow(AuthException::usuarioNoEncontrado);

        usuarioRepository.delete(usuario);
        log.info("Admin {} elimino la cuenta id={}", adminActualId, objetivoId);
    }
}

