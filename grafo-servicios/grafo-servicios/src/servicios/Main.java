package servicios;

import servicios.analisis.AnalizadorRed;
import servicios.analisis.InformeRed;
import servicios.grafo.Nodo;
import servicios.grafo.Servicio;
import servicios.modelo.RedServicios;
import servicios.reporte.Reporte;
import servicios.reporte.SvgRed;
import servicios.simulacion.SimuladorFallas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Punto de entrada del proyecto "Grafo de servicios: luz y agua con inyección de fallos".
 *
 * <p>Modos:</p>
 * <ul>
 *   <li>Sin argumentos: menú interactivo en consola.</li>
 *   <li>{@code --demo}: ejecuta una demostración guiada con todos los escenarios.</li>
 *   <li>{@code --escenario N}: aplica el escenario N y muestra el informe.</li>
 *   <li>{@code --csv salida.csv}: genera el volcado de datos.</li>
 * </ul>
 */
public class Main {

    public static void main(String[] args) throws IOException {
        RedServicios red = new RedServicios();
        SimuladorFallas simulador = new SimuladorFallas(red);
        AnalizadorRed analizador = simulador.getAnalizador();

        if (args.length == 0) {
            menu(red, simulador, analizador);
            return;
        }

        switch (args[0]) {
            case "--demo":
                demo(red, simulador, analizador);
                break;

            case "--escenario":
                if (args.length < 2) {
                    System.out.println("Uso: --escenario <1..7>");
                    return;
                }
                System.out.println("Aplicando escenario: " + simulador.aplicarEscenario(args[1]));
                System.out.println(Reporte.informeCompleto(red, analizador, simulador.getHistorial()));
                // Diagrama con los fallos visibles + informe en archivo
                Files.writeString(Path.of("red_servicios.svg"),
                        SvgRed.generar(red, analizador), StandardCharsets.UTF_8);
                Files.writeString(Path.of("informe_red.txt"),
                        Reporte.informeCompleto(red, analizador, simulador.getHistorial()),
                        StandardCharsets.UTF_8);
                System.out.println("Generados: red_servicios.svg y informe_red.txt");
                break;

            case "--svg":
                String salidaSvg = args.length > 1 ? args[1] : "red_servicios.svg";
                Files.writeString(Path.of(salidaSvg),
                        SvgRed.generar(red, analizador), StandardCharsets.UTF_8);
                System.out.println("Diagrama SVG escrito en " + Path.of(salidaSvg).toAbsolutePath());
                break;

            case "--csv":
                String destino = args.length > 1 ? args[1] : "red_servicios.csv";
                Files.writeString(Path.of(destino), Reporte.aCsv(red, analizador.analizarTodo()),
                        StandardCharsets.UTF_8);
                System.out.println("CSV escrito en " + Path.of(destino).toAbsolutePath());
                break;

            default:
                System.out.println("Argumento no reconocido: " + args[0]);
                System.out.println("Opciones: --demo | --escenario N | --csv archivo.csv | --svg archivo.svg");
        }
    }

    // ==================================================================
    // Menú interactivo
    // ==================================================================

    private static void menu(RedServicios red, SimuladorFallas simulador, AnalizadorRed analizador) {
        Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8);
        boolean salir = false;
        System.out.println("\n*** SISTEMA DE GRAFOS: LUZ Y AGUA CON INYECCIÓN DE FALLOS ***");

        while (!salir) {
            System.out.println("\n--- MENÚ ---");
            System.out.println(" 1) Ver topología de las redes");
            System.out.println(" 2) Ver informe / estado de servicio");
            System.out.println(" 3) Provocar fallo en un NODO        (ej. TRF-3, EMP-1)");
            System.out.println(" 4) Provocar fallo en una CONEXIÓN   (ej. E01, A05)");
            System.out.println(" 5) Aplicar escenario predefinido");
            System.out.println(" 6) Fallo aleatorio");
            System.out.println(" 7) Reparar toda la red");
            System.out.println(" 8) Puntos únicos de fallo (análisis N-1)");
            System.out.println(" 9) Prueba de robustez aleatoria");
            System.out.println("10) Camino más corto entre dos nodos (Dijkstra)");
            System.out.println("11) Exportar informe .txt, datos .csv y diagrama .svg");
            System.out.println("12) Exportar/actualizar solo el diagrama .svg");
            System.out.println(" 0) Salir");
            System.out.print("Opción: ");
            String op = sc.hasNextLine() ? sc.nextLine().trim() : "0";

            switch (op) {
                case "1":
                    System.out.print(Reporte.topologiaEnTexto(red, true, analizador.estadoActivacion()));
                    break;
                case "2":
                    System.out.print(Reporte.informeCompleto(red, analizador, simulador.getHistorial()));
                    break;
                case "3": {
                    System.out.print("ID del nodo a averiar: ");
                    String id = sc.nextLine().trim();
                    if (!simulador.fallarNodo(id, "fallo inyectado manualmente")) {
                        System.out.println("No existe el nodo " + id + ". Consulta la topología (opción 1).");
                    }
                    break;
                }
                case "4": {
                    System.out.print("ID de la conexión a averiar: ");
                    String id = sc.nextLine().trim();
                    if (!simulador.fallarArista(id, "fallo inyectado manualmente")) {
                        System.out.println("No existe la conexión " + id + ".");
                    }
                    break;
                }
                case "5": {
                    Map<String, String> esc = simulador.escenariosDisponibles();
                    esc.forEach((k, v) -> System.out.println("  " + k + ") " + v));
                    System.out.print("Escenario: ");
                    String k = sc.nextLine().trim();
                    System.out.println(">> " + simulador.aplicarEscenario(k));
                    System.out.print(Reporte.informeCompleto(red, analizador, simulador.getHistorial()));
                    break;
                }
                case "6": {
                    Nodo n = simulador.fallarNodoAleatorio("avería imprevista");
                    System.out.println(n == null ? "No hay nodos disponibles." : "Falló " + n.getId());
                    break;
                }
                case "7":
                    simulador.repararTodo();
                    System.out.println("Red reparada. Servicio restablecido.");
                    break;
                case "8":
                    System.out.println("Puntos únicos de fallo (un nodo cuya avería deja consumidores sin servicio):");
                    for (Servicio s : Servicio.values()) {
                        System.out.println("  " + s.getEtiqueta() + ": "
                                + String.join(", ", analizador.puntosUnicosDeFallo(s)));
                    }
                    break;
                case "9": {
                    SimuladorFallas.ResultadoRobustez r = simulador.robustez(200);
                    System.out.println("  " + r);
                    for (InformeRed i : analizador.analizarTodo()) {
                        System.out.printf("  %-13s servicio %s %.1f%%%n",
                                i.getServicio().name(),
                                Reporte.barra(i.getPorcentajeServicio(), 30),
                                i.getPorcentajeServicio());
                    }
                    break;
                }
                case "10": {
                    System.out.print("Nodo origen: ");
                    String a = sc.nextLine().trim();
                    System.out.print("Nodo destino: ");
                    String b = sc.nextLine().trim();
                    var camino = red.getLuz().caminoMasCorto(a, b);
                    if (camino.isEmpty()) camino = red.getAgua().caminoMasCorto(a, b);
                    if (camino.isEmpty()) {
                        System.out.println("No hay camino activo entre " + a + " y " + b + ".");
                    } else {
                        System.out.println("Camino: " + camino);
                    }
                    break;
                }
                case "11":
                    exportar(red, analizador, simulador);
                    break;
                case "12":
                    try {
                        Files.writeString(Path.of("red_servicios.svg"),
                                SvgRed.generar(red, analizador), StandardCharsets.UTF_8);
                        System.out.println("Generado: red_servicios.svg (ábrelo en el navegador)");
                    } catch (IOException e) {
                        System.out.println("Error al exportar SVG: " + e.getMessage());
                    }
                    break;
                case "0":
                    salir = true;
                    break;
                default:
                    System.out.println("Opción no válida.");
            }
        }
        System.out.println("Fin de la simulación.");
    }

    // ==================================================================
    // Exportación
    // ==================================================================

    private static void exportar(RedServicios red, AnalizadorRed analizador, SimuladorFallas simulador) {
        try {
            List<InformeRed> informes = analizador.analizarTodo();
            Files.writeString(Path.of("informe_red.txt"),
                    Reporte.informeCompleto(red, analizador, simulador.getHistorial()),
                    StandardCharsets.UTF_8);
            Files.writeString(Path.of("red_servicios.csv"), Reporte.aCsv(red, informes),
                    StandardCharsets.UTF_8);
            Files.writeString(Path.of("red_servicios.svg"),
                    SvgRed.generar(red, analizador), StandardCharsets.UTF_8);
            System.out.println("Generados: informe_red.txt, red_servicios.csv y red_servicios.svg");
        } catch (IOException e) {
            System.out.println("Error al exportar: " + e.getMessage());
        }
    }

    // ==================================================================
    // Demostración guiada
    // ==================================================================

    private static void demo(RedServicios red, SimuladorFallas simulador, AnalizadorRed analizador) {
        System.out.print(Reporte.topologiaEnTexto(red, true, analizador.estadoActivacion()));
        System.out.print(Reporte.informeCompleto(red, analizador, simulador.getHistorial()));

        System.out.println("\n>>> ESCENARIO 1: apagón en SUB-1");
        simulador.aplicarEscenario("1");
        System.out.print(Reporte.informeCompleto(red, analizador, simulador.getHistorial()));

        System.out.println("\n>>> ESCENARIO 5: tormenta (2 postes y 1 válvula)");
        simulador.aplicarEscenario("5");
        System.out.print(Reporte.informeCompleto(red, analizador, simulador.getHistorial()));

        System.out.println("\n>>> ESCENARIO 3: rotura en EMP-1");
        simulador.aplicarEscenario("3");
        System.out.print(Reporte.informeCompleto(red, analizador, simulador.getHistorial()));

        System.out.println("\n>>> ANÁLISIS N-1 (puntos únicos de fallo)");
        for (Servicio s : Servicio.values()) {
            System.out.println("  " + s.getEtiqueta() + ": "
                    + String.join(", ", analizador.puntosUnicosDeFallo(s)));
        }

        System.out.println("\n>>> PRUEBA DE ROBUSTEZ (200 fallos aleatorios)");
        System.out.println("  " + simulador.robustez(200));

        // Restauramos y dejamos la red sana
        simulador.repararTodo();
        System.out.println("\n>>> RED REPARADA");
        for (InformeRed i : analizador.analizarTodo()) {
            System.out.printf("  %-13s %s %.1f%%%n", i.getServicio().name(),
                    Reporte.barra(i.getPorcentajeServicio(), 30), i.getPorcentajeServicio());
        }
        try {
            Files.writeString(Path.of("red_servicios.svg"),
                    SvgRed.generar(red, analizador), StandardCharsets.UTF_8);
            Files.writeString(Path.of("informe_ejemplo.txt"),
                    Reporte.informeCompleto(red, analizador, simulador.getHistorial()), StandardCharsets.UTF_8);
            System.out.println("\n  Diagrama generado: red_servicios.svg");
        } catch (IOException e) {
            System.out.println("  No se pudo escribir el SVG: " + e.getMessage());
        }

        System.out.println("\n  Ejecuta sin argumentos para usar el menú interactivo:");
        System.out.println("      java servicios.Main");
    }
}
