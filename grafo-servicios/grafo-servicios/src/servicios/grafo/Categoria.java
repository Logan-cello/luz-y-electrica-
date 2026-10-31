package servicios.grafo;

/**
 * Tipo de elemento dentro de la red. Determina si el nodo produce, transporta
 * o consume el servicio.
 *
 * <p>La categoría es clave para el análisis: los nodos "consumidores" son los
 * que se cuentan para medir cuánta demanda queda sin atender cuando hay fallos.</p>
 */
public enum Categoria {

    // ---------- Red eléctrica ----------
    PLANTA("Planta generadora", Rol.FUENTE),
    SUBESTACION("Subestación", Rol.TRANSPORTE),
    TRANSFORMADOR("Transformador", Rol.TRANSPORTE),
    POSTE("Poste de distribución", Rol.TRANSPORTE),

    // ---------- Red de agua ----------
    RESERVORIO("Reservorio", Rol.FUENTE),
    BOMBA("Estación de bombeo", Rol.TRANSPORTE),
    TANQUE("Tanque de almacenamiento", Rol.TRANSPORTE),
    VALVULA("Válvula reguladora", Rol.TRANSPORTE),
    EMPALME("Empalme / cruce de tubería", Rol.TRANSPORTE),

    // ---------- Consumidores ----------
    VIVIENDA("Viviendas", Rol.CONSUMIDOR),
    HOSPITAL("Hospital", Rol.CONSUMIDOR),
    INDUSTRIA("Industria", Rol.CONSUMIDOR);

    public enum Rol { FUENTE, TRANSPORTE, CONSUMIDOR }

    private final String etiqueta;
    private final Rol rol;

    Categoria(String etiqueta, Rol rol) {
        this.etiqueta = etiqueta;
        this.rol = rol;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean esConsumidor() {
        return rol == Rol.CONSUMIDOR;
    }

    public boolean esFuente() {
        return rol == Rol.FUENTE;
    }
}
