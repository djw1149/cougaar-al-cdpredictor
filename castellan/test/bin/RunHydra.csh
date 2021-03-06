#!/bin/tcsh 

setenv CASTELLANHOME /mnt/shared/hydra
setenv LIBPATH $COUGAAR_INSTALL_PATH/sys/castellan.jar:$COUGAAR_INSTALL_PATH/lib/core.jar:$COUGAAR_INSTALL_PATH/lib/glm.jar:$COUGAAR_INSTALL_PATH/sys/grappa1_2_bbn.jar

set LIBPATH=$COUGAAR_INSTALL_PATH/sys/xerces.jar:$COUGAAR_INSTALL_PATH/sys/mm-mysql-2.jar:${LIBPATH}:$COUGAAR_INSTALL_PATH/sys/jgl3.1.0.jar

echo java -Xmx164m -Xss2m -Dcastellanhome=$CASTELLANHOME -cp $LIBPATH org.hydra.server.ServerApp
java -Xmx164m -Xss2m -Dcastellanhome=$CASTELLANHOME -cp $LIBPATH org.hydra.server.ServerApp
