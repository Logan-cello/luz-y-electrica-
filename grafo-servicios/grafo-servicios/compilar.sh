#!/usr/bin/env bash
# Compila el proyecto completo en ./out
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
echo "Compilado en ./out"
echo "Ejecuta:  ./ejecutar.sh          (menú interactivo)"
echo "          ./ejecutar.sh --demo   (demostración guiada)"
