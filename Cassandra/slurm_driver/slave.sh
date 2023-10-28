#!/bin/bash

echo $SLURM_PROCID "slave" $SLURM_JOB_NODELIST $(hostname -I)

### Global variables
ROOT_DIR="../../.." # Root directory
DIR="/temp/cs4224h" # Use the temp directory for storing data
CONF_DIR=$ROOT_DIR/apache-cassandra-4.1.3 # Path to apache-cassandra
SENTINEL_FILE="./sentinel.txt"
SENTINEL_FILE_2="./sentinel2.txt"
DATA_DIR="../scripts/data"
JAR_DIR="../build/libs"
CQL_SCHEMA_DIR="../src/main/resources/schema.cql" # Path of CQL file
NODE_IP_ADDR="" # Ip address of current node
TRANSACTION_DIR=$ROOT_DIR/project_files/xact_files # Xact files
MAX_FILE=19
CLIENT_CSV="./clients.csv"
SLEEP_TIME=20

configure_ip() {
  IP_1=$(hostname -I | awk '{print $1}')
  IP_2=$(hostname -I | awk '{print $2}')
  if $CONF_DIR/bin/nodetool status | grep -q $IP_1; then
    NODE_IP_ADDR=$IP_1
  else
    NODE_IP_ADDR=$IP_2
  fi
}

populate_data() {
  sleep $SLEEP_TIME
  echo "populate data"
  configure_ip
  echo "RUNNING" $NODE_IP_ADDR $CQL_SCHEMA_DIR
  $CONF_DIR/bin/cqlsh $NODE_IP_ADDR -f $CQL_SCHEMA_DIR
  dsbulk load -k cs4224h -t district_by_warehouse -url ../scripts/data/district_by_warehouse.csv -h $concatenated_ips -header false -m '0 = w_id, 1 = d_id, 2 = w_name, 3 = w_street_1, 4 = w_street_2, 5 = w_city, 6 = w_state, 7 = w_zip, 8 = w_tax, 9 = d_name, 10 = d_street_1, 11 = d_street_2, 12 = d_city, 13 = d_state, 14 = d_zip, 15 = d_tax, 16 = d_ytd, 17 = d_next_o_id, 18 = d_last_undelivered_o_d'
  dsbulk load -k cs4224h -t customers -url ../scripts/data/customer_balance.csv -h $concatenated_ips -header false -m '0 = c_w_id, 1 = c_d_id, 2 = c_id, 3 = c_first, 4 = c_middle, 5 = c_last, 6 = c_street_1, 7 = c_street_2, 8 = c_city, 9 = c_state, 10 = c_zip, 11 = c_phone, 12 = c_since, 13 = c_credit, 14 = c_credit_lim, 15 = c_discount, 16 = c_balance, 17 = c_ytd_payment, 18 = c_payment_cnt, 19 = c_delivery_cnt, 20 = c_data, 21 = w_name, 22 = d_name, 23 = dummy_key'
  dsbulk load -k cs4224h -t orders_by_district -url ../scripts/data/orders_by_district.csv -h $concatenated_ips -header false -m '0 = d_w_id, 1 = d_id, 2 = o_id, 3 = c_id, 4 = o_entry_d, 5 = o_carrier_id, 6 = ol_delivery_d, 7 = c_first, 8 = c_middle, 9 = c_last, 10 = items'
  dsbulk load -k cs4224h -t customer_item_denorm -url ../scripts/data/customer_item_denorm.csv -h $concatenated_ips -header false -m '0 = c_w_id, 1 = c_d_id, 2 = c_id, 3 = ol_i_id'
}

## STAGE 1: Set up all cassandra nodes
main() {
  # Remove temp directory
  java --version
  rm -rf $DIR
  rm ./clients.csv
  rm ./throughput.csv
  rm ./dbstate.csv
  rm $SENTINEL_FILE
  rm $SENTINEL_FILE_2
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
      sleep $SLEEP_TIME  # Optional: sleep for 5 seconds between checks
  done

  # STAGE 2: Run Data
  if [ $SLURM_PROCID -eq 0 ]; then  
    # commands to run if the slurm job is task #0
    
    # Populate data
    echo "Populating data"
    populate_data

    sleep $SLEEP_TIME
    $CONF_DIR/bin/nodetool status

    # Create sentinel file so other jobs exit while loop
    touch $SENTINEL_FILE
  else
    # commands to run otherwise
    while [ ! -f $SENTINEL_FILE ]; do
      sleep $SLEEP_TIME
      echo "Waiting for data to be populated..."
    done 
  fi

  # while true; do
  #   echo "sleeping..."
  #   sleep 60
  # done

  #Stage 2: Run 4 clients
  configure_ip

  # Run 4 clients
  for i in $(seq 0 4); do
      FILE_NUM=$(( $SLURM_PROCID + 5 * $i ))
      if [[ $FILE_NUM -le $MAX_FILE ]]; then
          echo "Running file $FILE_NUM on $NODE_IP_ADDR"

          # Run the file here, for example:
          java -jar $JAR_DIR/CassandraProcessor.jar $NODE_IP_ADDR 9042 Run $TRANSACTION_DIR/$FILE_NUM.txt &
      fi
  done

  # Clean up
  wait  
  echo "Clients completed running on this server!"

  # Wait until every single client is done
  line_count=$(wc -l < $CLIENT_CSV)
  # Loop until line count is 20
  while [ "$line_count" -ne 20 ]; do
      # Your code here...
      # Sleep for a short period before checking again (optional but recommended to avoid overloading the system)
      sleep $SLEEP_TIME
      # Update the line count for the next iteration
      line_count=$(wc -l < $CLIENT_CSV)
  done


  # Stage 3
  if [ $SLURM_PROCID -eq 0 ]; then  # commands to run if the slurm job is task #0
    # Create throughput.csv file
    python ../scripts/tools/throughput.py

    # Generate dbstate.csv
    java -jar $JAR_DIR/CassandraProcessor.jar $NODE_IP_ADDR 9042 State dbstate.csv
    
    # Create sentinel file so other jobs exit while loop
    touch $SENTINEL_FILE_2
  else
    # commands to run otherwise
    while [ ! -f $SENTINEL_FILE_2 ]; do
      sleep $SLEEP_TIME
      echo "Waiting for throughput.csv and dbstate.csv to be created..."
    done 
  fi
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
