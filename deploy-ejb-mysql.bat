@echo off
echo =========================================
echo Deploiement EJB MySQL pour Chess Ping
echo =========================================

REM Aller au projet
cd /d "%~dp0\chess-ping-ejb-mysql"

REM Construction avec Maven
echo.
echo [1/3] Construction du projet avec Maven...
call mvn clean package
if errorlevel 1 (
    echo [ERREUR] Echec de la construction Maven
    pause
    exit /b 1
)

REM Verification du fichier WAR
set WAR_FILE=target\chess-ping-ejb-mysql.war
if not exist "%WAR_FILE%" (
    echo [ERREUR] Fichier WAR non trouve: %WAR_FILE%
    pause
    exit /b 1
)

REM Copie vers WildFly
echo.
echo [2/3] Copie vers WildFly...
set WILDFLY_DEPLOY=E:\wildfly-37.0.1.Final\standalone\deployments
echo Source: %CD%\%WAR_FILE%
echo Destination: %WILDFLY_DEPLOY%

copy /Y "%WAR_FILE%" "%WILDFLY_DEPLOY%"
if errorlevel 1 (
    echo [ERREUR] Echec de la copie
    echo Verifiez que:
    echo 1. Le chemin WildFly est correct
    echo 2. Vous avez les droits d'ecriture
    echo 3. WildFly est arrete
    pause
    exit /b 1
)

echo.
echo [3/3] Deploiement termine avec succes!
echo.
echo Instructions:
echo 1. Demarrez WildFly
echo 2. Accedez a l'application via: http://localhost:8080/chess-ping-ejb-mysql
echo.
pause