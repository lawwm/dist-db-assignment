import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.SimpleStatement;

import table.Tables;
import utils.ItemsMetadata;
import utils.Transaction;
import utils.TransactionBuilder;

public class CassandraInit {
    private static final String KEYSPACE_REF = "CS4224H";
    private static final String SCHEMA_FILE_PATH = "schema.cql";
    private static final String ITEMS_METADATA_PATH = "../../../project_files/data_files/item.csv";
    private static final String DB_STATE_FILE = "dbstate.csv";

    private static final String CREATE_TEMP_ITEM_ID_INDEX_QUERY = "create index temp on customer_item_denorm (ol_i_id);";
    private static final String TRANSACTION_STATISTICS_TEMPLATE = "Total Transactions: %d, Total Elapsed Time (s): %.2f, "
            +
            "Transaction Throughput: %.2f, Average Latency (ms): %.2f, Median Latency (ms): %d, " +
            "95 percentile latency (ms): %d, 99 percentile latency (ms): %d \n";

    private static final String INVALID_ARGUMENTS_ERROR_MESSAGE = "Arguments Invalid. Enter: Host Port Command ClientPath";
    private static final String INVALID_TRANSACTION_ERROR_TEMPLATE = "Transaction Invalid: ";

    private static final String SESSION_CONN_SUCC_MESSAGE = "Connected to session";
    private static final String CLEARED_DB_SUCC_MESSAGE = "Cleared DB";

    private static final String[] commands = { "Create", "Run", "State" };

    public static void main(String[] args) {
        boolean hasNecessaryArgs = args.length >= 4;

        if (!hasNecessaryArgs) {
            System.out.println(INVALID_ARGUMENTS_ERROR_MESSAGE);
            return;
        }
        try {
            // Collect set of items from items csv file
            ItemsMetadata itemsMetadata = new ItemsMetadata();
            itemsMetadata.populate(ITEMS_METADATA_PATH);

            // Connect to cassandra cluster
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String cmd = args[2];
            String clientPath = args[3];
            System.out.println(clientPath);
            Cluster cluster = Cluster.builder().addContactPoint(host).withPort(port).build();
            Session session = cluster.connect();
            System.out.println(SESSION_CONN_SUCC_MESSAGE);

            // Run specific command
            if (cmd.equals(commands[0])) {
                session = preprocess(session, cluster);
                processTransactions(session, itemsMetadata, clientPath);
            } else if (cmd.equals(commands[1])) {
                session = cluster.connect(KEYSPACE_REF);
                processTransactions(session, itemsMetadata, clientPath);
            } else if (cmd.equals(commands[2])) {
                session = cluster.connect(KEYSPACE_REF);
                generateState(session);
            }
            session.close();
            cluster.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Clears the DB, creates tables and fills tables with data.
     * 
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
     * 
     * @param session Session to execute the transaction in
     * @throws IOException
     */
    private static void processTransactions(Session session, ItemsMetadata metadata, String clientPath)
            throws IOException {
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
            try {
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
                System.out.printf("Time taken: %d\n", currLatency);
                latencies.add(currLatency);
                numTransaction++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        long endTime = System.currentTimeMillis();
        reader.close();

        // This calculates measurements for clients.csv file
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
                "'class': 'SimpleStrategy'," +
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
     * 
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

    private static void computeStatistics(String clientNum, long numTransaction, List<Long> latencies, long startTime,
            long endTime) throws IOException {
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

        // Pass to csv file
        String statisticLine = String.format(TRANSACTION_STATISTICS_TEMPLATE, numTransaction, totalElapsedTime,
                transThroughput, averageLatency, medianLatency, ninetyFifthPer, ninetyNinePer);
        String csvStatisticLine = clientNum + "," + numTransaction + "," + totalElapsedTime + "," +
                transThroughput + "," + averageLatency + "," + medianLatency + "," +
                ninetyFifthPer + "," + ninetyNinePer + '\n';
        System.out.println(statisticLine);

        Path filePath = Paths.get("clients.csv");
        if (!Files.exists(filePath)) {
            // Create the file if it doesn't exist
            Files.createFile(filePath);
        }
        // Append to the file (or write if it's just been created)
        Files.write(filePath, csvStatisticLine.getBytes(), StandardOpenOption.APPEND);
    }

    /**
     * Computes the median, 95 percentile and 99 percentile of latencies
     * 
     * @param latencies Latencies to be used for computation
     * @return The calculated values of the latency
     */
    private static long[] computeLatencies(List<Long> latencies) {
        Collections.sort(latencies);
        int numLatencies = latencies.size();
        if (latencies.isEmpty()) {
            return new long[] { 0, 0, 0 };
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
        return new long[] { median, ninetyFifthPercentile, ninetyNinePercentile };
    }

    public static double roundTo2DP(double number) {
        DecimalFormat df = new DecimalFormat("#.##");
        String formattedNumber = df.format(number);
        return Double.parseDouble(formattedNumber);
    }

    /**
     * Generates the final state of the DB
     * 
     * @param session Used to executed queries to get final state of DB
     */

    private static void generateState(Session session) {
        String distWareQuery = "select sum(D_YTD), sum(D_NEXT_O_ID) from district_by_warehouse";
        String custQuery = "select sum(C_BALANCE), sum(C_YTD_PAYMENT), sum(C_PAYMENT_CNT), sum(C_DELIVERY_CNT) " +
                "from customers";
        String orderQuery = "select max(O_ID) from orders_by_customer";
        String orderLineQuery = "select ITEMS from orders_by_district";
        String stockQuery = "select sum(S_QUANTITY), sum(S_YTD), sum(S_ORDER_CNT), sum(S_REMOTE_CNT) " +
                "from stocks_by_warehouse";
        Row stockResultRow = session.execute(new SimpleStatement(stockQuery).setReadTimeoutMillis(65000)).one();        
        Row distWareResultRow = session.execute(distWareQuery).one();
        Row custResultRow = session.execute(custQuery).one();
        Row orderResultRow = session.execute(orderQuery).one();
        ResultSet orderLineResults = session.execute(orderLineQuery);
        BigDecimal d_ytd = distWareResultRow.getDecimal(0); // Same as W_YTD
        int d_next_o_id = distWareResultRow.getInt(1);
        BigDecimal c_balance = custResultRow.getDecimal(0);
        BigDecimal c_ytd_payment = custResultRow.getDecimal(1);
        int c_payment_cnt = custResultRow.getInt(2);
        int c_delivery_cnt = custResultRow.getInt(3);
        int o_id = orderResultRow.getInt(0);
        BigDecimal ol_amount = new BigDecimal(0);
        int ol_quantity = 0;
        int o_ol_cnt = 0;

        for (Row currRow : orderLineResults) {
            List<UDTValue> items = currRow.getList("ITEMS", UDTValue.class);
            o_ol_cnt += items.size();
            for (UDTValue currItem : items) {
                BigDecimal currOlAmount = currItem.getDecimal("OL_AMOUNT");
                int currOlQuantity = currItem.getInt("OL_QUANTITY");
                ol_amount = ol_amount.add(currOlAmount);
                ol_quantity += currOlQuantity;
            }
        }
        BigDecimal s_quantity = stockResultRow.getDecimal(0);
        BigDecimal s_ytd = stockResultRow.getDecimal(1);
        int s_order_cnt = stockResultRow.getInt(2);
        int s_remote_cnt = stockResultRow.getInt(3);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(DB_STATE_FILE));
            writeRow(writer, d_ytd.toString()); // SUM(W_YTD)
            writeRow(writer, d_ytd.toString()); // SUM(D_YTD)
            writeRow(writer, String.valueOf(d_next_o_id)); // SUM(D_NEXT_O_ID)
            writeRow(writer, c_balance.toString()); // SUM(C_BALANCE)
            writeRow(writer, c_ytd_payment.toString()); // SUM(C_YTD_PAYMENT)

            writeRow(writer, String.valueOf(c_payment_cnt)); // SUM(C_PAYMENT_CNT)
            writeRow(writer, String.valueOf(c_delivery_cnt)); // SUM(C_DELIVERY_CNT)
            writeRow(writer, String.valueOf(o_id)); // MAX(O_ID)
            writeRow(writer, String.valueOf(o_ol_cnt));
            writeRow(writer, ol_amount.toString()); // SUM(OL_AMOUNT)

            writeRow(writer, String.valueOf(ol_quantity));  // SUM(OL_QUANTITY)
            writeRow(writer, s_quantity.toString());  // SUM(S_QUANTITY)
            writeRow(writer, s_ytd.toString()); // SUM(S_YTD)
            writeRow(writer, String.valueOf(s_order_cnt));  // SUM(S_ORDER_CNT)
            writeRow(writer, String.valueOf(s_remote_cnt)); // SUM(S_REMOTE_CNT)
            writer.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void writeRow(BufferedWriter writer, String value) throws IOException {
        writer.write(value);
        writer.newLine();
    }
}
