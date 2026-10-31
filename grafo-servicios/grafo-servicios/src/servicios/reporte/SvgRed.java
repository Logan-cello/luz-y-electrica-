package servicios.reporte;

import servicios.analisis.AnalizadorRed;
import servicios.analisis.InformeRed;
import servicios.grafo.Arista;
import servicios.grafo.Grafo;
import servicios.grafo.Nodo;
import servicios.modelo.RedServicios;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Exporta la red a un archivo SVG usando las coordenadas (x, y) de cada nodo.
 *
 * <p>Colores: azul = red de agua, ámbar = red eléctrica. Los elementos averiados
 * se pintan en rojo y las líneas de dependencia entre servicios en morado
 * discontinuo. Abre el SVG en cualquier navegador (o en el visor de la app).</p>
 */
public class SvgRed {

    private static final int ANCHO = 1000;
    private static final int ALTO = 780;
    private static final int MARGEN_Y = 70;

    private static final String COLOR_LUZ = "#e8a33d";
    private static final String COLOR_AGUA = "#2f8fd8";
    private static final String COLOR_FALLO = "#d63b3b";
    private static final String COLOR_DEP = "#8b5cf6";
    private static final String FONDO = "#101725";

    /**
     * Genera el SVG con el estado real de la red: averías y consumidores que se
     * quedaron sin servicio (incluidas las caídas en cascada).
     */
    public static String generar(RedServicios red, AnalizadorRed analizador) {
        Map<String, Boolean> activo = analizador.estadoActivacion();
        Set<String> sinServicio = new HashSet<>();
        for (InformeRed inf : analizador.analizarTodo()) {
            for (Nodo n : inf.getNodosSinServicio()) sinServicio.add(n.getId());
        }
        return generar(red, activo, sinServicio);
    }

    public static String generar(RedServicios red, Map<String, Boolean> activo) {
        return generar(red, activo, Set.of());
    }

    public static String generar(RedServicios red, Map<String, Boolean> activo,
                                 Set<String> sinServicio) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append(String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\" "
                        + "font-family=\"Segoe UI, Helvetica, Arial, sans-serif\">%n",
                ANCHO, ALTO, ANCHO, ALTO));
        sb.append(dibujarDefiniciones());
        sb.append(String.format("<rect width=\"%d\" height=\"%d\" fill=\"%s\"/>%n", ANCHO, ALTO, FONDO));
        sb.append(titulo());

        // 1) Aristas
        sb.append("<g id=\"aristas\">\n");
        for (Grafo g : red.grafos()) {
            String color = g.getServicio().name().equals("LUZ") ? COLOR_LUZ : COLOR_AGUA;
            for (Arista a : g.getAristas()) {
                sb.append(arista(a, a.estaFallada() ? COLOR_FALLO : color));
            }
        }
        sb.append("</g>\n");

        // 2) Dependencias entre servicios
        sb.append("<g id=\"dependencias\">\n");
        for (Nodo n : red.getIndice().values()) {
            for (Nodo.Dependencia d : n.getDependencias()) {
                Nodo requerido = red.nodo(d.getIdNodoRequerido());
                if (requerido == null) continue;
                sb.append(String.format(
                        "<line x1=\"%.0f\" y1=\"%.0f\" x2=\"%.0f\" y2=\"%.0f\" stroke=\"%s\" "
                                + "stroke-width=\"1.4\" stroke-dasharray=\"6 5\" opacity=\"0.55\" marker-end=\"url(#flechaDep)\"/>%n",
                        px(n), n.getY() + MARGEN_Y, px(requerido), requerido.getY() + MARGEN_Y, COLOR_DEP));
            }
        }
        sb.append("</g>\n");

        // 3) Nodos
        sb.append("<g id=\"nodos\">\n");
        for (Grafo g : red.grafos()) {
            for (Nodo n : g.getNodos()) {
                boolean conServicio = (activo == null
                        ? !n.estaFallado()
                        : Boolean.TRUE.equals(activo.get(n.getId())))
                        && !sinServicio.contains(n.getId());
                sb.append(nodo(n, conServicio));
            }
        }
        sb.append("</g>\n");

        sb.append(leyenda());
        sb.append("</svg>\n");
        return sb.toString();
    }

    /**
     * Coloca la red eléctrica en la mitad izquierda del plano y la de agua en la
     * derecha, para que ambos grafos se lean por separado.
     */
    private static double px(Nodo n) {
        return n.getServicio().name().equals("AGUA") ? n.getX() + 40 : n.getX() + 60;
    }

    private static String dibujarDefiniciones() {
        return "<defs>\n"
                + "  <marker id=\"flechaLuz\" markerWidth=\"10\" markerHeight=\"10\" refX=\"9\" refY=\"3\" "
                + "orient=\"auto\" markerUnits=\"strokeWidth\">"
                + "<path d=\"M0,0 L0,6 L9,3 z\" fill=\"" + COLOR_LUZ + "\"/></marker>\n"
                + "  <marker id=\"flechaAgua\" markerWidth=\"10\" markerHeight=\"10\" refX=\"9\" refY=\"3\" "
                + "orient=\"auto\" markerUnits=\"strokeWidth\">"
                + "<path d=\"M0,0 L0,6 L9,3 z\" fill=\"" + COLOR_AGUA + "\"/></marker>\n"
                + "  <marker id=\"flechaFallo\" markerWidth=\"10\" markerHeight=\"10\" refX=\"9\" refY=\"3\" "
                + "orient=\"auto\" markerUnits=\"strokeWidth\">"
                + "<path d=\"M0,0 L0,6 L9,3 z\" fill=\"" + COLOR_FALLO + "\"/></marker>\n"
                + "  <marker id=\"flechaDep\" markerWidth=\"10\" markerHeight=\"10\" refX=\"9\" refY=\"3\" "
                + "orient=\"auto\" markerUnits=\"strokeWidth\">"
                + "<path d=\"M0,0 L0,6 L9,3 z\" fill=\"" + COLOR_DEP + "\"/></marker>\n"
                + "</defs>\n";
    }

    private static String titulo() {
        return "<text x=\"24\" y=\"42\" fill=\"#e8eef8\" font-size=\"21\" font-weight=\"700\">"
                + "Red de luz y agua &#8212; grafo dirigido con fallos</text>\n"
                + "<text x=\"24\" y=\"64\" fill=\"#8fa3bf\" font-size=\"13\">"
                + "Rojo = elemento averiado / sin servicio &#183; morado discontinuo = dependencia entre servicios"
                + "</text>\n"
                + "<text x=\"120\" y=\"132\" fill=\"#e8a33d\" font-size=\"12\" font-weight=\"700\" "
                + "letter-spacing=\"1.5\">RED ELÉCTRICA</text>\n"
                + "<text x=\"700\" y=\"132\" fill=\"#2f8fd8\" font-size=\"12\" font-weight=\"700\" "
                + "letter-spacing=\"1.5\">RED DE AGUA</text>\n"
                + "<line x1=\"520\" y1=\"80\" x2=\"520\" y2=\"690\" stroke=\"#243149\" stroke-width=\"1\" "
                + "stroke-dasharray=\"4 6\"/>\n";
    }

    private static String arista(Arista a, String color) {
        String marcador = color.equals(COLOR_FALLO) ? "flechaFallo"
                : (color.equals(COLOR_LUZ) ? "flechaLuz" : "flechaAgua");
        double x1 = px(a.getOrigen());
        double y1 = a.getOrigen().getY() + MARGEN_Y;
        double x2 = px(a.getDestino());
        double y2 = a.getDestino().getY() + MARGEN_Y;

        // Acortamos la línea para que la punta de flecha no quede bajo el círculo
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        double r = 19;
        double ux = len == 0 ? 0 : dx / len;
        double uy = len == 0 ? 0 : dy / len;
        double xs = x1 + ux * r, ys = y1 + uy * r;
        double xe = x2 - ux * (r + 3), ye = y2 - uy * (r + 3);

        return String.format(
                "<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"%s\" stroke-width=\"%s\" "
                        + "marker-end=\"url(#%s)\">%s%s</line>%n",
                xs, ys, xe, ye, color, a.estaFallada() ? "2.6" : "1.8", marcador,
                a.estaFallada() ? "<title>" + esc(a.toString()) + " (AVERIADA: " + esc(a.getMotivoFallo()) + ")</title>" : "",
                a.estaFallada() ? "" : "<title>" + esc(a.toString()) + "</title>");
    }

    private static String nodo(Nodo n, boolean conServicio) {
        double cx = px(n);
        double cy = n.getY() + MARGEN_Y;
        String relleno = FONDO;
        String borde;

        if (n.estaFallado()) {
            borde = COLOR_FALLO;
        } else if (!conServicio) {
            borde = COLOR_FALLO;
        } else {
            borde = n.getServicio().name().equals("LUZ") ? COLOR_LUZ : COLOR_AGUA;
        }
        boolean apagado = n.estaFallado() || !conServicio;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "<circle cx=\"%.0f\" cy=\"%.0f\" r=\"17\" fill=\"%s\" stroke=\"%s\" stroke-width=\"2.4\" "
                        + "opacity=\"%s\"><title>%s | %s | %s | demanda %.0f %s%s</title></circle>%n",
                cx, cy, relleno, borde, apagado ? "0.55" : "1",
                esc(n.getId()), esc(n.getNombre()), n.getCategoria().getEtiqueta(),
                n.getDemanda(), n.getServicio().getUnidad(),
                n.estaFallado() ? " | AVERIADO: " + esc(n.getMotivoFallo()) : ""));

        // Icono: cuadrado = fuente, rombo = consumidor, círculo = transporte
        if (n.esFuente()) {
            sb.append(String.format(
                    "<rect x=\"%.0f\" y=\"%.0f\" width=\"13\" height=\"13\" fill=\"%s\" transform=\"rotate(45 %.0f %.0f)\"/>%n",
                    cx - 6.5, cy - 6.5, borde, cx, cy));
        } else if (n.esConsumidor()) {
            sb.append(String.format(
                    "<rect x=\"%.0f\" y=\"%.0f\" width=\"12\" height=\"12\" fill=\"%s\"/>%n",
                    cx - 6, cy - 6, borde));
        } else {
            sb.append(String.format("<circle cx=\"%.0f\" cy=\"%.0f\" r=\"4.5\" fill=\"%s\"/>%n", cx, cy, borde));
        }

        sb.append(String.format(
                "<text x=\"%.0f\" y=\"%.0f\" fill=\"#cfdcee\" font-size=\"10.5\" text-anchor=\"middle\">%s</text>%n",
                cx, cy + 31, esc(n.getId())));
        if (n.esConsumidor() && n.getDemanda() > 0) {
            sb.append(String.format(
                    "<text x=\"%.0f\" y=\"%.0f\" fill=\"#7f92ad\" font-size=\"9\" text-anchor=\"middle\">%.0f %s</text>%n",
                    cx, cy + 43, n.getDemanda(), n.getServicio().getUnidad()));
        }
        return sb.toString();
    }

    private static String leyenda() {
        double x = 24, y = ALTO - 38;
        return "<g id=\"leyenda\">\n"
                + "<text x=\"" + x + "\" y=\"" + (y - 14) + "\" fill=\"#8fa3bf\" font-size=\"11\">"
                + "&#9679; transporte &#160;&#160; &#9632; consumidor &#160;&#160; &#9670; fuente"
                + "</text>\n"
                + "<line x1=\"" + x + "\" y1=\"" + y + "\" x2=\"" + (x + 34) + "\" y2=\"" + y
                + "\" stroke=\"" + COLOR_LUZ + "\" stroke-width=\"2\"/>\n"
                + "<text x=\"" + (x + 42) + "\" y=\"" + (y + 4) + "\" fill=\"#cfdcee\" font-size=\"11\">Red eléctrica</text>\n"
                + "<line x1=\"" + (x + 150) + "\" y1=\"" + y + "\" x2=\"" + (x + 184) + "\" y2=\"" + y
                + "\" stroke=\"" + COLOR_AGUA + "\" stroke-width=\"2\"/>\n"
                + "<text x=\"" + (x + 192) + "\" y=\"" + (y + 4) + "\" fill=\"#cfdcee\" font-size=\"11\">Red de agua</text>\n"
                + "<line x1=\"" + (x + 290) + "\" y1=\"" + y + "\" x2=\"" + (x + 324) + "\" y2=\"" + y
                + "\" stroke=\"" + COLOR_DEP + "\" stroke-width=\"1.4\" stroke-dasharray=\"6 5\"/>\n"
                + "<text x=\"" + (x + 332) + "\" y=\"" + (y + 4) + "\" fill=\"#cfdcee\" font-size=\"11\">Dependencia</text>\n"
                + "<line x1=\"" + (x + 440) + "\" y1=\"" + y + "\" x2=\"" + (x + 474) + "\" y2=\"" + y
                + "\" stroke=\"" + COLOR_FALLO + "\" stroke-width=\"2.6\"/>\n"
                + "<text x=\"" + (x + 482) + "\" y=\"" + (y + 4) + "\" fill=\"#cfdcee\" font-size=\"11\">Avería / sin servicio</text>\n"
                + "</g>\n";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
