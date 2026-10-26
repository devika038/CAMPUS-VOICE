@REM Campus Voice Maven Wrapper Script
@echo off
setlocal

set "MAVEN_CMD=C:\Users\devika\maven\apache-maven-3.9.9\bin\mvn.cmd"

if exist "%MAVEN_CMD%" (
    call "%MAVEN_CMD%" %*
) else (
    mvn %*
)
