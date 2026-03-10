@echo off
REM ═══════════════════════════════════════════════════════════════
REM  build-pipeline.bat — Compila y empaqueta todos los módulos
REM
REM  Genera:
REM    - 8 JARs ejecutables (uno por filtro) en cada modulo/target/
REM    - 1 fat JAR principal en Inicio/target/TuberiasFiltros-ejecutable.jar
REM
REM  Uso: build-pipeline.bat
REM ═══════════════════════════════════════════════════════════════

echo.
echo ════════════════════════════════════════════════════
echo   Compilando y empaquetando Tuberias y Filtros...
echo ════════════════════════════════════════════════════
echo.

cd /d "%~dp0"
call mvn clean package -q

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ✘ Error en la compilacion. Revisa los errores arriba.
    pause
    exit /b 1
)

echo.
echo ════════════════════════════════════════════════════
echo   ✔ Compilacion exitosa
echo ════════════════════════════════════════════════════
echo.
echo JARs generados:
echo   Filtro 1: 1_CargarArchivo\target\1-CargarArchivo-1.0-SNAPSHOT.jar
echo   Filtro 2: 2_TextoABinario\target\2-TextoABinario-1.0-SNAPSHOT.jar
echo   Filtro 3: 3_FiltroImagenes\target\3-FiltroImagenes-1.0-SNAPSHOT.jar
echo   Filtro 4: 4_BinarioABase64\target\4-BinarioABase64-1.0-SNAPSHOT.jar
echo   Filtro 5: 5_Base64ABinario\target\5-Base64ABinario-1.0-SNAPSHOT.jar
echo   Filtro 6: 6_EncriptarSHA256\target\6-EncriptarSHA256-1.0-SNAPSHOT.jar
echo   Filtro 7: 7_BuscarPalabra\target\7-BuscarPalabra-1.0-SNAPSHOT.jar
echo   Filtro 8: 8_ContarOcurrencias\target\8-ContarOcurrencias-1.0-SNAPSHOT.jar
echo.
echo   GUI:      Inicio\target\TuberiasFiltros-ejecutable.jar
echo   Pipeline: java -cp Inicio\target\TuberiasFiltros-ejecutable.jar inicio.MainPipeline
echo.
