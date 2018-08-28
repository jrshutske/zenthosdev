#!/bin/sh
export CLASSPATH=".:dist/odinms.jar:mina-core.jar:slf4j-api.jar:slf4j-jdk14.jar:mysql-connector-java-bin.jar"
java -Drecvops=recvops.properties \
-Dsendops=sendops.properties \
-Dwzpath=. \
-Djavax.net.ssl.keyStore=world.keystore \
-Djavax.net.ssl.keyStorePassword=worldkeystorepassword \
-Djavax.net.ssl.trustStore=world.truststore \
-Djavax.net.ssl.trustStorePassword=worldtruststorepassword \
net.world.WorldServer