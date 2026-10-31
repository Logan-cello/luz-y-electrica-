package servicios.grafo;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Vértice del grafo: un componente físico de la red (planta, subestación,
 * poste, reservorio, bomba, vivienda...).
 *
 * <p>Cada nodo guarda además el estado de fallo. Un nodo fallado sigue existiendo
 * en el grafo (no se borra): así el análisis puede distinguir entre
 * "no hay camino" y "el camino está cortado por un componente averiado".</p>
 */
public class Nodo {

    private final String id;
    private final String nombre;
    private final Categoria categoria;
    private final Servicio servicio;

    /** Demanda del nodo. 0 para nodos de transporte y fuentes. */
    private final double demanda;

    /** Coordenadas solo para dibujar el grafo en texto/plano. */
    private final double x;
    private final double y;

    private boolean fallado;
    private String motivoFallo = "";

    /**
     * Servicio del que este nodo depende para funcionar. Por ejemplo: una bomba
     * de agua necesita electricidad, un hospital necesita luz y agua.
     * Es {@code null} si el nodo no depende de nada externo.
     */
    private final Set<Dependencia> dependencias = new LinkedHashSet<>();

    public Nodo(String id, String nombre, Categoria categoria, Servicio servicio,
                double demanda, double x, double y) {
        this.id = Objects.requireNonNull(id, "id");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.categoria = Objects.requireNonNull(categoria, "categoria");
        this.servicio = Objects.requireNonNull(servicio, "servicio");
        if (demanda < 0) {
            throw new IllegalArgumentException("La demanda no puede ser negativa: " + id);
        }
        this.demanda = demanda;
        this.x = x;
        this.y = y;
    }

    public Nodo(String id, String nombre, Categoria categoria, Servicio servicio) {
        this(id, nombre, categoria, servicio, 0, 0, 0);
    }

    // ------------------------------------------------------------------
    // Fallos
    // ------------------------------------------------------------------

    /** Marca el nodo como averiado con el motivo indicado. */
    public void fallar(String motivo) {
        this.fallado = true;
        this.motivoFallo = motivo == null ? "" : motivo;
    }

    /** Repara el nodo y borra el motivo. */
    public void reparar() {
        this.fallado = false;
        this.motivoFallo = "";
    }

    public boolean estaFallado() {
        return fallado;
    }

    public String getMotivoFallo() {
        return motivoFallo;
    }

    // ------------------------------------------------------------------
    // Dependencias entre servicios
    // ------------------------------------------------------------------

    /**
     * Declara que este nodo necesita que otro nodo (posiblemente de otra red)
     * esté operativo. Ej.: {@code bomba1.dependeDe(hospitalX)}.
     */
    public void agregarDependencia(String idNodoRequerido, String descripcion) {
        dependencias.add(new Dependencia(idNodoRequerido, descripcion));
    }

    public Set<Dependencia> getDependencias() {
        return java.util.Collections.unmodifiableSet(dependencias);
    }

    /** Una dependencia entre servicios: "para funcionar necesito que X esté activo". */
    public static final class Dependencia {
        private final String idNodoRequerido;
        private final String descripcion;

        public Dependencia(String idNodoRequerido, String descripcion) {
            this.idNodoRequerido = idNodoRequerido;
            this.descripcion = descripcion == null ? "" : descripcion;
        }

        public String getIdNodoRequerido() {
            return idNodoRequerido;
        }

        public String getDescripcion() {
            return descripcion;
        }

        @Override
        public String toString() {
            return idNodoRequerido + (descripcion.isEmpty() ? "" : " (" + descripcion + ")");
        }
    }

    // ------------------------------------------------------------------
    // Accesores
    // ------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public double getDemanda() {
        return demanda;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public boolean esConsumidor() {
        return categoria.esConsumidor();
    }

    public boolean esFuente() {
        return categoria.esFuente();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Nodo)) return false;
        return id.equals(((Nodo) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " [" + categoria.getEtiqueta() + "]";
    }
}
