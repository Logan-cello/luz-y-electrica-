package redes.modelo;

import java.util.Objects;

/**
 * Vértice del grafo. Es inmutable: el estado de falla NO vive aquí sino en {@link Escenario},
 * lo que permite simular "qué pasaría si..." sin mutar la red.
 *
 * @param capacidad   flujo máximo que puede atravesar el nodo (o producir, si es fuente). Un valor
 *                    menor o igual a 0 significa "sin límite".
 * @param consumo     demanda exacta del dispositivo final (0 para el resto de nodos).
 * @param cargaInicial presión/tensión de salida (solo relevante para fuentes).
 */
public record Nodo(String id, TipoNodo tipo, String nombre,
                   double capacidad, double consumo, double cargaInicial) {

    public Nodo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tipo, "tipo");
        if (id.isBlank()) throw new IllegalArgumentException("El id del nodo no puede ser vacío");
        if (consumo < 0) throw new IllegalArgumentException("Consumo negativo en " + id);
        nombre = (nombre == null || nombre.isBlank()) ? id : nombre;
    }

    public boolean esFuente() { return tipo == TipoNodo.FUENTE; }

    public boolean esDispositivo() { return tipo == TipoNodo.DISPOSITIVO; }

    @Override
    public String toString() { return id + " (" + tipo + ")"; }
}
