import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import table.Tables;
import utils.ItemsMetadata;
import utils.Transaction;
import utils.TransactionBuilder;

public class CassandraInit {
    private static final String KEYSPACE_REF = "CS4224H";
    private static final String SCHEMA_FILE_PATH = "schema.cql";
    private static final String ITEMS_METADATA_PATH = "project_files/data_files/item.csv";

    private static final String CREATE_TEMP_ITEM_ID_INDEX_QUERY = "create index temp on customer_item_denorm (ol_i_id);";
    private static final String TRANSACTION_STATISTICS_TEMPLATE = "Total Transactions: %d, Total Elapsed Time (s): %.2f, " +
            "Transaction Throughput: %.2f, Average Latency (ms): %.2f, Median Latency (ms): %d, " +
            "95 percentile latency (ms): %d, 99 percentile latency (ms): %d \n";

    private static final String INVALID_ARGUMENTS_ERROR_MESSAGE = "Arguments Invalid. Enter: Host Port Command ClientPath";
    private static final String INVALID_TRANSACTION_ERROR_TEMPLATE = "Transaction Invalid: ";

    private static final String SESSION_CONN_SUCC_MESSAGE = "Connected to session";
    private static final String CLEARED_DB_SUCC_MESSAGE = "Cleared DB";

    private static final String[] commands = { "Create", "Run" };

    public static void main(String[] args) {
        boolean hasNecessaryArgs = args.length >= 4;
        if (!hasNecessaryArgs) {
            System.out.println(INVALID_ARGUMENTS_ERROR_MESSAGE);
            return;
        }
        try {
            ItemsMetadata itemsMetadata = new ItemsMetadata();
            itemsMetadata.populate(ITEMS_METADATA_PATH);
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String cmd = args[2];
            String clientPath = args[3];
            System.out.println(clientPath);
            Cluster cluster = Cluster.builder().addContactPoint(host).withPort(port).build();
            Session session = cluster.connect();
            System.out.println(SESSION_CONN_SUCC_MESSAGE);
            if (cmd.equals(commands[0])) {
                session = preprocess(session, cluster);
                processTransactions(session, itemsMetadata, clientPath);
            } else if (cmd.equals(commands[1])) {
                session = cluster.connect(KEYSPACE_REF);
                processTransactions(session, itemsMetadata, clientPath);
            }
            session.close();
            cluster.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Clears the DB, creates tables and fills tables with data.
     * @param session Session to generate the tables in
     * @param cluster Cluster to generate the tables in
     * @return The session with the generated tables.
     * @throws IOException
     */
    private static Session preprocess(Session session, Cluster cluster) throws IOException {
        clearDB(session);
        createKeyspace(session);
        session = cluster.connect(KEYSPACE_REF);
        Tables table = new Tables();
        ClassLoader classLoader = CassandraInit.class.getClassLoader();
        InputStream schemaStream = classLoader.getResourceAsStream(SCHEMA_FILE_PATH);
        table.runCqlScript(session, schemaStream);
        execute();
        session.execute(CREATE_TEMP_ITEM_ID_INDEX_QUERY);
        return session;
    }

    /**
     * Builds the transactions and executes it.
     * @param session Session to execute the transaction in
     * @throws IOException
     */
    private static void processTransactions(Session session, ItemsMetadata metadata, String clientPath) throws IOException {
        File clientFile = new File(clientPath);
        BufferedReader reader = new BufferedReader(new FileReader(clientFile));
        String clientNum = clientFile.getName().split("\\.")[0];
        TransactionBuilder builder = new TransactionBuilder();
        String line;
        long numTransaction = 0;
        List<Long> latencies = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        while ((line = reader.readLine()) != null) {
            // process the line
            Transaction t = builder.build(reader, line);
            if (t == null) {
                System.err.println(INVALID_TRANSACTION_ERROR_TEMPLATE + line);
                continue;
            }
            System.out.println(t.getClass().getName());
            long transStartTime = System.currentTimeMillis();
            t.run(session, metadata);
            long transEndTime = System.currentTimeMillis();
            long currLatency = transEndTime - transStartTime;
            latencies.add(currLatency);
            numTransaction++;
        }
        long endTime = System.currentTimeMillis();
        reader.close();
        computeStatistics(clientNum, numTransaction, latencies, startTime, endTime);
    }

    private static void clearDB(Session session) {
        String clearQuery = "DROP KEYSPACE IF EXISTS " + KEYSPACE_REF + ";";
        session.execute(clearQuery);
        System.out.println(CLEARED_DB_SUCC_MESSAGE);
    }

    private static void createKeyspace(Session session) {
        String nameSpaceQuery = "CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE_REF +
                " WITH replication = {" +
                "'class': 'NetworkTopologyStrategy'," +
                "'replication_factor': 3};";
        session.execute(nameSpaceQuery);
        System.out.println("Created Keyspace: " + KEYSPACE_REF);
    }

    /**
     * Runs the commands stored in the given file.
     */
    private static void execute() {
        String command = "../apache-cassandra-4.1.3/bin/cqlsh -f command.txt";
        executeCommand(command);
    }

    /**
     * Executes the command in the cmd.
     * @param command The command to run
     */
    private static void executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);

            // Capture and print the command output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String currLine;
            while ((currLine = reader.readLine()) != null) {
                System.out.println(currLine);
            }

            // Wait for the command to complete
            int exitCode = process.waitFor();
            System.out.println("Command exited with code: " + exitCode);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void computeStatistics(String clientNum, long numTransaction, List<Long> latencies, long startTime, long endTime) {
        double totalElapsedTime = (endTime - startTime) / 1000.0;
        totalElapsedTime = roundTo2DP(totalElapsedTime);
        double transThroughput = numTransaction / totalElapsedTime;
        transThroughput = roundTo2DP(transThroughput);
        OptionalDouble possibleAverageLatency = latencies.stream().mapToDouble(x -> x).average();
        double averageLatency = 0;
        if (possibleAverageLatency.isPresent()) {
            averageLatency = possibleAverageLatency.getAsDouble();
            averageLatency = roundTo2DP(averageLatency);
        }
        long[] latencyComputations = computeLatencies(latencies);
        long medianLatency = latencyComputations[0];
        long ninetyFifthPer = latencyComputations[1];
        long ninetyNinePer = latencyComputations[2];
        System.out.printf(TRANSACTION_STATISTICS_TEMPLATE, numTransaction, totalElapsedTime,
                transThroughput, averageLatency, medianLatency, ninetyFifthPer, ninetyNinePer);
        System.err.println(clientNum + "," + numTransaction + "," + totalElapsedTime + "," +
                transThroughput + "," + averageLatency + "," + medianLatency + "," +
                ninetyFifthPer + "," + ninetyNinePer);
    }

    /**
     * Computes the median, 95 percentile and 99 percentile of latencies
     * @param latencies Latencies to be used for computation
     * @return The calculated values of the latency
     */
    private static long[] computeLatencies(List<Long> latencies) {
        Collections.sort(latencies);
        int numLatencies = latencies.size();
        if (latencies.isEmpty()) {
            return new long[]{0, 0, 0};
        }
        int ninetyFifthPercentileIndex = (int) Math.ceil(0.95 * numLatencies);
        long ninetyFifthPercentile = latencies.get(ninetyFifthPercentileIndex - 1);
        int ninetyNinePercentileIndex = (int) Math.ceil(0.99 * numLatencies);
        long ninetyNinePercentile = latencies.get(ninetyNinePercentileIndex - 1);
        long median;
        int middleIndex = numLatencies / 2;
        boolean isOddMiddleIndex = middleIndex % 2 == 1;
        if (isOddMiddleIndex || middleIndex == 0) {
            median = latencies.get(middleIndex);
        } else {
            median = (latencies.get(middleIndex) + latencies.get(middleIndex - 1)) / 2;
        }
        return new long[]{median, ninetyFifthPercentile, ninetyNinePercentile};
    }

    public static double roundTo2DP(double number) {
        DecimalFormat df = new DecimalFormat("#.##");
        String formattedNumber = df.format(number);
        return Double.parseDouble(formattedNumber);
    }
}
