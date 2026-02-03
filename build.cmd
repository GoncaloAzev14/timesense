@echo off

setlocal

for /f "tokens=1,2 delims==" %%A in (VERSION) do (
    set "%%A=%%B"
)

if "%revision%" == "" (
    echo "Version is not defined"
    exit /b 1
)
mvn "-Drevision=%revision%" "-Dchangelist=%changelist%" "-Dsha1=%sha1%" %*

endlocal
