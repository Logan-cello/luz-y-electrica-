package servicios.grafo;

/**
 * Servicios públicos modelados como grafos independientes pero interdependientes.
 */
public enum Servicio {

    LUZ("Luz eléctrica", "kW"),
    AGUA("Agua potable", "L/min");

    private final String etiqueta;
    private final String unidad;

    Servicio(String etiqueta, String unidad) {
        this.etiqueta = etiqueta;
        this.unidad = unidad;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Unidad de la demanda: kW para luz, L/min para agua. */
    public String getUnidad() {
        return unidad;
    }
}
