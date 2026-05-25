@echo off
set "KKFILEVIEW_BIN_FOLDER=%cd%"
cd "%KKFILEVIEW_BIN_FOLDER%"
set "JAR_NAME="
for %%F in (kkFileView-*.jar) do (
    set "JAR_NAME=%%~nxF"
    goto :jar_found
)
echo Error: kkFileView jar not found in %KKFILEVIEW_BIN_FOLDER%
exit /b 1

:jar_found
echo Using KKFILEVIEW_BIN_FOLDER %KKFILEVIEW_BIN_FOLDER%
echo Using JAR_NAME %JAR_NAME%
echo Starting kkFileView...
echo Please check log file in ../log/kkFileView.log for more information
echo For support, please use Mango project documentation and administrator channels
java -Dspring.config.location=..\config\application.properties -jar "%JAR_NAME%" > ..\log\kkFileView.log 2>&1
