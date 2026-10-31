package servicios.grafo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Grafo dirigido (o no dirigido) que representa una red de servicio.
 *
 * <p>Responsabilidad única: guardar la topología y ofrecer los recorridos
 * básicos (BFS, Dijkstra, componentes conexas). La política de qué se considera
 * "activo" vive en {@link servicios.analisis.AnalizadorRed}, para poder comparar
 * distintos escenarios sobre el mismo grafo.</p>
 *
 * <p>Todas las consultas ignoran por defecto los nodos y aristas averiados.</p>
 */
public class Grafo {

    private final String nombre;
    private final Servicio servicio;
    private final boolean dirigido;

    private final Map<String, Nodo> nodos = new LinkedHashMap<>();
    private final Map<String, Arista> aristas = new LinkedHashMap<>();

    /** Aristas que salen de cada nodo (adyacencia principal). */
    private final Map<String, List<Arista>> salientes = new HashMap<>();
    /** Aristas que llegan a cada nodo (necesarias para tratar el grafo como no dirigido). */
    private final Map<String, List<Arista>> entrantes = new HashMap<>();

    public Grafo(String nombre, Servicio servicio, boolean dirigido) {
        this.nombre = nombre;
        this.servicio = servicio;
        this.dirigido = dirigido;
    }

    public static Grafo dirigido(String nombre, Servicio servicio) {
        return new Grafo(nombre, servicio, true);
    }

    // ------------------------------------------------------------------
    // Construcción
    // ------------------------------------------------------------------

    public void agregar(Nodo n) {
        if (nodos.putIfAbsent(n.getId(), n) != null) {
            throw new IllegalArgumentException("Nodo duplicado: " + n.getId());
        }
        salientes.put(n.getId(), new ArrayList<>());
        entrantes.put(n.getId(), new ArrayList<>());
    }

    public void agregar(Nodo... ns) {
        for (Nodo n : ns) agregar(n);
    }

    /** Conecta dos nodos existentes y devuelve la arista creada. */
    public Arista conectar(String idArista, String idOrigen, String idDestino,
                           double capacidad, double longitud) {
        Nodo o = requerir(idOrigen);
        Nodo d = requerir(idDestino);
        Arista a = new Arista(idArista, o, d, capacidad, longitud);
        if (aristas.putIfAbsent(idArista, a) != null) {
            throw new IllegalArgumentException("Arista duplicada: " + idArista);
        }
        salientes.get(idOrigen).add(a);
        entrantes.get(idDestino).add(a);
        return a;
    }

    private Nodo requerir(String id) {
        Nodo n = nodos.get(id);
        if (n == null) throw new IllegalArgumentException("No existe el nodo: " + id);
        return n;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public Nodo nodo(String id) {
        return nodos.get(id);
    }

    public Arista arista(String id) {
        return aristas.get(id);
    }

    public Collection<Nodo> getNodos() {
        return Collections.unmodifiableCollection(nodos.values());
    }

    public Collection<Arista> getAristas() {
        return Collections.unmodifiableCollection(aristas.values());
    }

    public String getNombre() {
        return nombre;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public boolean esDirigido() {
        return dirigido;
    }

    /** Aristas incidentes en un nodo, respetando o no la dirección. */
    public List<Arista> incidentes(String idNodo, boolean respetarDireccion) {
        List<Arista> salida = new ArrayList<>(salientes.getOrDefault(idNodo, List.of()));
        if (!dirigido || !respetarDireccion) {
            salida.addAll(entrantes.getOrDefault(idNodo, List.of()));
        }
        return salida;
    }

    /** Vecinos alcanzables desde un nodo siguiendo aristas activas. */
    public List<Nodo> vecinosActivos(String idNodo, boolean seguirDireccion) {
        List<Nodo> vecinos = new ArrayList<>();
        for (Arista a : incidentes(idNodo, seguirDireccion)) {
            if (a.estaFallada()) continue;
            Nodo otro = a.otroExtremo(nodos.get(idNodo));
            if (otro != null && !otro.estaFallado()) vecinos.add(otro);
        }
        return vecinos;
    }

    // ------------------------------------------------------------------
    // Recorridos
    // ------------------------------------------------------------------

    /** BFS: nodos alcanzables desde {@code origen} sin atravesar fallos. */
    public Set<Nodo> alcanzablesDesde(Nodo origen, boolean seguirDireccion) {
        Set<Nodo> visitados = new LinkedHashSet<>();
        if (origen == null || origen.estaFallado()) return visitados;
        Deque<Nodo> cola = new ArrayDeque<>();
        cola.add(origen);
        visitados.add(origen);
        while (!cola.isEmpty()) {
            Nodo actual = cola.poll();
            for (Nodo v : vecinosActivos(actual.getId(), seguirDireccion)) {
                if (visitados.add(v)) cola.add(v);
            }
        }
        return visitados;
    }

    /** Nodos que quedan aislados de toda fuente activa (sin servicio). */
    public Set<Nodo> sinServicio(List<Nodo> fuentes) {
        Set<Nodo> alcanzados = new LinkedHashSet<>();
        for (Nodo fuente : fuentes) {
            alcanzados.addAll(alcanzablesDesde(fuente, true));
        }
        Set<Nodo> afectados = new LinkedHashSet<>();
        for (Nodo n : nodos.values()) {
            if (!alcanzados.contains(n)) afectados.add(n);
        }
        return afectados;
    }

    /**
     * Dijkstra sobre la longitud de las aristas (coste de reposición/riesgo).
     * Devuelve la lista de nodos del camino más corto, o lista vacía si no hay
     * camino con la red en su estado actual.
     */
    public List<Nodo> caminoMasCorto(String idOrigen, String idDestino) {
        Nodo origen = nodos.get(idOrigen);
        Nodo destino = nodos.get(idDestino);
        if (origen == null || destino == null || origen.estaFallado() || destino.estaFallado()) {
            return List.of();
        }

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> previo = new HashMap<>();
        for (String id : nodos.keySet()) dist.put(id, Double.POSITIVE_INFINITY);
        dist.put(origen.getId(), 0.0);

        // Par (distancia acumulada, id del nodo)
        PriorityQueue<Object[]> cola = new PriorityQueue<>(
                (a, b) -> Double.compare((Double) a[0], (Double) b[0]));
        cola.add(new Object[]{0.0, origen.getId()});
        Set<String> cerrados = new HashSet<>();

        while (!cola.isEmpty()) {
            Object[] par = cola.poll();
            String actual = (String) par[1];
            if (!cerrados.add(actual)) continue;
            if (actual.equals(destino.getId())) break;

            for (Nodo v : vecinosActivos(actual, true)) {
                double coste = costeArista(actual, v.getId());
                if (coste == Double.POSITIVE_INFINITY) continue;
                double nd = dist.get(actual) + coste;
                if (nd < dist.get(v.getId())) {
                    dist.put(v.getId(), nd);
                    previo.put(v.getId(), actual);
                    cola.add(new Object[]{nd, v.getId()});
                }
            }
        }

        if (!origen.equals(destino) && !previo.containsKey(destino.getId())) return List.of();

        List<Nodo> camino = new ArrayList<>();
        String paso = destino.getId();
        camino.add(nodos.get(paso));
        while (!paso.equals(origen.getId())) {
            paso = previo.get(paso);
            if (paso == null) return List.of();
            camino.add(0, nodos.get(paso));
        }
        return camino;
    }

    /** Longitud de la arista activa más corta entre dos nodos vecinos. */
    private double costeArista(String idOrigen, String idDestino) {
        double mejor = Double.POSITIVE_INFINITY;
        for (Arista a : incidentes(idOrigen, true)) {
            if (a.estaFallada()) continue;
            if (a.getDestino().getId().equals(idDestino)) {
                mejor = Math.min(mejor, a.getLongitud());
            }
        }
        return mejor;
    }

    /** Componentes conexas considerando la red como no dirigida e ignorando fallos. */
    public List<Set<Nodo>> componentesConexas() {
        List<Set<Nodo>> componentes = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (Nodo n : nodos.values()) {
            if (n.estaFallado() || vistos.contains(n.getId())) continue;
            Set<Nodo> comp = new LinkedHashSet<>();
            Deque<Nodo> cola = new ArrayDeque<>();
            cola.add(n);
            vistos.add(n.getId());
            while (!cola.isEmpty()) {
                Nodo actual = cola.poll();
                comp.add(actual);
                for (Nodo v : vecinosActivos(actual.getId(), false)) {
                    if (vistos.add(v.getId())) cola.add(v);
                }
            }
            componentes.add(comp);
        }
        return componentes;
    }

    /** Suma de la demanda de los consumidores del grafo. */
    public double demandaTotal() {
        double total = 0;
        for (Nodo n : nodos.values()) {
            if (n.esConsumidor() && !n.estaFallado()) total += n.getDemanda();
        }
        return total;
    }
}
