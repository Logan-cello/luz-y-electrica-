package servicios.reporte;

import servicios.analisis.AnalizadorRed;
import servicios.analisis.InformeRed;
import servicios.grafo.Arista;
import servicios.grafo.Grafo;
import servicios.grafo.Nodo;
import servicios.grafo.Servicio;
import servicios.modelo.RedServicios;
import servicios.simulacion.EventoFallo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Genera la salida legible del sistema: informe por servicio, topología en
 * texto, historial de fallos y volcado a CSV.
 */
public class Reporte {

    private final static String SEP = "=".repeat(78);

    // ------------------------------------------------------------------
    // Informe completo
    // ------------------------------------------------------------------

    public static String informeCompleto(RedServicios red, AnalizadorRed analizador,
                                         List<EventoFallo> historial) {
        StringBuilder sb = new StringBuilder();
        sb.append(SEP).append('\n');
        sb.append("  INFORME DE RED DE LUZ Y AGUA  (grafo dirigido + dependencias)\n");
        sb.append(SEP).append('\n');

        Map<String, Boolean> activo = analizador.estadoActivacion();
        Set<String> cascada = analizador.nodosCaidosPorDependencia(activo);
        if (!cascada.isEmpty()) {
            sb.append("  Fallos en cascada detectados (caen por dependencia de otro servicio): ")
              .append(String.join(", ", cascada)).append("\n\n");
        }

        for (Servicio s : Servicio.values()) {
            informeServicio(sb, red, analizador.analizar(s), historial);
        }

        sb.append(SEP).append('\n');
        sb.append("  HISTORIAL DE EVENTOS (").append(historial.size()).append(")\n");
        sb.append(SEP).append('\n');
        if (historial.isEmpty()) {
            sb.append("  (sin eventos registrados)\n");
        } else {
            for (EventoFallo e : historial) sb.append("  ").append(e).append('\n');
        }
        return sb.toString();
    }

    private static void informeServicio(StringBuilder sb, RedServicios red,
                                        InformeRed inf, List<EventoFallo> historial) {
        sb.append("- ").append(inf.getServicio().getEtiqueta().toUpperCase()).append('\n');
        sb.append(inf.resumenCorto()).append('\n');
        sb.append(String.format("  Nodos activos: %d/%d   Aristas: %d (averiadas: %d)   Componentes conexas: %d%n",
                inf.getNodosActivos(), inf.getNodosTotales(),
                inf.getAristasTotales(), inf.getAristasFalladas(),
                inf.getComponentes().size()));

        if (!inf.getConsumidoresSinServicio().isEmpty()) {
            sb.append("  Consumidores sin servicio:\n");
            for (String linea : inf.detalleAfectados()) sb.append(linea).append('\n');
        } else {
            sb.append("  Todos los consumidores reciben servicio.\n");
        }

        Grafo g = red.grafo(inf.getServicio());
        List<String> caidos = new ArrayList<>();
        for (Nodo n : g.getNodos()) if (n.estaFallado()) caidos.add(n.getId());
        List<String> conexionesCaidas = new ArrayList<>();
        for (Arista a : g.getAristas()) if (a.estaFallada()) conexionesCaidas.add(a.getId());
        if (!caidos.isEmpty() || !conexionesCaidas.isEmpty()) {
            sb.append("  Elementos averiados -> nodos: ")
              .append(caidos.isEmpty() ? "-" : String.join(", ", caidos))
              .append(" | conexiones: ")
              .append(conexionesCaidas.isEmpty() ? "-" : String.join(", ", conexionesCaidas))
              .append('\n');
        }
        sb.append('\n');
    }

    // ------------------------------------------------------------------
    // Topología en texto (esquema ASCII)
    // ------------------------------------------------------------------

    public static String topologiaEnTexto(RedServicios red, boolean funcionando,
                                          Map<String, Boolean> activo) {
        StringBuilder sb = new StringBuilder();
        sb.append(SEP).append('\n');
        sb.append("  TOPOLOGÍA DE LAS REDES\n");
        sb.append(SEP).append('\n');
        for (Grafo g : red.grafos()) {
            sb.append('[').append(g.getNombre()).append("] ")
              .append(g.getNodos().size()).append(" nodos, ")
              .append(g.getAristas().size()).append(" conexiones\n");
            for (Nodo n : g.getNodos()) {
                boolean conServicio = funcionando
                        ? Boolean.TRUE.equals(activo.get(n.getId()))
                        : !n.estaFallado();
                String marca = n.estaFallado() ? "[X]" : (conServicio ? "[*]" : "[ ]");
                sb.append(String.format("  %s %-9s %-26s %-22s demanda=%5.0f %s%n",
                        marca, n.getId(), n.getNombre(), n.getCategoria().getEtiqueta(),
                        n.getDemanda(), n.getServicio().getUnidad()));
            }
            sb.append("  Conexiones:\n");
            for (Arista a : g.getAristas()) {
                String marca = a.estaFallada() ? "[X]" : "[ ]";
                sb.append(String.format("    %s %-4s %-9s -> %-9s cap=%6.0f  long=%.1f%n",
                        marca, a.getId(), a.getOrigen().getId(), a.getDestino().getId(),
                        a.getCapacidad(), a.getLongitud()));
            }
            sb.append('\n');
        }
        sb.append("  Leyenda: [*] operativo con servicio   [ ] sin servicio   [X] averiado\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Volcado CSV
    // ------------------------------------------------------------------

    public static String aCsv(RedServicios red, List<InformeRed> informes) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,nombre,servicio,categoria,demanda,estado\n");
        for (Grafo g : red.grafos()) {
            for (Nodo n : g.getNodos()) {
                sb.append(String.join(",",
                        n.getId(),
                        '"' + n.getNombre() + '"',
                        n.getServicio().name(),
                        n.getCategoria().name(),
                        String.format("%.0f", n.getDemanda()),
                        n.estaFallado() ? "FALLADO" : "OK")).append('\n');
            }
        }
        sb.append('\n');
        sb.append("servicio,demanda_total,demanda_atendida,demanda_perdida,porcentaje_servicio,consumidores_sin_servicio\n");
        for (InformeRed i : informes) {
            sb.append(String.format("%s,%.0f,%.0f,%.0f,%.2f,%d%n",
                    i.getServicio().name(), i.getDemandaTotal(), i.getDemandaAtendida(),
                    i.getDemandaPerdida(), i.getPorcentajeServicio(),
                    i.getConsumidoresSinServicio().size()));
        }
        return sb.toString();
    }

    /** Barra de progreso simple para mostrar porcentajes en consola. */
    public static String barra(double porcentaje, int ancho) {
        int llenos = (int) Math.round(porcentaje / 100.0 * ancho);
        llenos = Math.max(0, Math.min(ancho, llenos));
        return "[" + "#".repeat(llenos) + ".".repeat(ancho - llenos) + "]";
    }
}
