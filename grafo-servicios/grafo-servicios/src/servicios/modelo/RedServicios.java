package servicios.modelo;

import servicios.grafo.Arista;
import servicios.grafo.Categoria;
import servicios.grafo.Grafo;
import servicios.grafo.Nodo;
import servicios.grafo.Servicio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Red de servicios de un sector urbano: dos grafos (luz y agua) unidos por
 * dependencias entre servicios.
 *
 * <p>Modelo de interdependencia:</p>
 * <ul>
 *   <li>Las <b>bombas de agua</b> necesitan electricidad para funcionar.</li>
 *   <li>El <b>hospital</b> y la <b>industria</b> necesitan luz para recibir agua a presión.</li>
 * </ul>
 * <p>Por eso un fallo eléctrico puede propagarse a la red de agua: es la parte
 * de "fallo en cascada" del simulador.</p>
 */
public class RedServicios {

    private final Grafo luz;
    private final Grafo agua;

    /** Índice global id -> nodo, para resolver dependencias entre grafos. */
    private final Map<String, Nodo> indice = new LinkedHashMap<>();

    public RedServicios() {
        this.luz = Grafo.dirigido("Red eléctrica", Servicio.LUZ);
        this.agua = Grafo.dirigido("Red de agua", Servicio.AGUA);
        construirRedElectrica();
        construirRedAgua();
        construirDependencias();
    }

    // ==================================================================
    // Construcción de la red eléctrica
    // ==================================================================
    private void construirRedElectrica() {
        Nodo planta = nuevo("PLT-1", "Central hidroeléctrica", Categoria.PLANTA, luz, 0, 170, 100);
        Nodo sub1 = nuevo("SUB-1", "Subestación Norte", Categoria.SUBESTACION, luz, 0, 110, 200);
        Nodo sub2 = nuevo("SUB-2", "Subestación Sur", Categoria.SUBESTACION, luz, 0, 330, 200);

        Nodo trf1 = nuevo("TRF-1", "Transformador 1", Categoria.TRANSFORMADOR, luz, 0, 80, 300);
        Nodo trf2 = nuevo("TRF-2", "Transformador 2", Categoria.TRANSFORMADOR, luz, 0, 190, 300);
        Nodo trf3 = nuevo("TRF-3", "Transformador 3", Categoria.TRANSFORMADOR, luz, 0, 300, 300);
        Nodo trf4 = nuevo("TRF-4", "Transformador 4", Categoria.TRANSFORMADOR, luz, 0, 410, 300);

        Nodo pos1 = nuevo("POS-1", "Poste Norte", Categoria.POSTE, luz, 0, 70, 400);
        Nodo pos2 = nuevo("POS-2", "Poste Centro", Categoria.POSTE, luz, 0, 180, 400);
        Nodo pos3 = nuevo("POS-3", "Poste Sur", Categoria.POSTE, luz, 0, 290, 400);
        Nodo pos4 = nuevo("POS-4", "Poste Este", Categoria.POSTE, luz, 0, 400, 400);

        nuevo("HOSP-E", "Hospital (luz)", Categoria.HOSPITAL, luz, 180, 50, 520);
        nuevo("IND-E", "Industria (luz)", Categoria.INDUSTRIA, luz, 400, 150, 520);
        nuevo("VIV-1-E", "Barrio Norte (luz)", Categoria.VIVIENDA, luz, 260, 250, 520);
        nuevo("VIV-2-E", "Barrio Sur (luz)", Categoria.VIVIENDA, luz, 220, 340, 520);
        nuevo("VIV-3-E", "Barrio Este (luz)", Categoria.VIVIENDA, luz, 200, 430, 520);

        // Generación -> subestaciones
        luz.conectar("E01", "PLT-1", "SUB-1", 6000, 2.0);
        luz.conectar("E02", "PLT-1", "SUB-2", 6000, 2.4);
        // Subestaciones -> transformadores (con redundancia entre subestaciones)
        luz.conectar("E03", "SUB-1", "TRF-1", 2500, 1.0);
        luz.conectar("E04", "SUB-1", "TRF-2", 2500, 1.3);
        luz.conectar("E05", "SUB-2", "TRF-2", 2500, 1.2);
        luz.conectar("E06", "SUB-2", "TRF-3", 2500, 1.0);
        luz.conectar("E07", "SUB-2", "TRF-4", 2500, 1.1);
        // Transformadores -> postes
        luz.conectar("E08", "TRF-1", "POS-1", 900, 0.8);
        luz.conectar("E09", "TRF-2", "POS-2", 900, 0.8);
        luz.conectar("E10", "TRF-3", "POS-3", 900, 0.8);
        luz.conectar("E11", "TRF-4", "POS-4", 900, 0.8);
        // Red de respaldo entre postes (permite rutas alternativas)
        luz.conectar("E12", "POS-1", "POS-2", 600, 1.5);
        luz.conectar("E13", "POS-2", "POS-3", 600, 1.5);
        luz.conectar("E14", "POS-3", "POS-4", 600, 1.5);
        // Postes -> consumidores
        luz.conectar("E15", "POS-1", "HOSP-E", 400, 0.9);
        luz.conectar("E16", "POS-2", "IND-E", 900, 0.9);
        luz.conectar("E17", "POS-2", "VIV-1-E", 500, 0.9);
        luz.conectar("E18", "POS-3", "VIV-2-E", 500, 0.9);
        luz.conectar("E19", "POS-4", "VIV-3-E", 500, 0.9);
    }

    // ==================================================================
    // Construcción de la red de agua
    // ==================================================================
    private void construirRedAgua() {
        Nodo res = nuevo("RES-1", "Reservorio principal", Categoria.RESERVORIO, agua, 0, 760, 100);
        Nodo bmb1 = nuevo("BMB-1", "Bombeo Norte", Categoria.BOMBA, agua, 0, 600, 200);
        Nodo bmb2 = nuevo("BMB-2", "Bombeo Sur", Categoria.BOMBA, agua, 0, 860, 200);

        nuevo("TANQ-1", "Tanque Norte", Categoria.TANQUE, agua, 0, 550, 300);
        nuevo("TANQ-2", "Tanque Sur", Categoria.TANQUE, agua, 0, 800, 300);

        nuevo("VALV-1", "Válvula 1", Categoria.VALVULA, agua, 0, 540, 400);
        nuevo("VALV-2", "Válvula 2", Categoria.VALVULA, agua, 0, 660, 400);
        nuevo("VALV-3", "Válvula 3", Categoria.VALVULA, agua, 0, 820, 400);
        nuevo("EMP-1", "Empalme central", Categoria.EMPALME, agua, 0, 600, 455);

        nuevo("HOSP-A", "Hospital (agua)", Categoria.HOSPITAL, agua, 120, 520, 520);
        nuevo("IND-A", "Industria (agua)", Categoria.INDUSTRIA, agua, 400, 620, 520);
        nuevo("VIV-1-A", "Barrio Norte (agua)", Categoria.VIVIENDA, agua, 90, 720, 520);
        nuevo("VIV-2-A", "Barrio Sur (agua)", Categoria.VIVIENDA, agua, 80, 820, 520);
        nuevo("VIV-3-A", "Barrio Este (agua)", Categoria.VIVIENDA, agua, 70, 920, 520);

        agua.conectar("A01", "RES-1", "BMB-1", 3000, 2.2);
        agua.conectar("A02", "RES-1", "BMB-2", 3000, 2.8);
        agua.conectar("A03", "BMB-1", "TANQ-1", 1600, 1.2);
        agua.conectar("A04", "BMB-2", "TANQ-2", 1600, 1.4);
        agua.conectar("A05", "TANQ-1", "VALV-1", 800, 1.0);
        agua.conectar("A06", "TANQ-1", "VALV-2", 800, 1.1);
        agua.conectar("A07", "TANQ-2", "VALV-3", 800, 1.0);
        agua.conectar("A08", "VALV-1", "EMP-1", 700, 0.9);
        agua.conectar("A09", "VALV-2", "EMP-1", 700, 0.9);
        agua.conectar("A10", "EMP-1", "HOSP-A", 400, 1.0);
        agua.conectar("A11", "EMP-1", "IND-A", 700, 1.0);
        agua.conectar("A12", "VALV-2", "VIV-1-A", 400, 1.0);
        agua.conectar("A13", "VALV-3", "VIV-2-A", 400, 1.0);
        agua.conectar("A14", "VALV-3", "VIV-3-A", 400, 1.0);
        // Red de respaldo: el tanque sur puede reforzar la válvula 2,
        // pero la zona este depende únicamente de TANQ-2 (punto único de fallo).
        agua.conectar("A15", "TANQ-2", "VALV-2", 600, 1.6);
    }

    // ==================================================================
    // Dependencias entre servicios (clave del fallo en cascada)
    // ==================================================================
    private void construirDependencias() {
        // Las bombas no trabajan sin energía eléctrica
        indice.get("BMB-1").agregarDependencia("SUB-2", "necesita energía de la subestación SUB-2");
        indice.get("BMB-2").agregarDependencia("SUB-1", "necesita energía de la subestación SUB-1");
        // Los grandes consumidores necesitan luz para bombear agua a presión
        indice.get("HOSP-A").agregarDependencia("HOSP-E", "planta de presión eléctrica");
        indice.get("IND-A").agregarDependencia("IND-E", "planta de presión eléctrica");
    }

    /** Crea el nodo, lo agrega al grafo y lo registra en el índice global. */
    private Nodo nuevo(String id, String nombre, Categoria cat, Grafo grafo,
                       double demanda, double x, double y) {
        Nodo n = new Nodo(id, nombre, cat, grafo.getServicio(), demanda, x, y);
        grafo.agregar(n);
        indice.put(id, n);
        return n;
    }

    // ==================================================================
    // Accesos
    // ==================================================================

    public Grafo getLuz() {
        return luz;
    }

    public Grafo getAgua() {
        return agua;
    }

    public Grafo grafo(Servicio s) {
        return s == Servicio.LUZ ? luz : agua;
    }

    public List<Grafo> grafos() {
        List<Grafo> g = new ArrayList<>();
        g.add(luz);
        g.add(agua);
        return g;
    }

    public Nodo nodo(String id) {
        return indice.get(id);
    }

    public Map<String, Nodo> getIndice() {
        return java.util.Collections.unmodifiableMap(indice);
    }

    /** Busca una arista por id en cualquiera de los dos grafos. */
    public Arista arista(String id) {
        Arista a = luz.arista(id);
        return a != null ? a : agua.arista(id);
    }

    /** Repara todos los nodos y aristas de ambas redes. */
    public void repararTodo() {
        for (Grafo g : grafos()) {
            for (Nodo n : g.getNodos()) n.reparar();
            for (Arista a : g.getAristas()) a.reparar();
        }
    }

    /** Fuentes activas de un grafo (plantas, reservorios). */
    public List<Nodo> fuentesActivas(Grafo g) {
        List<Nodo> fuentes = new ArrayList<>();
        for (Nodo n : g.getNodos()) {
            if (n.esFuente() && !n.estaFallado()) fuentes.add(n);
        }
        return fuentes;
    }
}
