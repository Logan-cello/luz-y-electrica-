# Gestión de redes de servicios con grafos (Java 21)

## Compilar y ejecutar
```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out redes.app.Main --demo            # corre las salidas sobre datos/red_agua.txt
java -cp out redes.app.Main datos/red_agua.txt # menú interactivo
```

## Estructura
| Paquete | Clase | Rol |
|---|---|---|
| `modelo` | `Nodo`, `Arista` | Vértices y aristas inmutables (records) |
| `modelo` | `Grafo` | Lista de adyacencia no dirigida, admite aristas paralelas |
| `modelo` | `Escenario` | Conjunto inmutable de fallas (permite simular hipótesis) |
| `modelo` | `Red` | Grafo + parámetros físicos + escenario vigente |
| `algoritmos` | `Servicio` | Dijkstra multi-fuente (máxima presión) -> conectividad real |
| `algoritmos` | `PlanReparacion` | Voraz con `PriorityQueue` por dispositivos restablecidos |
| `algoritmos` | `Vulnerabilidad` | Tarjan: puentes y puntos de articulación + impacto real |
| `algoritmos` | `FlujoCapacidad` | Edmonds-Karp: flujo máximo con límites de arista y nodo |
| `io` | `CargadorRed` | Carga estructurada desde archivo de texto |
| `app` | `Main`, `Reportes` | Menú de consola y reportes |

## Formato de entrada
Ver comentarios en `datos/red_agua.txt` y en `CargadorRed`.
