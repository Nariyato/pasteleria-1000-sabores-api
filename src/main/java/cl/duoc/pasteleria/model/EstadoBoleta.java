package cl.duoc.pasteleria.model;

/** Estados de despacho de una boleta (en el front: preparacion, en_camino, entregado). */
public enum EstadoBoleta {
    PREPARACION,
    EN_CAMINO,
    ENTREGADO;

    public String aJson() {
        return switch (this) {
            case PREPARACION -> "preparacion";
            case EN_CAMINO -> "en_camino";
            case ENTREGADO -> "entregado";
        };
    }

    public static EstadoBoleta desdeJson(String texto) {
        return switch (texto) {
            case "preparacion" -> PREPARACION;
            case "en_camino" -> EN_CAMINO;
            case "entregado" -> ENTREGADO;
            default -> throw new IllegalArgumentException("Estado no válido: " + texto);
        };
    }
}
