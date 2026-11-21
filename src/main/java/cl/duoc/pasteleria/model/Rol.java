package cl.duoc.pasteleria.model;

/** Roles del sistema. ADMIN todo, VENDEDOR solo lectura de productos y boletas, CLIENTE solo la tienda. */
public enum Rol {
    ADMIN,
    VENDEDOR,
    CLIENTE;

    public String aJson() {
        return name().toLowerCase();
    }

    public static Rol desdeJson(String texto) {
        return valueOf(texto.toUpperCase());
    }
}
