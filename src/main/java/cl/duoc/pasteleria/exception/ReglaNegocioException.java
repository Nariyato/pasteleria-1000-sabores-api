package cl.duoc.pasteleria.exception;

/** Se lanza cuando una operación rompe una regla de negocio. El GlobalExceptionHandler la convierte en un 409. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
