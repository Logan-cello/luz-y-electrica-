package redes.algoritmos;

import redes.modelo.*;

import java.util.*;

/**
 * Detección de puntos críticos con el algoritmo de TARJAN (DFS con tiempos de descubrimiento).
 *
 * TEORÍA DE GRAFOS:
 *  - disc[u]: orden en que el DFS descubre u.  low[u]: el menor disc alcanzable desde el
 *    subárbol de u usando como máximo UNA arista de retroceso.
 *  - Arista (u,v) del árbol DFS es PUENTE (arista de corte)  <=>  low[v] > disc[u]
 *    (el subárbol de v no tiene ninguna otra vía hacia u o sus ancestros).
 *  - u es PUNTO DE ARTICULACIÓN  <=>  (u no es raíz y algún hijo v cumple low[v] >= disc[u])
 *                                     o (u es raíz y tiene 2 o más hijos DFS).
 *  - Se salta la arista por su ID (no por el nodo padre) para tratar bien las aristas paralelas.
 *  Complejidad: O(V + E).
 *
 * MATIZ IMPORTANTE: un puente estructural no siempre deja sin servicio a alguien (si el otro
 * lado ya tiene su propia fuente). Por eso cada hallazgo se cruza con la simulación de
 * {@link Servicio}: se mide cuántos dispositivos se pierden realmente si ese componente cae.
 *
 * Nota: la recursión del DFS es suficiente para redes de miles de nodos; para redes de cientos
 * de miles habría que convertirla en iterativa o ejecutarla en un hilo con pila grande.
 */
public final class Vulnerabilidad {

    public enum Clase { PUENTE, ARTICULACION }

    /** @param aislados dispositivos que pierden el servicio si el componente falla. */
    public record Hallazgo(Clase clase, String id, String descripcion, Set<String> aislados, double porcentaje) { }

    private final Red red;
    private final Map<String, Integer> disc = new HashMap<>();
    private final Map<String, Integer> low = new HashMap<>();
    private final Set<Arista> puentes = new LinkedHashSet<>();
    private final Set<String> articulaciones = new TreeSet<>();
    private int tiempo = 0;

    private Vulnerabilidad(Red red) { this.red = red; }

    /** Analiza la red tal como está diseñada (sin fallas) y devuelve hallazgos ordenados por gravedad. */
    public static List<Hallazgo> analizar(Red red) {
        Vulnerabilidad v = new Vulnerabilidad(red);
        for (Nodo n : red.grafo().nodos())
            if (!v.disc.containsKey(n.id())) v.dfs(n.id(), null);

        Set<String> base = Servicio.calcular(red, Escenario.SIN_FALLAS).servidos();
        List<Hallazgo> res = new ArrayList<>();

        for (Arista a : v.puentes) {
            Set<String> perdidos = perdidos(red, base, Escenario.SIN_FALLAS.conAristaCaida(a.id()));
            res.add(new Hallazgo(Clase.PUENTE, a.id(), a.toString(), perdidos, pct(perdidos, base)));
        }
        for (String id : v.articulaciones) {
            Nodo n = red.grafo().nodo(id);
            if (n.esDispositivo()) continue; // un consumidor nunca es punto de paso del servicio
            Set<String> perdidos = perdidos(red, base, Escenario.SIN_FALLAS.conNodoCaido(id));
            res.add(new Hallazgo(Clase.ARTICULACION, id, n.toString(), perdidos, pct(perdidos, base)));
        }
        res.sort(Comparator.comparingInt((Hallazgo h) -> h.aislados().size()).reversed()
                .thenComparing(Hallazgo::id));
        return res;
    }

    private static Set<String> perdidos(Red red, Set<String> base, Escenario esc) {
        Set<String> r = new TreeSet<>(base);
        r.removeAll(Servicio.calcular(red, esc).servidos());
        return r;
    }

    private static double pct(Set<String> perdidos, Set<String> base) {
        return base.isEmpty() ? 0 : 100.0 * perdidos.size() / base.size();
    }

    private boolean usable(Arista a) { return a.longitud() <= red.distanciaMaxima(); }

    private void dfs(String u, String aristaPadre) {
        disc.put(u, ++tiempo);
        low.put(u, tiempo);
        int hijos = 0;
        for (Arista a : red.grafo().incidentes(u)) {
            if (!usable(a) || a.id().equals(aristaPadre)) continue;
            String v = a.otroExtremo(u);
            if (!disc.containsKey(v)) {
                hijos++;
                dfs(v, a.id());
                low.put(u, Math.min(low.get(u), low.get(v)));
                if (low.get(v) > disc.get(u)) puentes.add(a);
                if (aristaPadre != null && low.get(v) >= disc.get(u)) articulaciones.add(u);
            } else {
                low.put(u, Math.min(low.get(u), disc.get(v))); // arista de retroceso
            }
        }
        if (aristaPadre == null && hijos > 1) articulaciones.add(u);
    }
}
