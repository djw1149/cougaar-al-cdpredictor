echo off
set CASTELLANHOME=C:\cvslocal\hydra
set LIBPATH=%COUGAAR_INSTALL_PATH%/lib/castellan.jar;
set LIBPATH=%LIBPATH%;%COUGAAR_INSTALL_PATH%/lib/core.jar;
set LIBPATH=%LIBPATH%;%COUGAAR_INSTALL_PATH%/lib/glm.jar;
set LIBPATH=%LIBPATH%;%COUGAAR_INSTALL_PATH%/lib/util.jar;
set LIBPATH=%LIBPATH%;%COUGAAR_INSTALL_PATH%/sys/grappa1_2.jar;
set LIBPATH=%LIBPATH%;%COUGAAR_INSTALL_PATH%/sys/xerces.jar;
set LIBPATH=%LIBPATH%;%COUGAAR_INSTALL_PATH%/sys/mm-mysql-2.jar;
set LIBPATH=%LIBPATH%;%COUGAAR_INSTALL_PATH%/sys/jgl3.1.0.jar
set LIBPATH=%LIBPATH%;%COUGAAR_INSTALL_PATH%/sys/matlib.jar
java -Xmx164m -Xss2m -Dcastellanhome=%CASTELLANHOME% -cp %LIBPATH% org.cougaar.tools.castellan.server.ServerApp
