# Sistema de grafos: red de luz y agua con inyección de fallos

Proyecto Java (sin dependencias externas) que modela la red eléctrica y la red de agua
de un sector urbano como **dos grafos dirigidos interdependientes**, y permite
**inyectar fallos** (nodos, conexiones, escenarios, aleatorios) para medir el impacto
real sobre el servicio: qué consumidores se quedan sin luz o sin agua y por qué.

---

## 1. Idea del modelo

| Concepto de teoría de grafos | En el proyecto |
|---|---|
| Vértice / nodo | Planta, subestación, transformador, poste, reservorio, bomba, tanque, válvula, vivienda, hospital, industria |
| Arista dirigida | Cable o tubería (con capacidad y longitud). La dirección es el sentido del flujo: fuente → consumidor |
| Peso de la arista | Longitud / coste de reposición → se usa en Dijkstra |
| Fuente | Nodo sin predecesores que produce el servicio (PLT-1, RES-1) |
| Sumidero | Consumidor final (demanda en kW o L/min) |
| Nodo/arista removido | Elemento averiado: no se borra del grafo, se marca como *fallado* |
| Componentes conexas | "Islas" de red que quedan después de los fallos |
| Dependencia entre grafos | Una bomba de agua necesita electricidad → el fallo eléctrico se propaga al agua |

### Topología (esquema)

```
RED ELÉCTRICA (dirigida, fuente PLT-1)
PLT-1
├── SUB-1
│   ├── TRF-1 → POS-1 → HOSP-E
│   └── TRF-2 → POS-2 → IND-E, VIV-1-E
└── SUB-2
    ├── TRF-2   (redundante: también alimenta desde SUB-1)
    ├── TRF-3 → POS-3 → VIV-2-E
    └── TRF-4 → POS-4 → VIV-3-E
POS-1 ↔ POS-2 ↔ POS-3 ↔ POS-4      (anillo de respaldo entre postes)

RED DE AGUA (dirigida, fuente RES-1)
RES-1
├── BMB-1 → TANQ-1 → VALV-1, VALV-2
└── BMB-2 → TANQ-2 → VALV-3, VALV-2   (refuerzo A15: TANQ-2 → VALV-2)
VALV-1, VALV-2 → EMP-1 → HOSP-A, IND-A
VALV-2 → VIV-1-A
VALV-3 → VIV-2-A, VIV-3-A

DEPENDENCIAS ENTRE SERVICIOS (causa del fallo en cascada)
BMB-1  necesita  SUB-2      (bomba sin luz = sin agua)
BMB-2  necesita  SUB-1
HOSP-A necesita  HOSP-E     (planta de presión eléctrica)
IND-A  necesita  IND-E
```

Los nodos están definidos en [`RedServicios.java`](src/servicios/modelo/RedServicios.java).

---

## 2. Estructura del proyecto

```
grafo-servicios/
├── LEEME.txt                       # guía rápida de 4 pasos
├── compilar.sh / compilar.bat      # compila a ./out (Linux-macOS / Windows)
├── ejecutar.sh / ejecutar.bat      # compila (si hace falta) y ejecuta
├── README.md
├── informe_ejemplo.txt             # salida real de la demo (referencia)
├── ejemplo_red.csv                 # volcado de datos de ejemplo
├── pruebas_ejemplo.txt             # salida de las pruebas
├── red_servicios.svg               # diagrama actual de la red
├── ejemplo_fallos.svg              # diagrama con el escenario 1 aplicado
├── docs/vista_fallos.png           # vista rápida del diagrama
└── src/servicios/
    ├── Main.java                   # menú interactivo + modo demo + CLI
    ├── PruebasGrafo.java           # 20 pruebas automáticas (sin JUnit)
    ├── grafo/                      # EL GRAFO (estructuras puras)
    │   ├── Grafo.java              # BFS, Dijkstra, componentes conexas
    │   ├── Nodo.java               # vértice + estado de fallo + dependencias
    │   ├── Arista.java             # conexión dirigida con capacidad
    │   ├── Categoria.java          # tipo de elemento (planta, bomba, vivienda...)
    │   └── Servicio.java           # LUZ / AGUA
    ├── modelo/
    │   └── RedServicios.java       # construye las dos redes y sus dependencias
    ├── analisis/
    │   ├── AnalizadorRed.java      # cascada por punto fijo, alcanzabilidad, N-1
    │   └── InformeRed.java         # métricas de un servicio en un instante
    ├── simulacion/
    │   ├── SimuladorFallas.java    # inyección de fallos + escenarios + robustez
    │   └── EventoFallo.java        # historial de eventos
    └── reporte/
        ├── Reporte.java            # informe, topología en texto, CSV
        └── SvgRed.java             # diagrama del grafo en SVG (averías en rojo)
```

**Separación de responsabilidades:** `grafo/` no sabe nada de luz ni de agua (solo
nodos, aristas y recorridos). `modelo/` define la red concreta. `analisis/` decide qué
significa "estar averiado" y calcula las métricas. `simulacion/` aplica los fallos.

---

## 3. Algoritmos

| Algoritmo | Dónde | Para qué |
|---|---|---|
| **BFS** desde las fuentes activas | `AnalizadorRed.alcanzables` | Saber qué nodos siguen recibiendo servicio respetando la dirección del flujo |
| **Punto fijo** sobre dependencias | `AnalizadorRed.estadoActivacion` | Propagar fallos entre servicios: si SUB-1 cae, BMB-2 cae, y sus consumidores se quedan sin agua |
| **Componentes conexas** | `AnalizadorRed.componentesConexas` | Contar las "islas" de red que quedan tras un fallo |
| **Dijkstra** | `Grafo.caminoMasCorto` | Ruta de alimentación más corta; replanifica si se corta una conexión |
| **Fuerza bruta N-1** | `AnalizadorRed.puntosUnicosDeFallo` | Detectar puntos únicos de fallo (averiar un solo nodo deja consumidores sin servicio) |
| **Monte Carlo** | `SimuladorFallas.robustez` | N fallos aleatorios: pérdida media de demanda y tasa de impacto |

Métrica central: la **demanda atendida**. Cada consumidor tiene demanda (kW o L/min);
si queda fuera del BFS de las fuentes activas, su demanda se pierde. Eso permite
comparar escenarios con un número, no solo con una lista de nodos.

---

## 4. Cómo compilar y ejecutar

Requiere **JDK 11 o superior** (`javac` y `java`).

```bash
# Compilar
./compilar.sh          # equivale a: javac -encoding UTF-8 -d out $(find src -name "*.java")

# Menú interactivo (recomendado para "meter fallos" a mano)
./ejecutar.sh

# Demo guiada: topología, 3 escenarios, análisis N-1 y robustez
./ejecutar.sh --demo

# Escenario concreto (1..7)
./ejecutar.sh --escenario 3

# Exportar datos y diagrama
./ejecutar.sh --csv red.csv
./ejecutar.sh --svg red.svg

# Pruebas automáticas (salen con código 1 si algo falla)
java -cp out servicios.PruebasGrafo
```

Sin scripts:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -Dfile.encoding=UTF-8 -cp out servicios.Main
```

---

## 5. Cómo meter fallos

### Desde el menú
```
 3) Provocar fallo en un NODO        -> escribe TRF-3, SUB-1, EMP-1, BMB-1...
 4) Provocar fallo en una CONEXIÓN   -> escribe E05, A09...
 5) Aplicar escenario predefinido    -> 1..7
 6) Fallo aleatorio
 7) Reparar toda la red
```

### Escenarios predefinidos

| # | Escenario | Qué prueba |
|---|---|---|
| 1 | Apagón en SUB-1 | Cascada eléctrica → bomba BMB-2 sin luz → barrios del sur sin agua |
| 2 | Transformador 3 quemado | Redundancia del anillo: POS-3 se realimenta, no hay corte |
| 3 | Rotura en EMP-1 | Fallo local de agua; la luz no se ve afectada |
| 4 | Bomba BMB-1 fuera de servicio | Mitad norte de la red de agua sin presión |
| 5 | Tormenta: 2 postes + 1 válvula | Fallos simultáneos en los dos servicios |
| 6 | Planta + reservorio averiados | Corte total: 0 % de servicio |
| 7 | Fallo triple aleatorio | Escenario no reproducible (usa aleatoriedad) |

### Desde código

```java
RedServicios red = new RedServicios();
SimuladorFallas sim = new SimuladorFallas(red);
AnalizadorRed analizador = sim.getAnalizador();

sim.fallarNodo("TRF-3", "sobrecarga");        // averiar un nodo
sim.fallarArista("E14", "cable cortado");     // averiar una conexión
sim.fallarCategoria(Categoria.BOMBA, "mantenimiento"); // averiar una categoría
sim.aplicarEscenario("1");                    // escenario completo

for (InformeRed inf : analizador.analizarTodo()) {
    System.out.println(inf.resumenCorto());
    inf.detalleAfectados().forEach(System.out::println);
}

sim.repararTodo();                            // restaurar la red
```

---

## 6. Ejemplo de salida

Escenario 1 (apagón en SUB-1), tomado de `informe_ejemplo.txt`:

```
Fallos en cascada detectados (caen por dependencia de otro servicio): BMB-2

- LUZ ELÉCTRICA
LUZ   servicio= 85.7%  demanda atendida=  1080/1260 kW  (consumidores sin servicio: 1/5)
  Consumidores sin servicio:
  - HOSP-E   Hospital (luz)   demanda 180 kW -> sin camino activo desde la fuente

- AGUA POTABLE
AGUA  servicio= 80.3%  demanda atendida=  610/760 L/min  (consumidores sin servicio: 2/5)
  Consumidores sin servicio:
  - VIV-2-A  Barrio Sur (agua)   demanda  80 L/min -> cascada aguas arriba: sin BMB-2
  - VIV-3-A  Barrio Este (agua)  demanda  70 L/min -> cascada aguas arriba: sin BMB-2
```

Lectura: la subestación SUB-1 no solo deja al hospital sin luz, sino que al caer
deja sin energía a la bomba BMB-2, y eso corta el agua en dos barrios. Ese es el
**fallo en cascada** entre servicios.

### Diagrama visual (SVG)

`./ejecutar.sh --svg red.svg` (o la opción 12 del menú) genera un plano de la red
usando las coordenadas de cada nodo:

- **ámbar** = red eléctrica, **azul** = red de agua;
- **rojo** = elemento averiado **o** consumidor sin servicio (incluye las caídas en
  cascada, que es justo lo interesante de ver);
- **morado discontinuo** = dependencia entre servicios (bomba → subestación);
- forma: rombo = fuente, cuadrado = consumidor, círculo = transporte.

Al ejecutar `--escenario N` se escribe automáticamente `red_servicios.svg` con el
estado resultante, así se puede **ver** qué zona queda sin servicio.

Ejemplo con el escenario 1 aplicado: `ejemplo_fallos.svg`
(vista rápida: `docs/vista_fallos.png`).

---

## 7. Cómo ampliar el proyecto

**Agregar un nodo** en `RedServicios.construirRedElectrica()` (o `construirRedAgua()`):

```java
Nodo pos5 = nuevo("POS-5", "Poste Oeste", Categoria.POSTE, luz, 0, 1000, 470);
luz.conectar("E20", "TRF-4", "POS-5", 900, 1.2);
```

**Agregar un consumidor con demanda:**

```java
Nuevo("VIV-4-E", "Barrio Oeste (luz)", Categoria.VIVIENDA, luz, 150, 1000, 620);
luz.conectar("E21", "POS-5", "VIV-4-E", 500, 0.9);
```

**Agregar una dependencia entre servicios** (en `construirDependencias()`):

```java
indice.get("BMB-1").agregarDependencia("TRF-1", "cuadro eléctrico del bombeo");
```

Ideas para extender:
- **Capacidad real**: hoy las capacidades se guardan pero no limitan el flujo; se puede
  añadir un algoritmo de flujo máximo (Edmonds-Karp) para modelar cortes por sobrecarga.
- **Aristas no dirigidas**: `new Grafo(nombre, servicio, false)` ya está soportado.
- **Fallo por probabilidad de aristas** (mantenimiento): muestrear con pesos.
- **Interfaz gráfica**: Swing/SwingX o exportar a Graphviz (`dot`) para un dibujo real.

---

## 8. Pruebas

`java -cp out servicios.PruebasGrafo` ejecuta 20 comprobaciones, entre ellas:

- la red sana entrega el 100 % de la demanda en ambos servicios;
- un transformador caído **no** produce corte (redundancia del anillo);
- SUB-1 caído sí produce corte eléctrico **y** cascada al agua (BMB-2);
- EMP-1 caído afecta al agua pero no a la luz;
- Dijkstra encuentra y replanifica caminos;
- tras reparar todo, el servicio vuelve al 100 %;
- la prueba de robustez termina siempre con la red restaurada.

Resultado esperado:

```
RESULTADO: 20 pruebas, 0 fallidas
```
