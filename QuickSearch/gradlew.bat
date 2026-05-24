@rem Gradle startup script for Windows
@if "%DEBUG%"=="" @echo off
set GRADLE_OPTS=%GRADLE_OPTS% -Xmx512m -Xms256m
set APP_HOME=%~dp0
java %GRADLE_OPTS% -jar "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" %*
