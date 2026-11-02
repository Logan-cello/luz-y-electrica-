package redes.io;

import redes.modelo.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Carga estructurada de una red desde un archivo de texto. Formato (una instrucción por línea,
 * '#' inicia un comentario):
 *
 *   RED          nombre TIPO(AGUA|ELECTRICA)
 *   PARAMETROS   distanciaMaxima presionMinima decaimientoPorUnidad
 *   NODO         id TIPO capacidad consumo cargaInicial [nombre...]
 *   ARISTA       id origen destino capacidad longitud
 *   FALLA_NODO   id
 *   FALLA_ARISTA id
 *
 * TIPO de nodo: FUENTE | DISTRIBUCION | SUBESTACION | DISPOSITIVO. Todos los NODO deben
 * declararse antes que las ARISTA que los usan.
 */
public final class CargadorRed {
    private CargadorRed() { }

    public static Red cargar(Path archivo) throws IOException {
        return interpretar(Files.readAllLines(archivo));
    }

    public static Red interpretar(List<String> lineas) {
        Red red = null;
        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            int c = linea.indexOf('#');
            if (c >= 0) linea = linea.substring(0, c);
            linea = linea.trim();
            if (linea.isEmpty()) continue;
            String[] t = linea.split("\\s+");
            try {
                switch (t[0].toUpperCase()) {
                    case "RED" -> red = new Red(t[1], TipoRed.valueOf(t[2].toUpperCase()));
                    case "PARAMETROS" -> exigir(red).configurar(
                            num(t[1]), num(t[2]), num(t[3]));
                    case "NODO" -> exigir(red).grafo().agregarNodo(new Nodo(
                            t[1], TipoNodo.valueOf(t[2].toUpperCase()),
                            t.length > 6 ? String.join(" ", Arrays.copyOfRange(t, 6, t.length)) : t[1],
                            num(t[3]), num(t[4]), num(t[5])));
                    case "ARISTA" -> exigir(red).grafo().agregarArista(new Arista(
                            t[1], t[2], t[3], num(t[4]), num(t[5])));
                    case "FALLA_NODO" -> exigir(red).registrarFallaNodo(t[1]);
                    case "FALLA_ARISTA" -> exigir(red).registrarFallaArista(t[1]);
                    default -> throw new IllegalArgumentException("Instrucción desconocida: " + t[0]);
                }
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Línea " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        if (red == null) throw new IllegalArgumentException("El archivo no declara la instrucción RED");
        return red;
    }

    private static Red exigir(Red red) {
        if (red == null) throw new IllegalStateException("Primero debe declararse RED");
        return red;
    }

    private static double num(String s) { return Double.parseDouble(s); }
}
