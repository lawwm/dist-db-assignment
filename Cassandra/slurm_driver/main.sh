#!/bin/bash
#
#SBATCH --job-name=cs4224h
#SBATCH --ntasks=5
#SBATCH --ntasks-per-node=1
#SBATCH --partition=long
#SBATCH --exclude=xcng0,xcng1

srun --output=task_output_%j_%t.out bash ./slave.sh