import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import table.Tables;
import read.OrderStatusTxn;
import utils.Transaction;
import utils.TransactionBuilder;
import java.io.BufferedReader;
import java.io.FileReader;

public class CassandraInit {
    private static final String DISTRICT_TABLE_BUILT_SUCC_MESSAGE = "District table built";
    private static final String KEYSPACE_REF = "CS4224H";
    private static final String DATAFILE_PATH = "./project_files/data_files";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS");

    private static final String INVALID_ARGUMENTS_ERROR_MESSAGE = "Arguments Invalid. Enter: Host Port";
    private static final String DISTRICT_FILE_NOT_FOUND_ERROR_MESSAGE = "District file not found";
    private static final String WAREHOUSE_FILE_NOT_FOUND_ERROR_MESSAGE = "Warehouse file not found";
    private static final String CUSTOMER_FILE_NOT_FOUND_ERROR_MESSAGE = "Customer file not found";
    private static final String INVALID_DATE_FORMAT_ERROR_MESSAGE = "Invalid Date format";
    private static final String ITEMS_FILE_NOT_FOUND_ERROR_MESSAGE = "Items file not found";

    private static final String SESSION_CONN_SUCC_MESSAGE = "Connected to session";
    private static final String CLEARED_DB_SUCC_MESSAGE = "Cleared DB";
    private static final String WAREHOUSE_TABLE_BUILT_SUCC_MESSAGE = "Warehouse table built";
    private static final String CUSTOMER_TABLE_BUILT_SUCC_MESSAGE = "Customer table built";
    private static final String ORDER_TABLE_BUILT_SUCC_MESSAGE = "Order table built";
    private static final String ITEM_TABLE_BUILT_SUCC_MESSAGE = "Item table built";

    public static void main(String[] args) {
        boolean hasNecessaryArgs = args.length >= 2;
        if (!hasNecessaryArgs) {
            System.out.println(INVALID_ARGUMENTS_ERROR_MESSAGE);
            return;
        }
        try {
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            System.out.println("Host: " + host + " Port: " + args[1]);
            Cluster cluster = Cluster.builder().addContactPoint(host).withPort(port).build();
            Session session = cluster.connect();
            System.out.println(SESSION_CONN_SUCC_MESSAGE);
            createKeyspace(session);
            session = cluster.connect(KEYSPACE_REF);

            Tables table = new Tables();
            table.runCqlScript(session, "./src/main/java/cql/schema.cql");
            // buildTables(session);
            session.close();
            session = cluster.connect();
            OrderStatusTxn txn = new OrderStatusTxn("1", "1", "1");
            // BufferedReader reader = new BufferedReader(new FileReader("./0.txt"));
            // TransactionBuilder builder = new TransactionBuilder();
            // String line;
            // while ((line = reader.readLine()) != null) {
            // // process the line
            // Transaction t = builder.build(reader, line);
            // System.out.println(t.getClass().getName());
            // }
            txn.run(session);

            // clearDB(session);
            session.close();
            cluster.close();
        } catch (Exception e) {
            System.out.println(e);
            return;
        }

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

    private static void buildTables(Session session) {
        createTables(session);
        execute();
        ResultSet results;
        results = session.execute("SELECT count(*) FROM \"Order\";");
        for (Row row : results) {
            System.out.println(row);
        }
    }

    private static void createTables(Session session) {
        String createWarehouseTableQuery = "CREATE TABLE IF NOT EXISTS Warehouse (" +
                "W_ID INT PRIMARY KEY," +
                "W_NAME VARCHAR," +
                "W_STREET_1 VARCHAR," +
                "W_STREET_2 VARCHAR," +
                "W_CITY VARCHAR," +
                "W_STATE VARCHAR," +
                "W_ZIP VARCHAR," +
                "W_TAX DECIMAL," +
                "W_YTD DECIMAL" + ");";
        session.execute(createWarehouseTableQuery);
        System.out.println(WAREHOUSE_TABLE_BUILT_SUCC_MESSAGE);
        String createDistrictTableQuery = "CREATE TABLE IF NOT EXISTS District (" +
                "D_W_ID INT," +
                "D_ID INT," +
                "D_NAME VARCHAR," +
                "D_STREET_1 VARCHAR," +
                "D_STREET_2 VARCHAR," +
                "D_CITY VARCHAR," +
                "D_STATE VARCHAR," +
                "D_ZIP VARCHAR," +
                "D_TAX DECIMAL," +
                "D_YTD DECIMAL," +
                "D_NEXT_O_ID INT," +
                "PRIMARY KEY ( (D_ID, D_W_ID) )" +
                ");";
        session.execute(createDistrictTableQuery);
        System.out.println(DISTRICT_TABLE_BUILT_SUCC_MESSAGE);
        String createCustomerTableQuery = "CREATE TABLE IF NOT EXISTS Customer (" +
                "C_W_ID INT," +
                "C_D_ID INT," +
                "C_ID INT," +
                "C_FIRST VARCHAR," +
                "C_MIDDLE VARCHAR," +
                "C_LAST VARCHAR," +
                "C_STREET_1 VARCHAR," +
                "C_STREET_2 VARCHAR," +
                "C_CITY VARCHAR," +
                "C_STATE VARCHAR," +
                "C_ZIP VARCHAR," +
                "C_PHONE VARCHAR," +
                "C_SINCE TIMESTAMP," +
                "C_CREDIT VARCHAR," +
                "C_CREDIT_LIM DECIMAL," +
                "C_DISCOUNT DECIMAL," +
                "C_BALANCE DECIMAL," +
                "C_YTD_PAYMENT DOUBLE," +
                "C_PAYMENT_CNT INT," +
                "C_DELIVERY_CNT INT," +
                "C_DATA VARCHAR, " +
                "PRIMARY KEY ( (C_W_ID, C_D_ID, C_ID) )" +
                ");";
        session.execute(createCustomerTableQuery);
        System.out.println(CUSTOMER_TABLE_BUILT_SUCC_MESSAGE);
        String createOrderTableQuery = "CREATE TABLE IF NOT EXISTS \"Order\" (" +
                "O_W_ID INT," +
                "O_D_ID INT," +
                "O_ID INT," +
                "O_C_ID INT," +
                "O_CARRIER_ID INT," +
                "O_OL_CNT DECIMAL," +
                "O_ALL_LOCAL DECIMAL," +
                "O_ENTRY_D TIMESTAMP," +
                "PRIMARY KEY (( O_W_ID, O_D_ID, O_ID ))" +
                ");";
        session.execute(createOrderTableQuery);
        System.out.println(ORDER_TABLE_BUILT_SUCC_MESSAGE);
    }

    private static void execute() {
        String command = "apache-cassandra-4.1.3/bin/cqlsh -f command.txt";
        executeCommand(command);
    }

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
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void buildOrderTable(Session session) {
        String orderDataPath = DATAFILE_PATH + "/order.csv";
        String createOrderTableQuery = "CREATE TABLE IF NOT EXISTS \"Order\" (" +
                "O_W_ID INT," +
                "O_D_ID INT," +
                "O_ID INT," +
                "O_C_ID INT," +
                "O_CARRIER_ID INT," +
                "O_OL_CNT DECIMAL," +
                "O_ALL_LOCAL DECIMAL," +
                "O_ENTRY_D TIMESTAMP," +
                "PRIMARY KEY (( O_W_ID, O_D_ID, O_ID ))" +
                ");";
        session.execute(createOrderTableQuery);
        PreparedStatement preparedStatement = session.prepare(
                "INSERT INTO \"Order\" (O_W_ID, O_D_ID, O_ID, O_C_ID, O_CARRIER_ID, O_OL_CNT, O_ALL_LOCAL, O_ENTRY_D) "
                        +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?);");

        try {
            Scanner sc = new Scanner(new File(orderDataPath));
            while (sc.hasNextLine()) {
                String currLine = sc.nextLine();
                String[] tokens = currLine.split(",");
                System.out.println(currLine);
                int warehouseId = Integer.parseInt(tokens[0]);
                int districtId = Integer.parseInt(tokens[1]);
                int orderId = Integer.parseInt(tokens[2]);
                int customerId = Integer.parseInt(tokens[3]);
                int carrierId = Integer.parseInt(tokens[4]);
                BigDecimal olCount = new BigDecimal(tokens[5]);
                BigDecimal allLocal = new BigDecimal(tokens[6]);
                Date entryDate = DATE_FORMAT.parse(tokens[7]);
                BoundStatement boundStatement = preparedStatement.bind(warehouseId, districtId, orderId, customerId,
                        carrierId, olCount, allLocal, entryDate);
                session.execute(boundStatement);
            }
        } catch (FileNotFoundException | ParseException e) {
            System.out.println("Order file not found or date parsing error");
        }
    }

    private static void buildItemsTable(Session session) {
        String itemDataPath = DATAFILE_PATH + "/item.csv";
        String createItemTableQuery = "CREATE TABLE IF NOT EXISTS Item (" +
                "    I_ID INT PRIMARY KEY," +
                "    I_NAME TEXT," +
                "    I_PRICE DECIMAL," +
                "    I_IM_ID INT," +
                "    I_DATA TEXT" + ");";
        session.execute(createItemTableQuery);
        PreparedStatement preparedStatement = session.prepare(
                "INSERT INTO Item (I_ID,I_NAME,I_PRICE,I_IM_ID,I_DATA) VALUES (?, ?, ?, ?, ?);");
        try {
            Scanner sc = new Scanner(new File(itemDataPath));
            while (sc.hasNextLine()) {
                String currLine = sc.nextLine();
                String[] tokens = currLine.split(",");
                int id = Integer.parseInt(tokens[0]);
                String name = tokens[1];
                BigDecimal price = new BigDecimal(tokens[2]);
                int imID = Integer.parseInt(tokens[3]);
                String data = tokens[4];
                BoundStatement boundStatement = preparedStatement.bind(id, name, price, imID, data);
                session.execute(boundStatement);
            }
        } catch (FileNotFoundException e) {
            System.out.println(ITEMS_FILE_NOT_FOUND_ERROR_MESSAGE);
        }
        System.out.println(ITEM_TABLE_BUILT_SUCC_MESSAGE);
    }

}
