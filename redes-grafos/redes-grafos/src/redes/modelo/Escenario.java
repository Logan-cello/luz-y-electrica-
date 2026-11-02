package redes.modelo;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Conjunto INMUTABLE de componentes inhabilitados (nodos averiados y aristas caídas).
 * Separar el estado de falla de la topología permite evaluar miles de hipótesis
 * ("¿y si falla esta tubería?", "¿y si reparo esta primero?") sin tocar la red original.
 */
public record Escenario(Set<String> nodosCaidos, Set<String> aristasCaidas) {

    public static final Escenario SIN_FALLAS = new Escenario(Set.of(), Set.of());

    public Escenario {
        // Copia ordenada e inmutable: resultados deterministas y a salvo de mutaciones externas.
        nodosCaidos = Collections.unmodifiableSortedSet(new TreeSet<>(nodosCaidos));
        aristasCaidas = Collections.unmodifiableSortedSet(new TreeSet<>(aristasCaidas));
    }

    public boolean nodoOperativo(String id) { return !nodosCaidos.contains(id); }

    public boolean aristaOperativa(String id) { return !aristasCaidas.contains(id); }

    public int totalFallas() { return nodosCaidos.size() + aristasCaidas.size(); }

    public Escenario conNodoCaido(String id) {
        Set<String> s = new TreeSet<>(nodosCaidos);
        s.add(id);
        return new Escenario(s, aristasCaidas);
    }

    public Escenario conAristaCaida(String id) {
        Set<String> s = new TreeSet<>(aristasCaidas);
        s.add(id);
        return new Escenario(nodosCaidos, s);
    }

    public Escenario sinNodoCaido(String id) {
        Set<String> s = new TreeSet<>(nodosCaidos);
        s.remove(id);
        return new Escenario(s, aristasCaidas);
    }

    public Escenario sinAristaCaida(String id) {
        Set<String> s = new TreeSet<>(aristasCaidas);
        s.remove(id);
        return new Escenario(nodosCaidos, s);
    }
}
