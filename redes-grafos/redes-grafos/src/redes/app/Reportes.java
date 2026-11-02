package redes.app;

import redes.algoritmos.*;
import redes.modelo.*;

import java.util.*;

/** Presentación por consola de las salidas del sistema (separada de la lógica de grafos). */
public final class Reportes {
    private Reportes() { }

    public static void resumen(Red red) {
        System.out.println(red.resumen());
        System.out.printf("Distancia máxima: %.0f m | Presión mínima: %.1f %s | Decaimiento: %.3f/m%n",
                red.distanciaMaxima(), red.presionMinima(), red.tipo().unidadCarga(),
                red.decaimientoPorUnidad());
        Escenario e = red.escenario();
        System.out.println("Aristas caídas: " + (e.aristasCaidas().isEmpty() ? "ninguna" : e.aristasCaidas()));
        System.out.println("Nodos averiados: " + (e.nodosCaidos().isEmpty() ? "ninguno" : e.nodosCaidos()));
    }

    /** SALIDA 1: dispositivos que tenían servicio de diseño y ya no lo reciben. */
    public static void afectados(Red red) {
        titulo("1. DISPOSITIVOS AFECTADOS");
        Set<String> base = Servicio.calcular(red, Escenario.SIN_FALLAS).servidos();
        Set<String> actual = Servicio.calcular(red, red.escenario()).servidos();
        List<String> afectados = new ArrayList<>(base);
        afectados.removeAll(actual);
        Collections.sort(afectados);

        for (String id : afectados) {
            Nodo d = red.grafo().nodo(id);
            String motivo = red.escenario().nodoOperativo(id)
                    ? "sin conectividad o presión insuficiente" : "dispositivo averiado";
            System.out.printf("  %-4s %-16s demanda %5.1f %s  -> %s%n",
                    id, d.nombre(), d.consumo(), red.tipo().unidadFlujo(), motivo);
        }
        System.out.printf("Afectados: %d de %d | Con servicio: %s%n",
                afectados.size(), base.size(), actual);
    }

    /** SALIDA 2: plan de reparación ordenado por impacto. */
    public static void plan(Red red) {
        titulo("2. PLAN DE REPARACIÓN PRIORIZADO");
        List<PlanReparacion.Paso> plan = PlanReparacion.calcular(red);
        if (plan.isEmpty()) { System.out.println("  No hay fallas registradas."); return; }
        for (PlanReparacion.Paso p : plan) {
            System.out.printf("  %d. Reparar %-6s %-4s costo %6.1f  -> restablece %d dispositivo(s) %s%n",
                    p.orden(), p.tipo(), p.id(), p.costo(), p.restablecidos().size(),
                    p.restablecidos().isEmpty() ? "(solo redundancia)" : p.restablecidos());
        }
    }

    /** SALIDA 3: aristas de corte y puntos de articulación. */
    public static void vulnerabilidad(Red red) {
        titulo("3. ANÁLISIS DE VULNERABILIDAD (puentes y puntos de articulación)");
        List<Vulnerabilidad.Hallazgo> hs = Vulnerabilidad.analizar(red);
        int criticos = 0;
        for (Vulnerabilidad.Hallazgo h : hs) {
            if (h.aislados().isEmpty()) continue; // estructuralmente puente, pero con respaldo de otra fuente
            String marca = h.porcentaje() >= 50 ? "!! CRITICO" : (h.porcentaje() >= 25 ? "!  ALTO   " : "   medio   ");
            if (h.porcentaje() >= 25) criticos++;
            System.out.printf("  %s %-12s %-5s %-34s aisla %d (%.0f%%) %s%n", marca, h.clase(), h.id(),
                    h.descripcion(), h.aislados().size(), h.porcentaje(), h.aislados());
        }
        long conRespaldo = hs.stream().filter(h -> h.aislados().isEmpty()).count();
        System.out.printf("Componentes que aíslan >=25%% de los usuarios: %d | Puentes/articulaciones con respaldo "
                + "de otra fuente (sin pérdida): %d%n", criticos, conRespaldo);
    }

    /** Extra: simulación de capacidad con flujo máximo sobre el escenario actual. */
    public static void capacidad(Red red) {
        titulo("4. SIMULACIÓN DE CAPACIDAD (flujo máximo)");
        double dem = 0, ent = 0;
        for (FlujoCapacidad.Entrega e : FlujoCapacidad.simular(red, red.escenario())) {
            dem += e.demanda();
            ent += e.entregado();
            System.out.printf("  %-4s demanda %5.1f entregado %5.1f %s%s%n", e.dispositivo(), e.demanda(),
                    e.entregado(), red.tipo().unidadFlujo(),
                    e.deficit() > 1e-6 ? String.format("  (déficit %.1f)", e.deficit()) : "");
        }
        System.out.printf("Total entregado %.1f de %.1f %s demandados por los dispositivos conectados%n",
                ent, dem, red.tipo().unidadFlujo());
    }

    private static void titulo(String t) { System.out.println("\n=== " + t + " ==="); }
}
