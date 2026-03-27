@echo off
setlocal

echo [DEBUG] Script directory: %~dp0
cd /d "%~dp0\chess-ping-ejb"
echo [DEBUG] Current directory after cd: %CD%

echo [DEBUG] Running Maven build...
call mvn clean package
if errorlevel 1 (
    echo [DEBUG] Build failed (errorlevel=%errorlevel%).
    exit /b 1
)

set WAR_FILE=target\chess-ping-ejb.war
set WILDFLY_DEPLOY=E:\wildfly-37.0.1.Final\standalone\deployments


echo [DEBUG] WAR_FILE=%WAR_FILE%
echo [DEBUG] WILDFLY_DEPLOY=%WILDFLY_DEPLOY%

if not exist "%WAR_FILE%" (
    echo [DEBUG] WAR file not found: %WAR_FILE%
    exit /b 1
)



echo [DEBUG] Listing target directory:
dir "%CD%\target"

echo [DEBUG] Copying "%WAR_FILE%" to "%WILDFLY_DEPLOY%\chess-ping-ejb.war" ...
copy /Y "%WAR_FILE%" "%WILDFLY_DEPLOY%\chess-ping-ejb.war"
echo [DEBUG] Copy command finished with errorlevel=%errorlevel%

echo Deploiement EJB termine.
pause
endlocal