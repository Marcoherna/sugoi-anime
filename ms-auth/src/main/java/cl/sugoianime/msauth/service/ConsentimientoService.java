package cl.sugoianime.msauth.service;

import cl.sugoianime.msauth.domain.Consentimiento;
import cl.sugoianime.msauth.domain.Usuario;
import cl.sugoianime.msauth.repository.ConsentimientoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Registro auditable de consentimientos. Ley 19.628.
 *
 * Bean SEPARADO de AuthService a proposito: asi la llamada cruza el
 * limite del proxy de Spring y @Transactional funciona de verdad.
 * (Es la misma leccion del bug de auto-invocacion en ms-catalogo.)
 */
@Service
public class ConsentimientoService {

    private final ConsentimientoRepository repository;

    @Value("${consentimiento.version-vigente}")
    private String versionVigente;

    public ConsentimientoService(ConsentimientoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void registrar(Usuario usuario, Consentimiento.Tipo tipo,
                          boolean aceptado, String ipOrigen) {

        repository.save(Consentimiento.builder()
                .usuario(usuario)
                .tipo(tipo)
                .aceptado(aceptado)
                .versionTerminos(versionVigente)
                .ipOrigen(ipOrigen)
                .build());
    }

    @Transactional(readOnly = true)
    public List<Consentimiento> historial(Long usuarioId) {
        return repository.findByUsuarioIdOrderByAceptadoEnDesc(usuarioId);
    }

    public String versionVigente() {
        return versionVigente;
    }
}
