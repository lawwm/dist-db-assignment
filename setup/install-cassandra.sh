#!/bin/bash

CASSANDRA_VERSION="4.1.3"
DIR="../.."

echo "Downloading cassandra..."

# Download Cassandra tarball
wget https://downloads.apache.org/cassandra/$CASSANDRA_VERSION/apache-cassandra-$CASSANDRA_VERSION-bin.tar.gz

echo "Untarring cassandra..."

# Untar the downloaded file
tar -xzvf apache-cassandra-$CASSANDRA_VERSION-bin.tar.gz -C $DIR

# Change the configuration files
cp ./cassandra-rackdc.properties $DIR/apache-cassandra-$CASSANDRA_VERSION/conf/