@echo off
rem Compiles and starts YEP on Windows. Needs Java 17 or newer (JDK). Open http://localhost:8080 afterwards.
cd /d "%~dp0"
if exist out rmdir /s /q out
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 goto end
del sources.txt
java -cp out yep.Main %*
:end
pause
