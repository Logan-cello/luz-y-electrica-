package redes.algoritmos;

import redes.modelo.*;

import java.util.*;

/**
 * Simulación de CAPACIDAD mediante FLUJO MÁXIMO (Edmonds-Karp = Ford-Fulkerson con BFS).
 *
 * TEORÍA DE GRAFOS: se construye una red de flujo:
 *   - Súper-origen S conectado a cada fuente operativa.
 *   - Cada nodo se "parte" en (entrada -> salida) con arco de capacidad = capacidad del nodo,
 *     de modo que también se respetan los límites de cajas/subestaciones (técnica de división
 *     de vértices).
 *   - Cada arista no dirigida se modela con dos arcos de capacidad c.
 *   - Cada dispositivo se conecta a un súper-destino T con capacidad = su consumo.
 * El flujo máximo S->T es la demanda total que la red puede entregar; el flujo que llega a cada
 * dispositivo permite detectar déficits por cuellos de botella (corte mínimo) aunque haya
 * conectividad. Complejidad O(V E^2).
 */
public final class FlujoCapacidad {
    private FlujoCapacidad() { }

    private static final double INF = 1e15;
    private static final double EPS = 1e-9;

    public record Entrega(String dispositivo, double demanda, double entregado) {
        public double deficit() { return demanda - entregado; }
    }

    // Lista de arcos con arcos residuales apareados (i, i^1).
    private static final class Residual {
        final List<List<Integer>> adj = new ArrayList<>();
        final List<Integer> hacia = new ArrayList<>();
        final List<Double> cap = new ArrayList<>();

        Residual(int n) { for (int i = 0; i < n; i++) adj.add(new ArrayList<>()); }

        int arco(int u, int v, double capUV, double capVU) {
            int idx = hacia.size();
            hacia.add(v); cap.add(capUV); adj.get(u).add(idx);
            hacia.add(u); cap.add(capVU); adj.get(v).add(idx + 1);
            return idx;
        }
    }

    public static List<Entrega> simular(Red red, Escenario esc) {
        Servicio.Resultado sv = Servicio.calcular(red, esc);
        List<String> ids = new ArrayList<>(sv.presion().keySet());
        Collections.sort(ids);
        Map<String, Integer> pos = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) pos.put(ids.get(i), i);

        int n = ids.size();
        int S = 2 * n, T = 2 * n + 1;
        Residual r = new Residual(2 * n + 2);
        Map<String, Integer> arcoDestino = new LinkedHashMap<>();

        for (String id : ids) {
            Nodo nodo = red.grafo().nodo(id);
            int in = 2 * pos.get(id), out = in + 1;
            r.arco(in, out, nodo.capacidad() > 0 ? nodo.capacidad() : INF, 0);
            if (nodo.esFuente()) r.arco(S, in, INF, 0);
            if (nodo.esDispositivo()) arcoDestino.put(id, r.arco(out, T, nodo.consumo(), 0));
        }
        for (Arista a : red.grafo().aristas()) {
            if (!esc.aristaOperativa(a.id()) || a.longitud() > red.distanciaMaxima()) continue;
            Integer u = pos.get(a.origen()), v = pos.get(a.destino());
            if (u == null || v == null || u.equals(v)) continue;
            double c = a.capacidad() > 0 ? a.capacidad() : INF;
            boolean origenEsDisp = red.grafo().nodo(a.origen()).esDispositivo();
            boolean destinoEsDisp = red.grafo().nodo(a.destino()).esDispositivo();
            // out(u) -> in(v) y out(v) -> in(u); un dispositivo (terminal) nunca reenvía.
            if (!origenEsDisp) r.arco(2 * u + 1, 2 * v, c, 0);
            if (!destinoEsDisp) r.arco(2 * v + 1, 2 * u, c, 0);
        }

        edmondsKarp(r, S, T);

        List<Entrega> res = new ArrayList<>();
        for (Map.Entry<String, Integer> e : arcoDestino.entrySet()) {
            double demanda = red.grafo().nodo(e.getKey()).consumo();
            double entregado = r.cap.get(e.getValue() ^ 1); // flujo = capacidad del arco inverso
            res.add(new Entrega(e.getKey(), demanda, entregado));
        }
        return res;
    }

    private static void edmondsKarp(Residual r, int S, int T) {
        int n = r.adj.size();
        while (true) {
            int[] arcoPrevio = new int[n];
            Arrays.fill(arcoPrevio, -1);
            boolean[] visto = new boolean[n];
            Deque<Integer> cola = new ArrayDeque<>();
            cola.add(S);
            visto[S] = true;
            while (!cola.isEmpty() && !visto[T]) {
                int u = cola.poll();
                for (int idx : r.adj.get(u)) {
                    int v = r.hacia.get(idx);
                    if (!visto[v] && r.cap.get(idx) > EPS) {
                        visto[v] = true;
                        arcoPrevio[v] = idx;
                        cola.add(v);
                    }
                }
            }
            if (!visto[T]) return;
            double cuello = INF;
            for (int v = T; v != S; v = r.hacia.get(arcoPrevio[v] ^ 1))
                cuello = Math.min(cuello, r.cap.get(arcoPrevio[v]));
            for (int v = T; v != S; v = r.hacia.get(arcoPrevio[v] ^ 1)) {
                int idx = arcoPrevio[v];
                r.cap.set(idx, r.cap.get(idx) - cuello);
                r.cap.set(idx ^ 1, r.cap.get(idx ^ 1) + cuello);
            }
        }
    }
}
