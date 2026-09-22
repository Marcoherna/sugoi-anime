package cl.sugoianime.mscatalogo.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static RecursoNoEncontradoException anime(Long id) {
        return new RecursoNoEncontradoException("No existe un anime con id " + id);
    }

    public static RecursoNoEncontradoException episodio(Long id) {
        return new RecursoNoEncontradoException("No existe un episodio con id " + id);
    }
}
