package cl.duoc.pasteleria.exception;

import java.util.Map;

/** Validación de campos hecha en el service (por ejemplo contraseña obligatoria al crear usuario). */
public class ValidacionCamposException extends RuntimeException {

    private final Map<String, String> errores;

    public ValidacionCamposException(Map<String, String> errores) {
        super("Hay campos con datos no válidos");
        this.errores = Map.copyOf(errores);
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}