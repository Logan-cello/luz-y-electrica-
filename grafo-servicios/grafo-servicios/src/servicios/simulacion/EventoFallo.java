package servicios.simulacion;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Registro de un fallo o reparación aplicado durante la simulación. */
public class EventoFallo {

    public enum Tipo { FALLO_NODO, FALLO_ARISTA, REPARACION, ESCENARIO }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final LocalDateTime hora;
    private final Tipo tipo;
    private final String idElemento;
    private final String descripcion;

    public EventoFallo(Tipo tipo, String idElemento, String descripcion) {
        this.hora = LocalDateTime.now();
        this.tipo = tipo;
        this.idElemento = idElemento;
        this.descripcion = descripcion;
    }

    public LocalDateTime getHora() {
        return hora;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getIdElemento() {
        return idElemento;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return String.format("[%s] %-13s %-9s %s", hora.format(FMT), tipo, idElemento, descripcion);
    }
}
