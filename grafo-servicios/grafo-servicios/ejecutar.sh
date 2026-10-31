#!/usr/bin/env bash
# Compila si hace falta y ejecuta el programa. Pasa los argumentos tal cual:
#   ./ejecutar.sh                 -> menú interactivo
#   ./ejecutar.sh --demo          -> demostración guiada
#   ./ejecutar.sh --escenario 3   -> escenario concreto
#   ./ejecutar.sh --csv salida.csv
set -e
cd "$(dirname "$0")"
if [ ! -d out ]; then
  mkdir -p out
  javac -encoding UTF-8 -d out $(find src -name "*.java")
fi
exec java -Dfile.encoding=UTF-8 -cp out servicios.Main "$@"
