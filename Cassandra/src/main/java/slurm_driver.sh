#!/bin/bash
#SBATCH --job-name=cassandra
#SBATCH --partition=medium
#SBATCH --nodes=5
#SBATCH --ntasks=5
#SBATCH --mem=1gb
#SBATCH --time=00:05:00
#SBATCH --output=cassandraDriver_%j.log
#SBATCH --error=cassandraDriverErr_%j.log
#SBATCH --nodelist=xcnd[38,39,40,41,42]
echo "Running job!"
echo "We are running on $(hostname)"
echo "Job started at $(date)"
# Actual "job"
host="127.0.0.1"
port="1234"
command="Create"
xactFolder="/home/stuproj/cs4224h/project_files/xact_files"
cassandraJar="CassandraProcessor.jar"

for i in {0..4}; do
  srun --nodes=1 --ntasks=1 -r $i slurm_batch.sh
done

for i in {0..19}; do
  selected_node=$(( i % 5 ))
  client_file="$xactFolder/$i.txt"
  srun --nodes=1 --ntasks=1 -r $selected_node java -jar $cassandraJar $host $port $command $client_file
done
# This is useful to know (in the logs) when the job ends
echo "Job ended at $(date)"
