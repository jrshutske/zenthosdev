#!/bin/sh
export CLASSPATH=".:dist/odinms.jar:mina-core.jar:slf4j-api.jar:slf4j-jdk14.jar:mysql-connector-java-bin.jar"
java -Drecvops=recvops.properties \
-Dsendops=sendops.properties \
-Dwzpath=. \
-Dchannel.config=channel.properties \
-Djavax.net.ssl.keyStore=channel.keystore \
-Djavax.net.ssl.keyStorePassword=channelkeystorepass \
-Djavax.net.ssl.trustStore=channel.truststore \
-Djavax.net.ssl.trustStorePassword=channeltruststorepass \
net.channel.ChannelServer