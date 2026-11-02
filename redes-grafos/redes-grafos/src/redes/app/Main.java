package redes.app;

import redes.io.CargadorRed;
import redes.modelo.*;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * Punto de entrada.
 *   java -cp out redes.app.Main --demo [archivo]   -> ejecuta las salidas sobre datos de prueba
 *   java -cp out redes.app.Main [archivo]          -> menú interactivo
 */
public final class Main {
    private static final String ARCHIVO_DEFECTO = "datos/red_agua.txt";

    public static void main(String[] args) throws Exception {
        // Salida en UTF-8 para que tildes y símbolos se vean igual en cualquier consola.
        System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
        boolean demo = args.length > 0 && args[0].equals("--demo");
        String ruta = ARCHIVO_DEFECTO;
        for (String a : args) if (!a.equals("--demo")) ruta = a;

        Red red = CargadorRed.cargar(Path.of(ruta));
        if (demo) {
            Reportes.resumen(red);
            Reportes.afectados(red);
            Reportes.plan(red);
            Reportes.vulnerabilidad(red);
            Reportes.capacidad(red);
        } else {
            menu(red);
        }
    }

    private static void menu(Red red) {
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("""

                    ===== GESTIÓN DE REDES DE SERVICIOS =====
                     1) Resumen de la red          6) Reparar componente
                     2) Agregar nodo               7) Dispositivos afectados
                     3) Agregar arista             8) Plan de reparación
                     4) Registrar falla de nodo    9) Análisis de vulnerabilidad
                     5) Registrar falla de arista 10) Simulación de capacidad
                     0) Salir""");
            System.out.print("Opción: ");
            if (!in.hasNextLine()) return;
            String op = in.nextLine().trim();
            try {
                switch (op) {
                    case "1" -> Reportes.resumen(red);
                    case "2" -> {
                        String id = leer(in, "id");
                        TipoNodo t = TipoNodo.valueOf(leer(in, "tipo (FUENTE|DISTRIBUCION|SUBESTACION|DISPOSITIVO)").toUpperCase());
                        red.grafo().agregarNodo(new Nodo(id, t, leer(in, "nombre"),
                                num(in, "capacidad (0 = sin límite)"), num(in, "consumo"),
                                num(in, "carga inicial (solo fuentes)")));
                    }
                    case "3" -> red.grafo().agregarArista(new Arista(leer(in, "id"), leer(in, "origen"),
                            leer(in, "destino"), num(in, "capacidad (0 = sin límite)"), num(in, "longitud")));
                    case "4" -> red.registrarFallaNodo(leer(in, "id del nodo"));
                    case "5" -> red.registrarFallaArista(leer(in, "id de la arista"));
                    case "6" -> {
                        String id = leer(in, "id (nodo o arista)");
                        if (red.grafo().existeArista(id)) red.repararArista(id); else red.repararNodo(id);
                    }
                    case "7" -> Reportes.afectados(red);
                    case "8" -> Reportes.plan(red);
                    case "9" -> Reportes.vulnerabilidad(red);
                    case "10" -> Reportes.capacidad(red);
                    case "0" -> { return; }
                    default -> System.out.println("Opción no válida");
                }
            } catch (RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static String leer(Scanner in, String campo) {
        System.out.print("  " + campo + ": ");
        return in.nextLine().trim();
    }

    private static double num(Scanner in, String campo) { return Double.parseDouble(leer(in, campo)); }
}
