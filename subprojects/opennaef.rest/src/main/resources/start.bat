title naef api
java -cp ./*;./lib/*; -Xmx1g -ea -Dvoss.mplsnms.rmi-service-name=mplsnms -Drunning_mode=console -Dnaef-rmi-port=38100 -Dtef-working-directory=./naef opennaef.rest.api.App
pause