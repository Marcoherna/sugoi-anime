package cl.sugoianime.msauth.service;

import cl.sugoianime.msauth.domain.Perfil;
import cl.sugoianime.msauth.domain.Usuario;
import cl.sugoianime.msauth.dto.CrearPerfilRequest;
import cl.sugoianime.msauth.dto.PerfilDTO;
import cl.sugoianime.msauth.exception.AuthException;
import cl.sugoianime.msauth.repository.PerfilRepository;
import cl.sugoianime.msauth.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PerfilService {

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${perfiles.maximo-por-cuenta}")
    private int maximoPerfiles;

    public PerfilService(PerfilRepository perfilRepository,
                         UsuarioRepository usuarioRepository) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<PerfilDTO> listar(Long usuarioId) {
        return perfilRepository.findByUsuarioIdOrderByCreadoEnAsc(usuarioId)
                .stream().map(PerfilDTO::de).toList();
    }

    @Transactional
    public PerfilDTO crear(Long usuarioId, CrearPerfilRequest request) {

        if (perfilRepository.countByUsuarioId(usuarioId) >= maximoPerfiles) {
            throw AuthException.limitePerfiles(maximoPerfiles);
        }

        String nombre = request.nombre().trim();

        if (perfilRepository.existsByUsuarioIdAndNombreIgnoreCase(usuarioId, nombre)) {
            throw AuthException.perfilDuplicado(nombre);
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(AuthException::usuarioNoEncontrado);

        Perfil perfil = Perfil.builder()
                .usuario(usuario)
                .nombre(nombre)
                .avatar(request.avatar() == null ? "default" : request.avatar())
                .esInfantil(Boolean.TRUE.equals(request.esInfantil()))
                .build();

        return PerfilDTO.de(perfilRepository.save(perfil));
    }

    @Transactional
    public PerfilDTO actualizar(Long usuarioId, Long perfilId, CrearPerfilRequest request) {

        // Filtrar por usuarioId ademas del id es la defensa contra IDOR:
        // impide que un usuario modifique el perfil de otro cambiando la URL.
        Perfil perfil = perfilRepository.findByIdAndUsuarioId(perfilId, usuarioId)
                .orElseThrow(AuthException::perfilNoEncontrado);

        String nombre = request.nombre().trim();

        if (!perfil.getNombre().equalsIgnoreCase(nombre)
                && perfilRepository.existsByUsuarioIdAndNombreIgnoreCase(usuarioId, nombre)) {
            throw AuthException.perfilDuplicado(nombre);
        }

        perfil.setNombre(nombre);
        if (request.avatar() != null) {
            perfil.setAvatar(request.avatar());
        }
        if (request.esInfantil() != null) {
            perfil.setEsInfantil(request.esInfantil());
        }

        return PerfilDTO.de(perfil);
    }

    @Transactional
    public void eliminar(Long usuarioId, Long perfilId) {

        Perfil perfil = perfilRepository.findByIdAndUsuarioId(perfilId, usuarioId)
                .orElseThrow(AuthException::perfilNoEncontrado);

        // Toda cuenta debe conservar al menos un perfil utilizable.
        if (perfilRepository.countByUsuarioId(usuarioId) <= 1) {
            throw new AuthException("No puedes eliminar tu unico perfil",
                    org.springframework.http.HttpStatus.CONFLICT);
        }

        perfilRepository.delete(perfil);
    }
}
