package servicios.grafo;

import java.util.Objects;

/**
 * Arista del grafo: una conexión física entre dos componentes
 * (cable, tubería, acometida...).
 *
 * <p>Las aristas son <b>dirigidas</b> porque el flujo tiene sentido: la energía
 * va de la planta hacia los consumidores y el agua va del reservorio hacia las
 * viviendas. Una tubería puede estar cerrada (válvula) y un cable puede estar
 * caído, y en ambos casos la arista queda "fuera de servicio".</p>
 */
public class Arista {

    private final String id;
    private final Nodo origen;
    private final Nodo destino;

    /** Capacidad: kW para luz, L/min para agua. */
    private final double capacidad;

    /** Coste/longitud de la conexión (se usa en Dijkstra como "riesgo"). */
    private final double longitud;

    private boolean fallada;
    private String motivoFallo = "";

    public Arista(String id, Nodo origen, Nodo destino, double capacidad, double longitud) {
        this.id = Objects.requireNonNull(id, "id");
        this.origen = Objects.requireNonNull(origen, "origen");
        this.destino = Objects.requireNonNull(destino, "destino");
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser positiva: " + id);
        }
        if (longitud <= 0) {
            throw new IllegalArgumentException("La longitud debe ser positiva: " + id);
        }
        this.capacidad = capacidad;
        this.longitud = longitud;
    }

    public void fallar(String motivo) {
        this.fallada = true;
        this.motivoFallo = motivo == null ? "" : motivo;
    }

    public void reparar() {
        this.fallada = false;
        this.motivoFallo = "";
    }

    public boolean estaFallada() {
        return fallada;
    }

    public String getMotivoFallo() {
        return motivoFallo;
    }

    public String getId() {
        return id;
    }

    public Nodo getOrigen() {
        return origen;
    }

    public Nodo getDestino() {
        return destino;
    }

    public double getCapacidad() {
        return capacidad;
    }

    public double getLongitud() {
        return longitud;
    }

    /** Devuelve el nodo en el otro extremo de la arista, o null si no pertenece a ella. */
    public Nodo otroExtremo(Nodo n) {
        if (n.equals(origen)) return destino;
        if (n.equals(destino)) return origen;
        return null;
    }

    @Override
    public String toString() {
        return id + ": " + origen.getId() + " -> " + destino.getId();
    }
}
