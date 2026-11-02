package redes.modelo;

/** Tipo de servicio que transporta la red; solo cambia las unidades que se muestran. */
public enum TipoRed {
    AGUA("L/s", "m.c.a."),
    ELECTRICA("kW", "V");

    private final String unidadFlujo;
    private final String unidadCarga;

    TipoRed(String unidadFlujo, String unidadCarga) {
        this.unidadFlujo = unidadFlujo;
        this.unidadCarga = unidadCarga;
    }

    /** Unidad de caudal (agua) o potencia (electricidad). */
    public String unidadFlujo() { return unidadFlujo; }

    /** Unidad de presión (agua) o tensión (electricidad). */
    public String unidadCarga() { return unidadCarga; }
}
