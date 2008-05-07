#!/bin/sh
# @version $Revision$ ($Author$) $Date$
#
SCRIPTS_DIR=`dirname ${0}`/..
. ${SCRIPTS_DIR}/environment.sh

JAVA_HOME=/usr/lib/jvm/java-gcj

#echo BLUECOVE_TESTER_APP_JAR=${BLUECOVE_TESTER_APP_JAR}

${JAVA_HOME}/bin/java -classpath ${BLUECOVE_TESTER_APP_JAR} net.sf.bluecove.awt.Main