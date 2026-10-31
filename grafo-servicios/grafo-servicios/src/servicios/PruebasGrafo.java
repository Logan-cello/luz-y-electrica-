package servicios;

import servicios.analisis.AnalizadorRed;
import servicios.analisis.InformeRed;
import servicios.grafo.Servicio;
import servicios.modelo.RedServicios;
import servicios.simulacion.SimuladorFallas;

import java.util.List;
import java.util.Map;

/**
 * Pruebas automáticas sin dependencias externas (no hace falta JUnit).
 * Se ejecutan con: {@code java servicios.PruebasGrafo}
 *
 * <p>Salida con código 1 si alguna prueba falla, para poder encadenarlo en CI.</p>
 */
public class PruebasGrafo {

    private static int pruebas = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        probarRedSana();
        probarFalloTransformador();
        probarApagonSubestacion();
        probarCascadaBombaSinLuz();
        probarRoturaTuberia();
        probarCaminoMasCorto();
        probarReparacionTotal();
        probarRobustez();

        System.out.println("\n" + "=".repeat(60));
        System.out.printf("RESULTADO: %d pruebas, %d fallidas%n", pruebas, fallos);
        System.out.println("=".repeat(60));
        if (fallos > 0) System.exit(1);
    }

    // ------------------------------------------------------------------
    // Casos
    // ------------------------------------------------------------------

    private static void probarRedSana() {
        RedServicios red = new RedServicios();
        AnalizadorRed an = new AnalizadorRed(red);
        List<InformeRed> inf = an.analizarTodo();
        afirmar("red sana: luz al 100%", inf.get(0).getPorcentajeServicio() == 100.0,
                "obtenido " + inf.get(0).getPorcentajeServicio());
        afirmar("red sana: agua al 100%", inf.get(1).getPorcentajeServicio() == 100.0,
                "obtenido " + inf.get(1).getPorcentajeServicio());
        afirmar("red sana: sin fallos en cascada", an.nodosCaidosPorDependencia(an.estadoActivacion()).isEmpty(),
                "cascada inesperada");
        afirmar("red sana: 16 nodos de luz", inf.get(0).getNodosTotales() == 16,
                "obtenido " + inf.get(0).getNodosTotales());
        afirmar("red sana: 14 nodos de agua", inf.get(1).getNodosTotales() == 14,
                "obtenido " + inf.get(1).getNodosTotales());
    }

    private static void probarFalloTransformador() {
        RedServicios red = new RedServicios();
        SimuladorFallas sim = new SimuladorFallas(red);
        sim.fallarNodo("TRF-3", "prueba");
        InformeRed luz = sim.getAnalizador().analizar(Servicio.LUZ);
        // TRF-3 alimenta POS-3, pero POS-3 también recibe por el anillo POS-2..POS-4
        afirmar("TRF-3 caído: la red sigue sirviendo (redundancia)",
                luz.getPorcentajeServicio() == 100.0,
                "servicio " + luz.getPorcentajeServicio());
    }

    private static void probarApagonSubestacion() {
        RedServicios red = new RedServicios();
        SimuladorFallas sim = new SimuladorFallas(red);
        sim.fallarNodo("SUB-1", "prueba");
        List<InformeRed> inf = sim.getAnalizador().analizarTodo();
        InformeRed luz = inf.get(0);
        afirmar("SUB-1 caído: hay pérdida de servicio eléctrico",
                luz.getPorcentajeServicio() < 100.0,
                "servicio " + luz.getPorcentajeServicio());
        // SUB-1 alimenta BMB-2 (dependencia), por lo tanto el agua también sufre
        afirmar("SUB-1 caído: el agua sufre por la bomba BMB-2 (cascada)",
                inf.get(1).getPorcentajeServicio() < 100.0,
                "servicio agua " + inf.get(1).getPorcentajeServicio());
        afirmar("SUB-1 caído: BMB-2 aparece como caído por dependencia",
                sim.getAnalizador().nodosCaidosPorDependencia(
                        sim.getAnalizador().estadoActivacion()).contains("BMB-2"),
                "no se marcó la cascada");
    }

    private static void probarCascadaBombaSinLuz() {
        RedServicios red = new RedServicios();
        SimuladorFallas sim = new SimuladorFallas(red);
        sim.fallarNodo("SUB-1", "corte eléctrico");
        sim.fallarNodo("TANQ-2", "avería del tanque sur");
        InformeRed agua = sim.getAnalizador().analizar(Servicio.AGUA);
        afirmar("BMB-2 sin luz: el tanque y consumidores del sur pierden agua",
                agua.getPorcentajeServicio() < 100.0,
                "servicio " + agua.getPorcentajeServicio());
    }

    private static void probarRoturaTuberia() {
        RedServicios red = new RedServicios();
        SimuladorFallas sim = new SimuladorFallas(red);
        sim.fallarNodo("EMP-1", "rotura");
        InformeRed agua = sim.getAnalizador().analizar(Servicio.AGUA);
        afirmar("EMP-1 caído: hospital e industria sin agua",
                agua.getConsumidoresSinServicio().size() >= 2,
                "afectados " + agua.getConsumidoresSinServicio().size());
        afirmar("EMP-1 caído: la luz no se ve afectada",
                sim.getAnalizador().analizar(Servicio.LUZ).getPorcentajeServicio() == 100.0,
                "la luz se vio afectada");
    }

    private static void probarCaminoMasCorto() {
        RedServicios red = new RedServicios();
        var camino = red.getLuz().caminoMasCorto("PLT-1", "VIV-3-E");
        afirmar("Dijkstra encuentra camino PLT-1 -> VIV-3-E", !camino.isEmpty(),
                "sin camino");
        afirmar("Dijkstra: el camino empieza en la planta",
                !camino.isEmpty() && camino.get(0).getId().equals("PLT-1"),
                "camino " + camino);

        // Si cortamos la ruta por el anillo, Dijkstra debe replanificar
        red.arista("E14").fallar("corte");
        var alterno = red.getLuz().caminoMasCorto("PLT-1", "VIV-3-E");
        afirmar("Dijkstra replanifica al cortar E14 y sigue llegando",
                !alterno.isEmpty(), "sin camino alternativo");
    }

    private static void probarReparacionTotal() {
        RedServicios red = new RedServicios();
        SimuladorFallas sim = new SimuladorFallas(red);
        sim.aplicarEscenario("5");
        sim.repararTodo();
        List<InformeRed> inf = sim.getAnalizador().analizarTodo();
        afirmar("tras reparar todo, luz al 100%", inf.get(0).getPorcentajeServicio() == 100.0,
                "servicio " + inf.get(0).getPorcentajeServicio());
        afirmar("tras reparar todo, agua al 100%", inf.get(1).getPorcentajeServicio() == 100.0,
                "servicio " + inf.get(1).getPorcentajeServicio());
    }

    private static void probarRobustez() {
        RedServicios red = new RedServicios();
        SimuladorFallas sim = new SimuladorFallas(red, new java.util.Random(42));
        SimuladorFallas.ResultadoRobustez r = sim.robustez(100);
        afirmar("robustez: se evalúan 100 corridas", r.getRepeticiones() == 100,
                "repeticiones " + r.getRepeticiones());
        afirmar("robustez: la pérdida media de luz no es negativa",
                r.getPerdidaLuzPromedio() >= 0, "valor " + r.getPerdidaLuzPromedio());
        afirmar("robustez: al terminar, la red queda sana",
                sim.getAnalizador().analizar(Servicio.LUZ).getPorcentajeServicio() == 100.0,
                "red no restaurada");
        System.out.println("  " + r);
    }

    // ------------------------------------------------------------------
    // Mini framework de aserciones
    // ------------------------------------------------------------------

    private static void afirmar(String nombre, boolean condicion, String detalle) {
        pruebas++;
        if (condicion) {
            System.out.println("  [OK]    " + nombre);
        } else {
            fallos++;
            System.out.println("  [FALLA] " + nombre + " -> " + detalle);
        }
    }

    @SuppressWarnings("unused")
    private static void mostrarMapa(Map<String, Boolean> mapa) {
        mapa.forEach((k, v) -> System.out.println("    " + k + " = " + v));
    }
}
