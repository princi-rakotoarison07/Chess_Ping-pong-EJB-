@echo off
setlocal

cd /d "%~dp0\chess-ping-ejb"

mvn clean package
if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

set WAR_FILE=target\chess-ping-ejb.war
set WILDFLY_DEPLOY=O:\wildfly-37.0.1.Final\standalone\deployments

if not exist "%WAR_FILE%" (
    echo WAR file not found: %WAR_FILE%
    exit /b 1
)

copy /Y "%WAR_FILE%" "%WILDFLY_DEPLOY%\chess-ping-ejb.war"

echo Deploiement EJB termine.
endlocal
