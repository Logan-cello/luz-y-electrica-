package redes.algoritmos;

import redes.modelo.*;

import java.util.*;

/**
 * Determina qué nodos reciben servicio y con qué presión/tensión.
 *
 * TEORÍA DE GRAFOS: es una búsqueda multi-fuente tipo DIJKSTRA con el criterio invertido:
 * en lugar de minimizar distancia se MAXIMIZA la presión residual. Como cada arista solo resta
 * presión (peso >= 0), el primer valor definitivo de cada nodo es el óptimo (propiedad de
 * Dijkstra), y la cola de prioridad (max-heap) lo garantiza en O((V + E) log V).
 *
 * Un nodo está "conectado" si es alcanzable desde alguna fuente operativa a través de
 * aristas operativas, no más largas que la distancia máxima, y sin que la presión caiga por
 * debajo del mínimo. Es decir: componente conexa + restricción física, no zonas geográficas.
 */
public final class Servicio {
    private Servicio() { }

    /** @param presion  presión final de cada nodo alcanzado.
     *  @param servidos ids de los dispositivos finales operativos que sí reciben servicio. */
    public record Resultado(Map<String, Double> presion, Set<String> servidos) { }

    private record Entrada(String id, double presion) { }

    public static Resultado calcular(Red red, Escenario esc) {
        Grafo g = red.grafo();
        Map<String, Double> mejor = new HashMap<>();
        PriorityQueue<Entrada> cola =
                new PriorityQueue<>(Comparator.comparingDouble(Entrada::presion).reversed());

        for (Nodo f : red.fuentes()) {
            if (esc.nodoOperativo(f.id())) {
                mejor.put(f.id(), f.cargaInicial());
                cola.add(new Entrada(f.id(), f.cargaInicial()));
            }
        }

        while (!cola.isEmpty()) {
            Entrada actual = cola.poll();
            if (actual.presion() < mejor.get(actual.id())) continue; // entrada obsoleta
            Nodo u = g.nodo(actual.id());
            if (u.esDispositivo()) continue; // los consumidores son terminales: no reenvían

            for (Arista a : g.incidentes(u.id())) {
                if (!esc.aristaOperativa(a.id())) continue;
                if (a.longitud() > red.distanciaMaxima()) continue; // pérdida crítica
                String v = a.otroExtremo(u.id());
                if (!esc.nodoOperativo(v)) continue;
                double nueva = actual.presion() - red.decaimientoPorUnidad() * a.longitud();
                if (nueva < red.presionMinima()) continue;
                if (nueva > mejor.getOrDefault(v, Double.NEGATIVE_INFINITY)) {
                    mejor.put(v, nueva);
                    cola.add(new Entrada(v, nueva));
                }
            }
        }

        Set<String> servidos = new TreeSet<>();
        for (Nodo d : red.dispositivos())
            if (esc.nodoOperativo(d.id()) && mejor.containsKey(d.id())) servidos.add(d.id());
        return new Resultado(mejor, servidos);
    }
}
