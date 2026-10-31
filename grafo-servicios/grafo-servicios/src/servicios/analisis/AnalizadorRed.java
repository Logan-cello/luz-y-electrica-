package servicios.analisis;

import servicios.grafo.Arista;
import servicios.grafo.Grafo;
import servicios.grafo.Nodo;
import servicios.grafo.Servicio;
import servicios.modelo.RedServicios;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Analizador de la red: aplica los algoritmos de teoría de grafos sobre
 * {@link RedServicios} y devuelve {@link InformeRed}.
 *
 * <p>Algoritmos utilizados:</p>
 * <ul>
 *   <li><b>BFS</b> desde las fuentes activas: qué nodos siguen recibiendo servicio.</li>
 *   <li><b>Punto fijo</b> sobre las dependencias: un nodo cuyo requisito está caído
 *       también cae (fallo en cascada entre luz y agua).</li>
 *   <li><b>Componentes conexas</b>: cuántas "islas" quedan tras los fallos.</li>
 *   <li><b>Fuerza bruta N-1</b>: qué nodos son puntos únicos de fallo.</li>
 * </ul>
 */
public class AnalizadorRed {

    private final RedServicios red;

    public AnalizadorRed(RedServicios red) {
        this.red = red;
    }

    // ==================================================================
    // Fallo en cascada: estado de activación por punto fijo
    // ==================================================================

    /**
     * Calcula el estado real de cada nodo considerando fallos propios y
     * dependencias entre servicios. Itera hasta que no cambia nada.
     */
    public Map<String, Boolean> estadoActivacion() {
        Map<String, Boolean> activo = new LinkedHashMap<>();
        for (Nodo n : red.getIndice().values()) {
            activo.put(n.getId(), !n.estaFallado());
        }

        boolean cambio = true;
        while (cambio) {
            cambio = false;
            for (Nodo n : red.getIndice().values()) {
                if (!activo.get(n.getId())) continue;
                for (Nodo.Dependencia dep : n.getDependencias()) {
                    Boolean reqActivo = activo.get(dep.getIdNodoRequerido());
                    if (reqActivo != null && !reqActivo) {
                        activo.put(n.getId(), false);
                        cambio = true;
                        break;
                    }
                }
            }
        }
        return activo;
    }

    /** Nodos que cayeron solo porque un servicio del que dependen falló. */
    public Set<String> nodosCaidosPorDependencia(Map<String, Boolean> activo) {
        Set<String> caidos = new LinkedHashSet<>();
        for (Nodo n : red.getIndice().values()) {
            if (n.estaFallado() || activo.get(n.getId())) continue;
            for (Nodo.Dependencia dep : n.getDependencias()) {
                Boolean req = activo.get(dep.getIdNodoRequerido());
                if (req != null && !req) {
                    caidos.add(n.getId());
                    break;
                }
            }
        }
        return caidos;
    }

    // ==================================================================
    // Análisis principal
    // ==================================================================

    public InformeRed analizar(Servicio servicio) {
        return analizar(servicio, estadoActivacion());
    }

    public List<InformeRed> analizarTodo() {
        Map<String, Boolean> activo = estadoActivacion();
        List<InformeRed> informes = new ArrayList<>();
        for (Servicio s : Servicio.values()) informes.add(analizar(s, activo));
        return informes;
    }

    private InformeRed analizar(Servicio servicio, Map<String, Boolean> activo) {
        Grafo g = red.grafo(servicio);
        Set<String> caidosPorDependencia = nodosCaidosPorDependencia(activo);

        // 1) Fuentes que siguen operativas
        List<Nodo> fuentesActivas = new ArrayList<>();
        for (Nodo n : g.getNodos()) {
            if (n.esFuente() && activo.get(n.getId())) fuentesActivas.add(n);
        }

        // 2) BFS desde todas las fuentes activas
        Set<String> conServicio = alcanzables(g, fuentesActivas, activo);

        // 3) Métricas
        int nodosActivos = 0;
        int aristasFalladas = 0;
        int consumidoresTotales = 0;
        int consumidoresConServicio = 0;
        double demandaTotal = 0;
        double demandaAtendida = 0;
        List<Nodo> consumidoresSinServicio = new ArrayList<>();
        List<Nodo> nodosSinServicio = new ArrayList<>();

        for (Nodo n : g.getNodos()) {
            if (activo.get(n.getId())) nodosActivos++;
            boolean tiene = conServicio.contains(n.getId());
            if (!tiene) nodosSinServicio.add(n);
            if (n.esConsumidor()) {
                consumidoresTotales++;
                demandaTotal += n.getDemanda();
                if (tiene) {
                    consumidoresConServicio++;
                    demandaAtendida += n.getDemanda();
                } else {
                    consumidoresSinServicio.add(n);
                }
            }
        }
        for (Arista a : g.getAristas()) {
            if (a.estaFallada()) aristasFalladas++;
        }

        // 4) Componentes conexas (tratando la red como no dirigida)
        List<Set<Nodo>> componentes = componentesConexas(g, activo);

        // 5) Explicación de por qué cada consumidor se quedó sin servicio
        Map<String, String> causas = explicarPerdidas(g, consumidoresSinServicio,
                caidosPorDependencia, activo);

        return new InformeRed(servicio, g.getNodos().size(), nodosActivos,
                g.getAristas().size(), aristasFalladas,
                consumidoresTotales, consumidoresConServicio,
                demandaTotal, demandaAtendida,
                consumidoresSinServicio, nodosSinServicio,
                caidosPorDependencia, componentes, causas);
    }

    /**
     * Construye una explicación legible de la pérdida para cada consumidor
     * afectado: avería propia, cascada entre servicios, o simplemente falta de
     * camino activo desde una fuente.
     */
    private Map<String, String> explicarPerdidas(Grafo g, List<Nodo> afectados,
                                                 Set<String> caidosPorDependencia,
                                                 Map<String, Boolean> activo) {
        Map<String, String> causas = new LinkedHashMap<>();
        if (afectados.isEmpty()) return causas;

        // Nodos de esta red que cayeron solo por dependencia (ej. una bomba sin luz)
        List<Nodo> caidosAqui = new ArrayList<>();
        for (Nodo n : g.getNodos()) {
            if (caidosPorDependencia.contains(n.getId())) caidosAqui.add(n);
        }

        for (Nodo afectado : afectados) {
            if (afectado.estaFallado()) {
                causas.put(afectado.getId(), "avería propia: " + afectado.getMotivoFallo());
                continue;
            }
            if (caidosPorDependencia.contains(afectado.getId())) {
                causas.put(afectado.getId(), "cascada entre servicios: " + afectado.getDependencias());
                continue;
            }
            // ¿Aguas arriba hay un nodo caído por dependencia en su misma componente?
            List<Nodo> culpables = new ArrayList<>();
            for (Nodo c : caidosAqui) {
                if (mismaComponente(g, afectado, c, activo)) culpables.add(c);
            }
            if (!culpables.isEmpty()) {
                List<String> ids = new ArrayList<>();
                for (Nodo c : culpables) ids.add(c.getId());
                causas.put(afectado.getId(),
                        "cascada aguas arriba: sin " + String.join(", ", ids)
                                + " (dependencia eléctrica cortada)");
            } else {
                causas.put(afectado.getId(), "sin camino activo desde la fuente");
            }
        }
        return causas;
    }

    /**
     * ¿El nodo {@code b} (que puede estar caído por dependencia) está conectado
     * al nodo {@code a} dentro de la red activa? Se recorre solo por nodos
     * activos y se comprueba al final si alguno de ellos es vecino de {@code b}.
     */
    private boolean mismaComponente(Grafo g, Nodo a, Nodo b, Map<String, Boolean> activo) {
        if (!Boolean.TRUE.equals(activo.get(a.getId()))) return false;
        Set<String> vistos = new HashSet<>();
        Deque<Nodo> cola = new ArrayDeque<>();
        cola.add(a);
        vistos.add(a.getId());
        while (!cola.isEmpty()) {
            Nodo actual = cola.poll();
            if (actual.getId().equals(b.getId())) return true;
            for (Arista ar : g.incidentes(actual.getId(), false)) {
                if (ar.estaFallada()) continue;
                Nodo otro = ar.otroExtremo(actual);
                if (otro == null) continue;
                // Vecino directo del nodo caído: están en la misma componente
                if (otro.getId().equals(b.getId())) return true;
                if (!Boolean.TRUE.equals(activo.get(otro.getId()))) continue;
                if (vistos.add(otro.getId())) cola.add(otro);
            }
        }
        return false;
    }

    /** BFS dirigido desde las fuentes, respetando fallos de nodos, aristas y dependencias. */
    private Set<String> alcanzables(Grafo g, List<Nodo> fuentes, Map<String, Boolean> activo) {
        Set<String> visitados = new LinkedHashSet<>();
        Deque<Nodo> cola = new ArrayDeque<>();
        for (Nodo f : fuentes) {
            if (visitados.add(f.getId())) cola.add(f);
        }
        while (!cola.isEmpty()) {
            Nodo actual = cola.poll();
            for (Arista a : g.incidentes(actual.getId(), true)) {
                if (a.estaFallada()) continue;
                Nodo destino = a.getDestino();
                if (!Boolean.TRUE.equals(activo.get(destino.getId()))) continue;
                if (visitados.add(destino.getId())) cola.add(destino);
            }
        }
        return visitados;
    }

    /** Componentes conexas de la red activa (aristas no dirigidas, sin fallos). */
    private List<Set<Nodo>> componentesConexas(Grafo g, Map<String, Boolean> activo) {
        List<Set<Nodo>> componentes = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (Nodo inicio : g.getNodos()) {
            if (!Boolean.TRUE.equals(activo.get(inicio.getId())) || !vistos.add(inicio.getId())) continue;
            Set<Nodo> comp = new LinkedHashSet<>();
            Deque<Nodo> cola = new ArrayDeque<>();
            cola.add(inicio);
            while (!cola.isEmpty()) {
                Nodo actual = cola.poll();
                comp.add(actual);
                for (Arista a : g.incidentes(actual.getId(), false)) {
                    if (a.estaFallada()) continue;
                    Nodo otro = a.otroExtremo(actual);
                    if (otro == null) continue;
                    if (!Boolean.TRUE.equals(activo.get(otro.getId()))) continue;
                    if (vistos.add(otro.getId())) cola.add(otro);
                }
            }
            componentes.add(comp);
        }
        return componentes;
    }

    // ==================================================================
    // Puntos únicos de fallo (análisis N-1 por fuerza bruta)
    // ==================================================================

    /**
     * Devuelve los nodos cuya avería, por sí sola, deja a algún consumidor sin
     * servicio. Son los candidatos obvios a redundancia.
     *
     * <p>Se prueban todos los nodos uno por uno (fuerza bruta N-1) midiendo la
     * demanda atendida de <b>ambos</b> servicios, para detectar también los
     * puntos críticos que solo afectan al otro servicio por dependencia
     * (ej.: una subestación que alimenta una bomba de agua).</p>
     */
    public List<String> puntosUnicosDeFallo(Servicio servicio) {
        List<String> criticos = new ArrayList<>();
        Grafo g = red.grafo(servicio);
        List<InformeRed> base = analizarTodo();
        double baseLuz = base.get(0).getDemandaAtendida();
        double baseAgua = base.get(1).getDemandaAtendida();

        for (Nodo n : new ArrayList<>(g.getNodos())) {
            // Los consumidores no cuentan como "punto de fallo de la red":
            // averiarlos solo los deja sin su propia demanda (trivial).
            if (n.esFuente() || n.esConsumidor()) continue;
            boolean teniaFallo = n.estaFallado();
            String motivoPrevio = n.getMotivoFallo();
            n.fallar("prueba N-1");
            List<InformeRed> despues = analizarTodo();
            if (teniaFallo) {
                n.fallar(motivoPrevio);
            } else {
                n.reparar();
            }
            boolean afectaLuz = despues.get(0).getDemandaAtendida() + 1e-9 < baseLuz;
            boolean afectaAgua = despues.get(1).getDemandaAtendida() + 1e-9 < baseAgua;
            if (afectaLuz || afectaAgua) {
                criticos.add(n.getId());
            }
        }
        return criticos;
    }

    /** Mapa consumidor -> motivo de la pérdida (útil para reportes). */
    public Map<String, String> causasDePerdida(Servicio servicio) {
        InformeRed informe = analizar(servicio);
        Map<String, Boolean> activo = estadoActivacion();
        Set<String> porDependencia = nodosCaidosPorDependencia(activo);
        Map<String, String> causas = new HashMap<>();
        for (Nodo n : informe.getConsumidoresSinServicio()) {
            if (porDependencia.contains(n.getId())) {
                causas.put(n.getId(), "cascada: " + n.getDependencias());
            } else if (n.estaFallado()) {
                causas.put(n.getId(), "nodo averiado: " + n.getMotivoFallo());
            } else {
                causas.put(n.getId(), "sin camino activo desde la fuente");
            }
        }
        return causas;
    }
}
