package servicios.analisis;

import servicios.grafo.Nodo;
import servicios.grafo.Servicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resultado completo del análisis de una red en un instante dado:
 * qué nodos tienen servicio, cuánta demanda se pierde y por qué.
 */
public class InformeRed {

    private final Servicio servicio;
    private final int nodosTotales;
    private final int nodosActivos;
    private final int aristasTotales;
    private final int aristasFalladas;

    private final int consumidoresTotales;
    private final int consumidoresConServicio;

    private final double demandaTotal;
    private final double demandaAtendida;
    private final double demandaPerdida;

    private final List<Nodo> consumidoresSinServicio;
    private final List<Nodo> nodosSinServicio;
    private final Set<String> nodosCaidosPorDependencia;
    private final List<Set<Nodo>> componentes;
    private final Map<String, String> causasPerdida;

    public InformeRed(Servicio servicio, int nodosTotales, int nodosActivos,
                      int aristasTotales, int aristasFalladas,
                      int consumidoresTotales, int consumidoresConServicio,
                      double demandaTotal, double demandaAtendida,
                      List<Nodo> consumidoresSinServicio, List<Nodo> nodosSinServicio,
                      Set<String> nodosCaidosPorDependencia, List<Set<Nodo>> componentes,
                      Map<String, String> causasPerdida) {
        this.servicio = servicio;
        this.nodosTotales = nodosTotales;
        this.nodosActivos = nodosActivos;
        this.aristasTotales = aristasTotales;
        this.aristasFalladas = aristasFalladas;
        this.consumidoresTotales = consumidoresTotales;
        this.consumidoresConServicio = consumidoresConServicio;
        this.demandaTotal = demandaTotal;
        this.demandaAtendida = demandaAtendida;
        this.demandaPerdida = Math.max(0, demandaTotal - demandaAtendida);
        this.consumidoresSinServicio = consumidoresSinServicio;
        this.nodosSinServicio = nodosSinServicio;
        this.nodosCaidosPorDependencia = nodosCaidosPorDependencia;
        this.componentes = componentes;
        this.causasPerdida = causasPerdida;
    }

    /** Motivo de la pérdida para cada consumidor afectado. */
    public Map<String, String> getCausasPerdida() {
        return causasPerdida;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public int getNodosTotales() {
        return nodosTotales;
    }

    public int getNodosActivos() {
        return nodosActivos;
    }

    public int getAristasTotales() {
        return aristasTotales;
    }

    public int getAristasFalladas() {
        return aristasFalladas;
    }

    public int getConsumidoresTotales() {
        return consumidoresTotales;
    }

    public int getConsumidoresConServicio() {
        return consumidoresConServicio;
    }

    public double getDemandaTotal() {
        return demandaTotal;
    }

    public double getDemandaAtendida() {
        return demandaAtendida;
    }

    public double getDemandaPerdida() {
        return demandaPerdida;
    }

    /** Porcentaje de demanda que la red todavía puede entregar. */
    public double getPorcentajeServicio() {
        if (demandaTotal <= 0) return 100.0;
        return 100.0 * demandaAtendida / demandaTotal;
    }

    public List<Nodo> getConsumidoresSinServicio() {
        return consumidoresSinServicio;
    }

    public List<Nodo> getNodosSinServicio() {
        return nodosSinServicio;
    }

    public Set<String> getNodosCaidosPorDependencia() {
        return nodosCaidosPorDependencia;
    }

    public List<Set<Nodo>> getComponentes() {
        return componentes;
    }

    public boolean esRedSana() {
        return demandaPerdida <= 1e-9 && consumidoresSinServicio.isEmpty();
    }

    /** Línea compacta para consola. */
    public String resumenCorto() {
        return String.format("%-13s servicio=%5.1f%%  demanda atendida=%6.0f/%.0f %s  (consumidores sin servicio: %d/%d)",
                servicio.name(), getPorcentajeServicio(), demandaAtendida, demandaTotal,
                servicio.getUnidad(), consumidoresSinServicio.size(), consumidoresTotales);
    }

    /** Detalle de cada consumidor afectado. */
    public List<String> detalleAfectados() {
        List<String> lineas = new ArrayList<>();
        for (Nodo n : consumidoresSinServicio) {
            String causa = causasPerdida.getOrDefault(n.getId(), "sin camino activo desde la fuente");
            lineas.add(String.format("  - %-10s %-24s demanda %6.0f %-6s -> %s",
                    n.getId(), n.getNombre(), n.getDemanda(), servicio.getUnidad(), causa));
        }
        return lineas;
    }

    @Override
    public String toString() {
        return resumenCorto();
    }
}
