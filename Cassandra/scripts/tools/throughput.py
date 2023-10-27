def process_throughput(input_filename, output_filename):
    with open(input_filename, 'r') as f:
        lines = f.readlines()
    
    throughputs = [float(line.split(',')[3]) for line in lines]
    
    min_throughput = min(throughputs)
    max_throughput = max(throughputs)
    avg_throughput = sum(throughputs) / len(throughputs)
    
    with open(output_filename, 'w') as f:
        f.write(f"{round(min_throughput, 2)},{round(max_throughput, 2)},{round(avg_throughput, 2)}\n")
        
process_throughput('clients.csv', 'throughput.csv')