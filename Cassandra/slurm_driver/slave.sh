#!/bin/bash

echo $SLURM_PROCID "slave" $SLURM_JOB_NODELIST $(hostname -I)

### Global variables
ROOT_DIR="../../.." # Root directory
DIR="/temp/cs4224h" # Use the temp directory for storing data
CONF_DIR=$ROOT_DIR/apache-cassandra-4.1.3 # Path to apache-cassandra
SENTINEL_FILE="./sentinel.txt"
DATA_DIR="../scripts/data"
JAR_DIR="../cassandra"
CQL_SCHEMA_DIR="../src/main/resources/schema.cql" # Path of CQL file
NODE_IP_ADDR="" # Ip address of current node
TRANSACTION_DIR=$ROOT_DIR/project_files/xact_files # Xact files
MAX_FILE=19

populate_data() {
  echo "populate data"
  IP_1=$(hostname -I | awk '{print $1}')
  IP_2=$(hostname -I | awk '{print $2}')
  if $CONF_DIR/bin/nodetool status | grep -q $IP_1; then
    NODE_IP_ADDR=$IP_1
  else
    NODE_IP_ADDR=$IP_2
  fi

  echo "RUNNING" $NODE_IP_ADDR $CQL_SCHEMA_DIR
  $CONF_DIR/bin/cqlsh $NODE_IP_ADDR -f $CQL_SCHEMA_DIR && break
}

## STAGE 1: Set up all cassandra nodes
main() {
  # Remove temp directory
  java --version
  rm -rf $DIR
  rm $SENTINEL_FILE
  echo "Before temp cs4224h folder created:"
  ls /temp

  # Concatenate all ip addresses for seed providers
  IPs=()
  for node in $(scontrol show hostnames $SLURM_JOB_NODELIST); do
      IPs+=($(host $node | awk '{print $4}'))
  done

  ip_1=${IPs[0]}
  ip_2=${IPs[1]}
  ip_3=${IPs[2]}
  ip_4=${IPs[3]}
  ip_5=${IPs[4]}
  concatenated_ips="$ip_1,$ip_2,$ip_3,$ip_4,$ip_5"

  # Copy configuration over to the temp directory
  mkdir -p $DIR
  cp -r $CONF_DIR/conf $DIR
  echo "After temp cs4224h folder created:"
  ls /temp

  # Set up the custom yaml configuration depending on the compute server
  python ./cassandra_script.py ./cassandra_template.yaml $DIR/conf/cassandra.yaml "$concatenated_ips"

  # Change the directory to find configuration file
  export CASSANDRA_CONF=$DIR/conf

  # Print the generated yaml file
  cat $DIR/conf/cassandra.yaml

  # Start the Cassandra Database
  $CONF_DIR/bin/cassandra

  # Wait for all servers to finish communicating with each other
  while true; do
      $CONF_DIR/bin/nodetool status
      count=$($CONF_DIR/bin/nodetool status | grep -c 'UN')
      if [[ $count -eq 5 ]]; then
          break
      fi
      sleep 10  # Optional: sleep for 5 seconds between checks
  done

  # STAGE 2: Run Data
  if [ $SLURM_PROCID -eq 0 ]; then  
    # commands to run if the slurm job is task #0
    
    # Populate data
    echo "Populating data"
    populate_data
    x=1
    while [ $x -le 10 ]
    do
      sleep 10
      echo "RUN DATA FILE"
      $CONF_DIR/bin/nodetool status
      x=$(( $x + 1 ))
    done  
    # Create sentinel file so other jobs exit while loop
    touch $SENTINEL_FILE
  else
    # commands to run otherwise
    while [ ! -f $SENTINEL_FILE ]; do
      sleep 1
      echo "Waiting for data to be populated..."
    done 
  fi

  while true; do
    sleep 10
  done

  # Stage 2: Run 4 clients
  # # Run 4 clients
  # for i in $(seq 0 4); do
  #     FILE_NUM=$(( $SLURM_PROCID + 5 * $i ))
  #     if [[ $FILE_NUM -le $MAX_FILE ]]; then
  #         echo "Running file $FILE_NUM"

  #         # Run the file here, for example:
  #         java -jar $JAR_DIR/CassandraProcessor.jar $NODE_IP_ADDR 9042 Run $TRANSACTION_DIR/$FILE_NUM.txt &
  #     fi
  # done

  # Clean up
  wait  
}

cleanup() {
  echo "CLEANING UP..."
  rm -rf $DIR
  echo "After temp cs4224h folder deleted:"
  ls /temp
}

# Make sure to always CLEANUP on exit
trap cleanup EXIT

# Run main function
main
