package redes.algoritmos;

import redes.modelo.*;

import java.util.*;

/**
 * Plan de reparación priorizado (heurística VORAZ con COLA DE PRIORIDAD).
 *
 * En cada ronda se evalúa, para cada componente averiado pendiente, cuántos dispositivos
 * recuperarían servicio si SOLO ese componente se reparara (ganancia marginal, medida con
 * {@link Servicio}: BFS/Dijkstra sobre el grafo con el componente restablecido). Una
 * {@link PriorityQueue} ordena los candidatos por:
 *     1) mayor cantidad de dispositivos restablecidos,
 *     2) menor costo de reparación (longitud de la tubería/línea; costo fijo para nodos),
 *     3) id (desempate determinista).
 * Se extrae el mejor, se aplica la reparación y se repite con el nuevo estado.
 *
 * Por qué recalcular en cada ronda: los beneficios NO son aditivos. Dos aristas caídas en
 * serie solo devuelven servicio juntas; dos en paralelo (redundantes) restablecen lo mismo
 * y reparar una reduce el valor de la otra.
 *
 * Es una heurística: maximiza el beneficio inmediato, no garantiza el óptimo global (el
 * problema general es NP-difícil). Complejidad: O(k^2 (V + E) log V) con k fallas.
 */
public final class PlanReparacion {
    private PlanReparacion() { }

    public static final double COSTO_NODO = 50.0;

    /** @param restablecidos dispositivos que recuperan servicio gracias a este paso. */
    public record Paso(int orden, String tipo, String id, double costo, Set<String> restablecidos) { }

    private record Componente(boolean esArista, String id, double costo) { }

    private record Candidato(Componente c, Set<String> restablecidos) { }

    public static List<Paso> calcular(Red red) {
        Escenario esc = red.escenario();
        List<Componente> pendientes = new ArrayList<>();
        for (String id : esc.aristasCaidas())
            pendientes.add(new Componente(true, id, red.grafo().arista(id).longitud()));
        for (String id : esc.nodosCaidos())
            pendientes.add(new Componente(false, id, COSTO_NODO));

        Comparator<Candidato> prioridad = Comparator
                .comparingInt((Candidato c) -> c.restablecidos().size()).reversed()
                .thenComparingDouble((Candidato c) -> c.c().costo())
                .thenComparing(c -> c.c().id());

        List<Paso> plan = new ArrayList<>();
        Set<String> servidos = Servicio.calcular(red, esc).servidos();

        while (!pendientes.isEmpty()) {
            PriorityQueue<Candidato> cola = new PriorityQueue<>(prioridad);
            for (Componente c : pendientes) {
                Set<String> despues = Servicio.calcular(red, reparar(esc, c)).servidos();
                Set<String> nuevos = new TreeSet<>(despues);
                nuevos.removeAll(servidos);
                cola.add(new Candidato(c, nuevos));
            }
            Candidato mejor = cola.poll();
            esc = reparar(esc, mejor.c());
            servidos = Servicio.calcular(red, esc).servidos();
            pendientes.remove(mejor.c());
            plan.add(new Paso(plan.size() + 1, mejor.c().esArista() ? "ARISTA" : "NODO",
                    mejor.c().id(), mejor.c().costo(), mejor.restablecidos()));
        }
        return plan;
    }

    private static Escenario reparar(Escenario esc, Componente c) {
        return c.esArista() ? esc.sinAristaCaida(c.id()) : esc.sinNodoCaido(c.id());
    }
}
