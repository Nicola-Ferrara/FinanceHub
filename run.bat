@echo off
echo Compilazione in corso...

REM Compila tutti i file .java nelle sottodirectory di src
javac -cp "lib/*;pwd_DB" -d bin src/controller/*.java src/dao/*.java src/db_connection/*.java src/dto/*.java src/exception/*.java src/web_server/*.java
if %errorlevel% neq 0 (
    echo Errore durante la compilazione.
    pause
    exit /b %errorlevel%
)

echo Esecuzione del procgramma...
java -cp "bin;lib/*;pwd_DB" controller.Controller
pause