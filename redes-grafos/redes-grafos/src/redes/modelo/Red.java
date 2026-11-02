package redes.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Red de servicio: un {@link Grafo} + parámetros físicos + el escenario de fallas vigente.
 * Es la fachada con la que trabajan los algoritmos y la interfaz de consola.
 */
public final class Red {
    private final String nombre;
    private final TipoRed tipo;
    private final Grafo grafo = new Grafo();

    /** Aristas más largas que este umbral se consideran pérdida crítica (desconexión). */
    private double distanciaMaxima = Double.POSITIVE_INFINITY;
    /** Presión/tensión mínima para que un nodo se considere abastecido. */
    private double presionMinima = 0;
    /** Pérdida de presión/tensión por unidad de longitud (modelo lineal de decaimiento). */
    private double decaimientoPorUnidad = 0;

    private Escenario escenario = Escenario.SIN_FALLAS;

    public Red(String nombre, TipoRed tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    // ---------------------------------------------------------------- parámetros
    public void configurar(double distanciaMaxima, double presionMinima, double decaimientoPorUnidad) {
        if (distanciaMaxima <= 0 || presionMinima < 0 || decaimientoPorUnidad < 0)
            throw new IllegalArgumentException("Parámetros físicos inválidos");
        this.distanciaMaxima = distanciaMaxima;
        this.presionMinima = presionMinima;
        this.decaimientoPorUnidad = decaimientoPorUnidad;
    }

    public String nombre() { return nombre; }
    public TipoRed tipo() { return tipo; }
    public Grafo grafo() { return grafo; }
    public double distanciaMaxima() { return distanciaMaxima; }
    public double presionMinima() { return presionMinima; }
    public double decaimientoPorUnidad() { return decaimientoPorUnidad; }

    // ---------------------------------------------------------------- consultas
    public List<Nodo> fuentes() {
        return grafo.nodos().stream().filter(Nodo::esFuente).toList();
    }

    public List<Nodo> dispositivos() {
        return grafo.nodos().stream().filter(Nodo::esDispositivo).toList();
    }

    // ---------------------------------------------------------------- fallas
    public Escenario escenario() { return escenario; }

    public void registrarFallaNodo(String id) {
        grafo.nodo(id); // valida existencia
        escenario = escenario.conNodoCaido(id);
    }

    public void registrarFallaArista(String id) {
        grafo.arista(id);
        escenario = escenario.conAristaCaida(id);
    }

    public void repararNodo(String id) { escenario = escenario.sinNodoCaido(id); }

    public void repararArista(String id) { escenario = escenario.sinAristaCaida(id); }

    public void limpiarFallas() { escenario = Escenario.SIN_FALLAS; }

    /** Texto breve con el tamaño de la red, útil para reportes. */
    public String resumen() {
        List<String> partes = new ArrayList<>();
        partes.add(grafo.nodos().size() + " nodos (" + fuentes().size() + " fuentes, "
                + dispositivos().size() + " dispositivos)");
        partes.add(grafo.aristas().size() + " conexiones");
        return nombre + " [" + tipo + "]: " + String.join(", ", partes);
    }
}
