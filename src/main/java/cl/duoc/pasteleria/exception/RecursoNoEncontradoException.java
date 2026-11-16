package cl.duoc.pasteleria.exception;

/** Se lanza cuando un registro no existe. El GlobalExceptionHandler la convierte en un 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
