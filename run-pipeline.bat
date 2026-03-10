@echo off
REM ═══════════════════════════════════════════════════════════════
REM  run-pipeline.bat — Ejecuta un pipeline de filtros desde CLI
REM
REM  Uso:
REM    run-pipeline.bat <archivo> <filtro1> [filtro2 ...] [--palabra=X]
REM
REM  Ejemplos:
REM    run-pipeline.bat ArchivosPrueba\1_archivo_texto.txt 1 2
REM    run-pipeline.bat ArchivosPrueba\1_archivo_texto.txt 1 6
REM    run-pipeline.bat ArchivosPrueba\1_archivo_texto.txt 1 7 --palabra=hola
REM    run-pipeline.bat ArchivosPrueba\1_archivo_texto.txt 1 8 --palabra=la
REM ═══════════════════════════════════════════════════════════════

cd /d "%~dp0"

set PIPELINE_JAR=Inicio\target\TuberiasFiltros-ejecutable.jar

if not exist "%PIPELINE_JAR%" (
    echo ✘ No se encontro el JAR principal.
    echo   Ejecuta 'build-pipeline.bat' primero.
    pause
    exit /b 1
)

if "%~1"=="" (
    echo.
    echo Uso: run-pipeline.bat ^<archivo^> ^<filtro1^> [filtro2 ...] [--palabra=X]
    echo.
    echo Filtros disponibles:
    echo   1 - Cargar archivo de texto     (PATH -^> TEXTO)
    echo   2 - Convertir texto a binario   (TEXTO -^> BINARIO)
    echo   3 - Filtro de imagenes (4 hilos) (IMAGEN -^> LISTA_IMAGEN)
    echo   4 - Binario a Base64            (BINARIO -^> LISTA_BASE64)
    echo   5 - Base64 a Binario            (BASE64 -^> BINARIO)
    echo   6 - Encriptar SHA-256           (TEXTO -^> TEXTO)
    echo   7 - Buscar palabra              (TEXTO -^> TEXTO)
    echo   8 - Contar ocurrencias          (TEXTO -^> TEXTO)
    echo.
    echo Ejemplos:
    echo   run-pipeline.bat ArchivosPrueba\1_archivo_texto.txt 1 2
    echo   run-pipeline.bat ArchivosPrueba\1_archivo_texto.txt 1 7 --palabra=hola
    echo.
    pause
    exit /b 0
)

echo.
echo ════════════════════════════════════════════════════
echo   Ejecutando Pipeline de Tuberias y Filtros...
echo ════════════════════════════════════════════════════
echo.

java -cp "%PIPELINE_JAR%" inicio.MainPipeline %*

echo.
if %ERRORLEVEL% EQU 0 (
    echo ✔ Pipeline ejecutado exitosamente.
) else (
    echo ✘ Pipeline finalizo con errores.
)
echo.
