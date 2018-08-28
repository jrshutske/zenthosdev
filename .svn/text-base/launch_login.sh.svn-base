#!/bin/sh
export CLASSPATH=".:dist/odinms.jar:mina-core.jar:slf4j-api.jar:slf4j-jdk14.jar:mysql-connector-java-bin.jar"
java -Drecvops=recvops.properties \
-Dsendops=sendops.properties \
-Dwzpath=. \
-Dlogin.config=login.properties \
-Djavax.net.ssl.keyStore=login.keystore \
-Djavax.net.ssl.keyStorePassword=loginkeystorepassword \
-Djavax.net.ssl.trustStore=login.truststore \
-Djavax.net.ssl.trustStorePassword=logintruststorepassword \
net.login.LoginServer