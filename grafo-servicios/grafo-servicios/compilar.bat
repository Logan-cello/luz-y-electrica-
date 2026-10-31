@echo off
REM Compila el proyecto en .\out (requiere JDK 11+)
cd /d "%~dp0"
if not exist out mkdir out
dir /s /b src\*.java > fuentes.txt
javac -encoding UTF-8 -d out @fuentes.txt
if errorlevel 1 (
  echo.
  echo ERROR de compilacion. Revisa que javac este en el PATH.
  pause
  exit /b 1
)
del fuentes.txt
echo.
echo Compilado en .\out
echo Ejecuta:  ejecutar.bat          (menu interactivo)
echo           ejecutar.bat --demo   (demostracion guiada)
pause
