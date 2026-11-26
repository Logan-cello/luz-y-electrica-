# Ayuda del proyecto: gestión de redes con grafos

¡Hola! Esta guía explica cómo está organizado el programa, qué calcula cada algoritmo y cómo compilarlo y ejecutarlo. El proyecto está escrito en Java y no necesita librerías externas ni Maven o Gradle.

## 1. ¿Qué hace el programa?

Representa una red de servicio —por ejemplo, una red de agua o eléctrica— como un grafo. Los nodos son fuentes, puntos intermedios de distribución y dispositivos consumidores. Las aristas son las conexiones entre ellos.

Con el archivo de ejemplo, el programa puede:

- Mostrar un resumen de la red y sus fallas.
- Identificar qué dispositivos dejaron de recibir servicio.
- Proponer un orden voraz para reparar los componentes averiados.
- Detectar conexiones o nodos que podrían aislar dispositivos.
- Estimar cuánto caudal o potencia llega a cada dispositivo teniendo en cuenta las capacidades.

El ejemplo incluido es una red de agua (`datos/red_agua.txt`). Aunque el modelo también define unidades para redes eléctricas, eso no convierte la simulación en un modelo de ingeniería eléctrica o hidráulica: es una herramienta académica de grafos con reglas simplificadas.

## 2. Estructura del proyecto

```text
redes-grafos/
├── README.md
├── ayuda.md
├── datos/
│   └── red_agua.txt
└── src/
    └── redes/
        ├── algoritmos/
        ├── app/
        ├── io/
        └── modelo/
```

### Paquetes y responsabilidades

| Paquete / clase | Responsabilidad |
|---|---|
| `redes.modelo.Nodo` | Describe un vértice: identificador, tipo, capacidad, consumo y carga inicial. |
| `redes.modelo.Arista` | Describe una conexión no dirigida: extremos, capacidad y longitud. |
| `redes.modelo.Grafo` | Guarda los nodos y conexiones mediante listas de adyacencia. Permite conexiones paralelas, identificadas por ID. |
| `redes.modelo.Escenario` | Guarda qué nodos y conexiones están averiados. Es inmutable para poder simular escenarios sin alterar la topología. |
| `redes.modelo.Red` | Reúne el grafo, las unidades, los parámetros físicos y el escenario actual. |
| `redes.modelo.TipoNodo` y `TipoRed` | Enumeraciones para clasificar nodos y servicios. |
| `redes.io.CargadorRed` | Lee el archivo de texto y construye la red. |
| `redes.algoritmos.Servicio` | Calcula qué dispositivos pueden recibir servicio y con qué presión/tensión residual. |
| `redes.algoritmos.PlanReparacion` | Prioriza las reparaciones según los dispositivos que cada una restablece. |
| `redes.algoritmos.Vulnerabilidad` | Busca puentes y puntos de articulación y mide su impacto real en los dispositivos. |
| `redes.algoritmos.FlujoCapacidad` | Calcula cuánto flujo puede entregarse respetando capacidades y demandas. |
| `redes.app.Main` | Punto de entrada; procesa argumentos, muestra el mapa inicial y ofrece el menú de consola. |
| `redes.app.MapaRed` | Muestra cada nodo y sus conexiones, indicando aristas caídas y nodos averiados. |
| `redes.app.Reportes` | Presenta los resultados en consola, separando la salida de los cálculos. |

La estructura sigue una separación sencilla: `modelo` contiene los datos, `io` los carga, `algoritmos` calcula y `app` permite usar y mostrar el sistema.

## 3. ¿Qué hace cada algoritmo?

### Servicio: búsqueda de máxima presión/tensión

`Servicio.calcular` parte de todas las fuentes operativas y explora el grafo usando una cola de prioridad tipo Dijkstra, pero el objetivo es **maximizar la carga residual** en vez de minimizar una distancia.

Al avanzar por una arista:

1. Se comprueba que la arista y el siguiente nodo estén operativos.
2. Se descartan aristas cuya longitud supere `distanciaMaxima`.
3. Se resta la pérdida `decaimientoPorUnidad × longitud`.
4. Se descarta el camino si la carga queda por debajo de `presionMinima`.

Los dispositivos son terminales: reciben servicio, pero no reenvían el recurso. Este cálculo decide conectividad y presión; **no distribuye caudal según las capacidades**.

### Plan de reparación: heurística voraz

Para cada falla pendiente, el algoritmo simula qué dispositivos nuevos recuperarían servicio si se reparara solamente ese componente. Elige el que más dispositivos restablece; en caso de empate, prioriza el menor costo y luego el ID.

- El costo de una arista es su longitud.
- El costo asignado a reparar un nodo es `50`.
- Después de cada reparación simulada, vuelve a calcular las prioridades porque las ganancias pueden cambiar.

Es una heurística: ofrece una prioridad razonable, pero **no garantiza el plan globalmente óptimo**. Los costos son valores de comparación del programa, no precios monetarios.

### Vulnerabilidad: Tarjan

Busca:

- **Puentes:** aristas cuya eliminación separa partes del grafo.
- **Puntos de articulación:** nodos intermedios cuya eliminación separa partes del grafo.

Luego simula la falla de cada hallazgo y calcula cuántos dispositivos que normalmente reciben servicio lo perderían. Así distingue una conexión estructuralmente importante de una que realmente deja usuarios sin servicio porque existe otra fuente o ruta.

Este reporte analiza la red **sin las fallas registradas**, como análisis de diseño, e ignora aristas más largas que la distancia máxima. Los hallazgos se ordenan por cantidad de dispositivos aislados.

### Capacidad: flujo máximo Edmonds–Karp

Construye una red de flujo y busca caminos aumentantes con BFS. Respeta las capacidades de las conexiones, los límites de los nodos intermedios y la demanda de cada dispositivo. El resultado estima lo entregado y el déficit por dispositivo.

Este cálculo complementa el de servicio: un dispositivo puede tener una ruta y presión suficientes, pero aun así recibir menos de su demanda por un cuello de botella de capacidad. El reporte usa el escenario de fallas actual y considera los dispositivos que `Servicio` encuentra conectados.

## 4. Compilar y ejecutar

Se recomienda tener instalado un **JDK 21** (incluye `javac` y `java`). Ejecuta estos comandos desde la carpeta `redes-grafos`, donde están `README.md`, `src` y `datos`.

Comprueba la instalación:

```text
java --version
javac --version
```

### Windows: PowerShell

```powershell
New-Item -ItemType Directory -Force .\out | Out-Null
$archivosJava = Get-ChildItem .\src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d .\out $archivosJava
```

Después de compilar, puedes elegir cualquiera de estas formas:

```powershell
# 1. Demo automática: imprime todos los reportes y termina.
java -cp .\out redes.app.Main --demo

# 2. Menú interactivo usando datos/red_agua.txt por defecto.
java -cp .\out redes.app.Main

# 3. Menú interactivo con un archivo de datos indicado explícitamente.
java -cp .\out redes.app.Main .\datos\red_agua.txt

# 4. Demo automática con un archivo de datos indicado explícitamente.
java -cp .\out redes.app.Main --demo .\datos\red_agua.txt
```

En Windows también puedes usar `java -cp out redes.app.Main ...`; la barra invertida de `.\out` es solo la forma habitual de escribir una ruta relativa en PowerShell.

### macOS o Linux: Bash

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name '*.java')

# Demo automática
java -cp out redes.app.Main --demo

# Menú interactivo
java -cp out redes.app.Main

# Usar otro archivo de datos
java -cp out redes.app.Main datos/red_agua.txt
```

La ruta del archivo se resuelve desde la carpeta actual de la terminal. Por eso conviene ejecutar los comandos desde la raíz del proyecto; si estás en otra carpeta, indica la ruta correcta al archivo.

### Desde un IDE

También puedes abrir la carpeta del proyecto en IntelliJ IDEA, Eclipse o VS Code con soporte para Java:

1. Configura un JDK 21.
2. Usa `src` como carpeta de fuentes y ejecuta `redes.app.Main`.
3. Deja los argumentos vacíos para abrir el menú, o escribe `--demo` para generar los reportes automáticamente.
4. Configura como directorio de trabajo la raíz del proyecto (`redes-grafos`), porque la ruta predeterminada de datos es `datos/red_agua.txt`.

## 5. Usar el menú

Al iniciar, tanto en modo demo como interactivo, se muestra primero el mapa topológico: cada nodo aparece con sus conexiones y se marcan las aristas caídas y los nodos averiados. No representa ubicaciones geográficas; cada conexión se lista desde ambos extremos. En el modo interactivo aparece después el menú:

| Opción | Acción |
|---:|---|
| 1 | Mostrar resumen, parámetros y fallas actuales. |
| 2 | Agregar un nodo. Tipos admitidos: `FUENTE`, `DISTRIBUCION`, `SUBESTACION`, `DISPOSITIVO`. |
| 3 | Agregar una arista entre dos nodos existentes. |
| 4 | Registrar la falla de un nodo. |
| 5 | Registrar la falla de una arista. |
| 6 | Reparar un nodo o una arista usando su ID. |
| 7 | Mostrar los dispositivos afectados comparando el escenario actual con la red sin fallas. |
| 8 | Mostrar el plan priorizado de reparación. |
| 9 | Mostrar el análisis de vulnerabilidad del diseño. |
| 10 | Simular la capacidad con las fallas actuales. |
| 11 | Volver a mostrar el mapa de la red. |
| 0 | Salir. |

Los nodos y aristas que se agregan en el menú solo existen durante esa ejecución. El programa no guarda esos cambios en `datos/red_agua.txt`; para conservarlos hay que editar el archivo de datos.

## 6. Formato del archivo de datos

El archivo usa una instrucción por línea. Las líneas vacías se ignoran y `#` inicia un comentario. El orden básico es: declarar la red, sus parámetros, todos los nodos, sus aristas y, opcionalmente, las fallas iniciales.

```text
RED nombre AGUA
PARAMETROS distanciaMaxima presionMinima decaimientoPorUnidad
NODO id TIPO capacidad consumo cargaInicial nombre opcional
ARISTA id origen destino capacidad longitud
FALLA_NODO id
FALLA_ARISTA id
```

Ejemplo mínimo:

```text
RED MiRed AGUA
PARAMETROS 250 20 0.2
NODO F1 FUENTE 100 0 100 Fuente principal
NODO C1 DISTRIBUCION 80 0 0 Caja central
NODO D1 DISPOSITIVO 0 10 0 Escuela
ARISTA a1 F1 C1 50 100
ARISTA a2 C1 D1 20 30
FALLA_ARISTA a2
```

Notas:

- `AGUA` muestra flujo en `L/s` y carga en `m.c.a.`; `ELECTRICA` muestra `kW` y `V`.
- Los tipos de nodo deben escribirse como `FUENTE`, `DISTRIBUCION`, `SUBESTACION` o `DISPOSITIVO`.
- Declara los nodos antes de las aristas que los referencian.
- Capacidad `0` significa “sin límite” para los cálculos de flujo.
- `consumo` representa la demanda de un dispositivo; para otros tipos de nodo normalmente es `0`.
- `cargaInicial` se usa para la presión/tensión inicial de una fuente.
- La distancia máxima se aplica a cada arista por separado; la pérdida de carga se acumula a lo largo de la ruta.
- Si se omite el nombre del nodo, se usa su ID. Los nombres pueden contener espacios.
- Los IDs de nodos y aristas deben ser únicos dentro de su grupo.

## 7. Preguntas frecuentes

### ¿Por qué un dispositivo aparece afectado?

Puede estar averiado, no tener una ruta operativa desde ninguna fuente, estar detrás de una arista demasiado larga o recibir una presión/tensión calculada por debajo del mínimo configurado.

### ¿Por qué la simulación de capacidad muestra déficit aunque haya servicio?

Porque “tener servicio” significa que existe una ruta válida con carga suficiente. El cálculo de capacidad agrega límites de flujo de aristas y nodos, además de la demanda del dispositivo; un cuello de botella puede reducir lo entregado.

### ¿Una falla cambia permanentemente el grafo?

No. El grafo conserva su topología y las fallas se registran en un `Escenario` aparte. El menú puede registrar o reparar fallas durante la sesión, pero no guarda esos cambios al salir.

### ¿El plan de reparación siempre encuentra la mejor solución?

No necesariamente. Prioriza el mayor beneficio inmediato y recalcula después de cada paso. Es útil para ordenar reparaciones, pero no asegura el óptimo global.

### ¿Puedo crear mi propia red?

Sí. Copia `datos/red_agua.txt`, modifica los nodos, conexiones, parámetros y fallas, y pasa la ruta nueva al programa:

```text
java -cp out redes.app.Main datos/mi_red.txt
```

### ¿Necesito instalar Maven o Gradle?

No. Basta con un JDK y los comandos `javac` y `java`. La carpeta `out` contiene las clases generadas al compilar; si la borras, vuelve a compilar antes de ejecutar.

### ¿Qué pasa si el programa indica un error de línea?

Revisa el número de línea informado en el archivo: puede haber una instrucción desconocida, un valor numérico inválido, un tipo mal escrito o una arista que referencia un nodo aún no declarado.
