package redes.modelo;

import java.util.*;

/**
 * Grafo no dirigido representado con LISTAS DE ADYACENCIA.
 * Espacio O(V + E); recorrer los vecinos de un nodo cuesta O(grado).
 * Admite aristas paralelas (dos tuberías entre los mismos nodos): por eso las aristas
 * se identifican por id y no por el par de nodos.
 */
public final class Grafo {
    private final Map<String, Nodo> nodos = new LinkedHashMap<>();
    private final Map<String, Arista> aristas = new LinkedHashMap<>();
    private final Map<String, List<Arista>> adyacencia = new HashMap<>();

    public void agregarNodo(Nodo n) {
        if (nodos.putIfAbsent(n.id(), n) != null)
            throw new IllegalArgumentException("Nodo duplicado: " + n.id());
        adyacencia.put(n.id(), new ArrayList<>());
    }

    public void agregarArista(Arista a) {
        if (!nodos.containsKey(a.origen()) || !nodos.containsKey(a.destino()))
            throw new IllegalArgumentException("Arista " + a.id() + " referencia un nodo inexistente");
        if (aristas.putIfAbsent(a.id(), a) != null)
            throw new IllegalArgumentException("Arista duplicada: " + a.id());
        adyacencia.get(a.origen()).add(a);
        if (!a.origen().equals(a.destino())) adyacencia.get(a.destino()).add(a);
    }

    public Nodo nodo(String id) {
        Nodo n = nodos.get(id);
        if (n == null) throw new NoSuchElementException("Nodo inexistente: " + id);
        return n;
    }

    public Arista arista(String id) {
        Arista a = aristas.get(id);
        if (a == null) throw new NoSuchElementException("Arista inexistente: " + id);
        return a;
    }

    public boolean existeNodo(String id) { return nodos.containsKey(id); }

    public boolean existeArista(String id) { return aristas.containsKey(id); }

    public Collection<Nodo> nodos() { return Collections.unmodifiableCollection(nodos.values()); }

    public Collection<Arista> aristas() { return Collections.unmodifiableCollection(aristas.values()); }

    /** Aristas incidentes a un nodo (vista de solo lectura). */
    public List<Arista> incidentes(String nodoId) {
        return Collections.unmodifiableList(adyacencia.getOrDefault(nodoId, List.of()));
    }
}
