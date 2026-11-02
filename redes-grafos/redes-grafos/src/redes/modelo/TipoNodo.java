package redes.modelo;

/** Rol que cumple un vértice dentro de la red de distribución. */
public enum TipoNodo {
    /** Origen del recurso (embalse/bomba o generador). Aporta la carga/presión inicial. */
    FUENTE,
    /** Caja de distribución (agua): punto de reparto intermedio. */
    DISTRIBUCION,
    /** Subestación (electricidad): punto de transformación/reparto intermedio. */
    SUBESTACION,
    /** Consumidor final. Es un nodo terminal: consume, pero no reenvía el recurso. */
    DISPOSITIVO
}
