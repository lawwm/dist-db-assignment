#!/bin/bash
#
#SBATCH --job-name=cs4224h
#SBATCH --ntasks=5
#SBATCH --time=1:00:00
#SBATCH --ntasks-per-node=1
#SBATCH --partition=medium

srun --output=task_output_%j_%t.out bash ./slave.sh