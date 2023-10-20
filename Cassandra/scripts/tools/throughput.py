import pandas as pd

df = pd.read_csv('clients.csv', header=None)

minThroughput = df[2].min().round(2)
maxThroughput = df[2].max().round(2)
avgThroughput = df[2].mean().round(2)

throughput_df = pd.DataFrame([[minThroughput, maxThroughput, avgThroughput]])

throughput_df.to_csv('throughput.csv', index=False, header=None)
