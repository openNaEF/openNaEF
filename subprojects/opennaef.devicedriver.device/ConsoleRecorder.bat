@echo off
set LIB=C:/Users/Administrator/Desktop/dist/lib/
set CLASSPATH=%LIB%voss.discovery.driver.jar
set CLASSPATH=%CLASSPATH%;%LIB%*

java -cp %CLASSPATH% -Dlogback.configurationFile=logback.xml -Xmx512m -ea -Dvossnms.root.dir=. voss.discovery.agent.ConsoleRecorder %1 %2 %3 %4 %5 %6 %7 %8 %9
pause
