package redes.modelo;

import java.util.Objects;

/**
 * Arista no dirigida (tubería o línea de transmisión) entre dos nodos.
 * La red real es bidireccional en topología, pero el sentido del flujo lo decide la simulación.
 *
 * @param capacidad flujo/corriente máximo soportado (<= 0 significa "sin límite").
 * @param longitud  largo de la conexión; determina la caída de presión/tensión y la
 *                  validez frente a la distancia máxima configurada.
 */
public record Arista(String id, String origen, String destino, double capacidad, double longitud) {

    public Arista {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(destino, "destino");
        if (longitud < 0) throw new IllegalArgumentException("Longitud negativa en " + id);
    }

    /** Dado un extremo, devuelve el opuesto (en un bucle devuelve el mismo nodo). */
    public String otroExtremo(String nodoId) {
        if (nodoId.equals(origen)) return destino;
        if (nodoId.equals(destino)) return origen;
        throw new IllegalArgumentException(nodoId + " no es extremo de la arista " + id);
    }

    @Override
    public String toString() { return id + " [" + origen + " — " + destino + ", " + longitud + " m]"; }
}
