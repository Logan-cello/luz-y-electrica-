package redes.app;

import redes.modelo.Arista;
import redes.modelo.Nodo;
import redes.modelo.Red;

import java.util.List;
import java.util.Locale;

/** Presenta la topología de la red y el estado actual de sus componentes. */
public final class MapaRed {
    private MapaRed() { }

    public static void mostrar(Red red) {
        System.out.println("\n=== MAPA TOPOLOGICO: " + red.nombre() + " ===");
        System.out.println("No esta a escala geografica; cada conexion se muestra desde ambos extremos.");

        for (Nodo nodo : red.grafo().nodos()) {
            boolean nodoCaido = !red.escenario().nodoOperativo(nodo.id());
            System.out.printf("%n[%s] %s - %s%s%n", nodo.id(), nodo.tipo(), nodo.nombre(),
                    nodoCaido ? " [NODO CAIDO]" : "");

            List<Arista> conexiones = red.grafo().incidentes(nodo.id());
            if (conexiones.isEmpty()) {
                System.out.println("  (sin conexiones)");
                continue;
            }

            for (Arista arista : conexiones) {
                Nodo vecino = red.grafo().nodo(arista.otroExtremo(nodo.id()));
                String estadoArista = red.escenario().aristaOperativa(arista.id()) ? "activa" : "CAIDA";
                String estadoVecino = red.escenario().nodoOperativo(vecino.id()) ? "" : " [NODO CAIDO]";
                String capacidad = arista.capacidad() <= 0
                        ? "sin limite"
                        : String.format(Locale.ROOT, "%.1f %s", arista.capacidad(), red.tipo().unidadFlujo());
                System.out.printf("  |-- %s (%s, %.0f m, capacidad %s) -- [%s] %s%s%n",
                        arista.id(), estadoArista, arista.longitud(), capacidad, vecino.id(), vecino.nombre(),
                        estadoVecino);
            }
        }
    }
}
