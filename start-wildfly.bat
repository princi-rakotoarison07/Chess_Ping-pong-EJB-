@echo off
echo ==========================================
echo 🔥 START WILDFLY - GESTION ETUDIANTE
echo ==========================================

:: Configuration WildFly
set WILDFLY_HOME=E:\wildfly-37.0.1.Final\

echo.
echo 📋 Vérification de WildFly...

if not exist "%WILDFLY_HOME%" (
    echo ❌ WildFly non trouvé dans %WILDFLY_HOME%
    echo    Veuillez vérifier le chemin d'installation de WildFly
    pause
    exit /b 1
)
echo ✅ WildFly trouvé: %WILDFLY_HOME%

if not exist "%WILDFLY_HOME%\bin\standalone.bat" (
    echo ❌ Script de démarrage non trouvé: %WILDFLY_HOME%\bin\standalone.bat
    pause
    exit /b 1
)

:: Vérifier si WildFly est déjà démarré
echo.
echo 🔍 Vérification si WildFly est déjà démarré...
powershell -Command "try { Invoke-WebRequest -Uri 'http://localhost:8080' -TimeoutSec 3 -UseBasicParsing | Out-Null; exit 0 } catch { exit 1 }" >nul 2>&1


echo.
echo 🔥 Démarrage de WildFly...
echo    Chemin: %WILDFLY_HOME%
echo    Mode: Standalone
echo    Profil: Default
echo.
echo 📋 URLs qui seront disponibles:
echo   • Application: http://localhost:8080
echo   • Admin Console: http://localhost:9990
echo.
echo 💡 Conseils:
echo   • Utilisez Ctrl+C pour arrêter WildFly
echo   • Les logs s'afficheront dans cette fenêtre
echo   • Gardez cette fenêtre ouverte pendant l'utilisation
echo.

pause

echo 🚀 Lancement de WildFly...
echo.

:: Démarrer WildFly
cd /d "%WILDFLY_HOME%\bin"
call standalone.bat

echo.
echo WildFly s'est arrêté.
pause
