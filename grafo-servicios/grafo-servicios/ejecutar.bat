@echo off
REM Compila si hace falta y ejecuta el simulador. Argumentos opcionales:
REM   ejecutar.bat                  -> menu interactivo
REM   ejecutar.bat --demo           -> demostracion guiada
REM   ejecutar.bat --escenario 3    -> escenario concreto
REM   ejecutar.bat --csv salida.csv
cd /d "%~dp0"
if not exist out (
  if not exist out mkdir out
  dir /s /b src\*.java > fuentes.txt
  javac -encoding UTF-8 -d out @fuentes.txt
  if errorlevel 1 (
    echo ERROR de compilacion. Revisa que javac este en el PATH.
    del fuentes.txt
    pause
    exit /b 1
  )
  del fuentes.txt
)
java -Dfile.encoding=UTF-8 -cp out servicios.Main %*
