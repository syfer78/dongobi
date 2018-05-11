#!/bin/sh

if [ -z "$WORK_HOME" ]; then
    WORK_HOME=$(dirname $0)/..
fi

java -dgenomatch.log.home=${WORK_HOME}/logs -cp ""${WORK_HOME}/lib/genomatch.jar:${WORK_HOME}/lib/jetty-client-9.4.8.v20171121.jar:${WORK_HOME}/lib/jetty-http-9.4.8.v20171121.jar:${WORK_HOME}/lib/jetty-io-9.4.8.v20171121.jar:${WORK_HOME}/lib/jetty-server-9.4.8.v20171121.jar:${WORK_HOME}/lib/jetty-util-9.4.8.v20171121.jar:${WORK_HOME}/lib/mysql-connector-java-5.1.6.jar" me.genomatch.Main