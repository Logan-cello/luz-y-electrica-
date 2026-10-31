package servicios.simulacion;

import servicios.analisis.AnalizadorRed;
import servicios.analisis.InformeRed;
import servicios.grafo.Arista;
import servicios.grafo.Categoria;
import servicios.grafo.Grafo;
import servicios.grafo.Nodo;
import servicios.grafo.Servicio;
import servicios.modelo.RedServicios;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Inyector de fallos sobre la red. Sabe averiar nodos y aristas, ejecutar
 * escenarios predefinidos y medir el impacto resultante.
 *
 * <p>Todo pasa por aquí para que la consola y las pruebas compartan el mismo
 * comportamiento y quede un historial de eventos.</p>
 */
public class SimuladorFallas {

    private final RedServicios red;
    private final AnalizadorRed analizador;
    private final Random random;
    private final List<EventoFallo> historial = new ArrayList<>();

    public SimuladorFallas(RedServicios red) {
        this(red, new Random());
    }

    public SimuladorFallas(RedServicios red, Random random) {
        this.red = red;
        this.analizador = new AnalizadorRed(red);
        this.random = random;
    }

    public AnalizadorRed getAnalizador() {
        return analizador;
    }

    public List<EventoFallo> getHistorial() {
        return Collections.unmodifiableList(historial);
    }

    // ==================================================================
    // Fallos unitarios
    // ==================================================================

    public boolean fallarNodo(String id, String motivo) {
        Nodo n = red.nodo(id);
        if (n == null) return false;
        n.fallar(motivo);
        historial.add(new EventoFallo(EventoFallo.Tipo.FALLO_NODO, id,
                "Fallo de nodo: " + motivo));
        return true;
    }

    public boolean fallarArista(String id, String motivo) {
        Arista a = red.arista(id);
        if (a == null) return false;
        a.fallar(motivo);
        historial.add(new EventoFallo(EventoFallo.Tipo.FALLO_ARISTA, id,
                "Fallo de conexión (" + a.getOrigen().getId() + " -> " + a.getDestino().getId() + "): " + motivo));
        return true;
    }

    /** Avería todos los nodos de una categoría (ej. todos los transformadores). */
    public int fallarCategoria(Categoria categoria, String motivo) {
        int contador = 0;
        for (Grafo g : red.grafos()) {
            for (Nodo n : g.getNodos()) {
                if (n.getCategoria() == categoria) {
                    n.fallar(motivo);
                    contador++;
                }
            }
        }
        if (contador > 0) {
            historial.add(new EventoFallo(EventoFallo.Tipo.ESCENARIO, categoria.name(),
                    contador + " nodos de tipo " + categoria.getEtiqueta() + " averiados: " + motivo));
        }
        return contador;
    }

    /** Avería un nodo de transporte al azar (simula un evento imprevisto). */
    public Nodo fallarNodoAleatorio(String motivo) {
        List<Nodo> candidatos = new ArrayList<>();
        for (Grafo g : red.grafos()) {
            for (Nodo n : g.getNodos()) {
                if (!n.esFuente() && !n.estaFallado()) candidatos.add(n);
            }
        }
        if (candidatos.isEmpty()) return null;
        Nodo elegido = candidatos.get(random.nextInt(candidatos.size()));
        elegido.fallar(motivo);
        historial.add(new EventoFallo(EventoFallo.Tipo.FALLO_NODO, elegido.getId(),
                "Fallo aleatorio: " + motivo));
        return elegido;
    }

    public Arista fallarAristaAleatoria(String motivo) {
        List<Arista> candidatas = new ArrayList<>();
        for (Grafo g : red.grafos()) {
            for (Arista a : g.getAristas()) {
                if (!a.estaFallada()) candidatas.add(a);
            }
        }
        if (candidatas.isEmpty()) return null;
        Arista elegida = candidatas.get(random.nextInt(candidatas.size()));
        elegida.fallar(motivo);
        historial.add(new EventoFallo(EventoFallo.Tipo.FALLO_ARISTA, elegida.getId(),
                "Fallo aleatorio de conexión: " + motivo));
        return elegida;
    }

    /** Repara toda la red y deja constancia en el historial. */
    public void repararTodo() {
        red.repararTodo();
        historial.add(new EventoFallo(EventoFallo.Tipo.REPARACION, "-", "Red completamente reparada"));
    }

    public void repararNodo(String id) {
        Nodo n = red.nodo(id);
        if (n != null) {
            n.reparar();
            historial.add(new EventoFallo(EventoFallo.Tipo.REPARACION, id, "Nodo reparado"));
        }
    }

    public void repararArista(String id) {
        Arista a = red.arista(id);
        if (a != null) {
            a.reparar();
            historial.add(new EventoFallo(EventoFallo.Tipo.REPARACION, id, "Conexión reparada"));
        }
    }

    // ==================================================================
    // Escenarios predefinidos
    // ==================================================================

    /** Lista de escenarios disponibles con su descripción. */
    public Map<String, String> escenariosDisponibles() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("1", "Apagón en Subestación Norte (SUB-1)");
        m.put("2", "Transformador 3 quemado (TRF-3)");
        m.put("3", "Rotura de tubería en empalme central (EMP-1)");
        m.put("4", "Bomba norte fuera de servicio (BMB-1)");
        m.put("5", "Tormenta: caen 2 postes y 1 válvula");
        m.put("6", "Corte total: planta + reservorio averiados");
        m.put("7", "Fallo triple aleatorio");
        return m;
    }

    /**
     * Aplica un escenario predefinido. Devuelve una descripción de lo aplicado.
     * El escenario parte siempre de una red sana para que el resultado sea reproducible.
     */
    public String aplicarEscenario(String clave) {
        repararTodo();
        switch (clave) {
            case "1":
                fallarNodo("SUB-1", "cortocircuito en el lado de alta tensión");
                return "Apagón en SUB-1";
            case "2":
                fallarNodo("TRF-3", "sobrecarga y quemado del transformador");
                return "TRF-3 quemado";
            case "3":
                fallarNodo("EMP-1", "rotura de tubería, inundación del empalme");
                return "Rotura en EMP-1";
            case "4":
                fallarNodo("BMB-1", "bomba sin mantenimiento, motor bloqueado");
                return "BMB-1 fuera de servicio";
            case "5":
                fallarNodo("POS-1", "poste caído por viento");
                fallarNodo("POS-3", "poste caído por viento");
                fallarNodo("VALV-3", "válvula dañada por caída de rama");
                return "Tormenta sobre la red";
            case "6":
                fallarNodo("PLT-1", "parada total de la central");
                fallarNodo("RES-1", "reservorio contaminado, fuera de servicio");
                return "Corte total de ambos servicios";
            case "7":
                Nodo a = fallarNodoAleatorio("avería imprevista");
                Nodo b = fallarNodoAleatorio("avería imprevista");
                Arista c = fallarAristaAleatoria("cable/tubería dañado");
                return "Fallo aleatorio triple: "
                        + (a == null ? "-" : a.getId()) + ", "
                        + (b == null ? "-" : b.getId()) + ", "
                        + (c == null ? "-" : c.getId());
            default:
                return "Escenario desconocido: " + clave;
        }
    }

    // ==================================================================
    // Evaluación de impacto
    // ==================================================================

    public List<InformeRed> informes() {
        return analizador.analizarTodo();
    }

    /**
     * Prueba de robustez: aplica {@code repeticiones} fallos aleatorios (uno por
     * corrida, desde una red sana) y devuelve el promedio de demanda perdida.
     */
    public ResultadoRobustez robustez(int repeticiones) {
        double perdidaLuz = 0;
        double perdidaAgua = 0;
        int peor = 0;

        for (int i = 0; i < repeticiones; i++) {
            red.repararTodo();
            if (random.nextBoolean()) {
                fallarNodoAleatorio("prueba de robustez");
            } else {
                fallarAristaAleatoria("prueba de robustez");
            }
            List<InformeRed> inf = analizador.analizarTodo();
            double l = inf.get(0).getDemandaPerdida();
            double a = inf.get(1).getDemandaPerdida();
            perdidaLuz += l;
            perdidaAgua += a;
            if (l + a > 0) peor++;
        }
        red.repararTodo();
        return new ResultadoRobustez(repeticiones, perdidaLuz / repeticiones,
                perdidaAgua / repeticiones, peor);
    }

    /** Resultado agregado de la prueba de robustez. */
    public static final class ResultadoRobustez {
        private final int repeticiones;
        private final double perdidaLuzPromedio;
        private final double perdidaAguaPromedio;
        private final int corridasConImpacto;

        public ResultadoRobustez(int repeticiones, double perdidaLuzPromedio,
                                 double perdidaAguaPromedio, int corridasConImpacto) {
            this.repeticiones = repeticiones;
            this.perdidaLuzPromedio = perdidaLuzPromedio;
            this.perdidaAguaPromedio = perdidaAguaPromedio;
            this.corridasConImpacto = corridasConImpacto;
        }

        public int getRepeticiones() {
            return repeticiones;
        }

        public double getPerdidaLuzPromedio() {
            return perdidaLuzPromedio;
        }

        public double getPerdidaAguaPromedio() {
            return perdidaAguaPromedio;
        }

        public int getCorridasConImpacto() {
            return corridasConImpacto;
        }

        public double getTasaImpacto() {
            return repeticiones == 0 ? 0 : 100.0 * corridasConImpacto / repeticiones;
        }

        @Override
        public String toString() {
            return String.format(
                    "Robustez (%d corridas): pérdida media luz = %.0f kW, agua = %.0f L/min; "
                            + "corridas con algún consumidor afectado = %d (%.0f%%)",
                    repeticiones, perdidaLuzPromedio, perdidaAguaPromedio,
                    corridasConImpacto, getTasaImpacto());
        }
    }
}
