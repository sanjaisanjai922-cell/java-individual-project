@echo off
call mvn -q clean package -DskipTests
if errorlevel 1 pause & exit /b 1
java -jar target\subscription-billing-management-system-1.0-jar-with-dependencies.jar
pause
