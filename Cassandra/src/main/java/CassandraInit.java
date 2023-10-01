import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

import table.Tables;
import read.OrderStatusTxn;
import utils.Transaction;
import utils.TransactionBuilder;
import java.io.BufferedReader;
import java.io.FileReader;
import utils.ItemsMetadata;

public class CassandraInit {
    private static final String DISTRICT_TABLE_BUILT_SUCC_MESSAGE = "District table built";
    private static final String KEYSPACE_REF = "CS4224H";
    private static final String DATAFILE_PATH = "./project_files/data_files";

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
    private static final String ITEM_TABLE_BUILT_SUCC_MESSAGE = "Item table built";

    private static final String[] commands = { "Create", "Load", "Run" };

    public static void main(String[] args) {
        boolean hasNecessaryArgs = args.length >= 3;
        if (!hasNecessaryArgs) {
            System.out.println(INVALID_ARGUMENTS_ERROR_MESSAGE);
            return;
        }
        try {
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String cmd = args[2];
            Cluster cluster = Cluster.builder().addContactPoint(host).withPort(port).build();
            Session session = cluster.connect(KEYSPACE_REF);
            if (cmd.equals(commands[2])) {
                ItemsMetadata itemsMetadata = new ItemsMetadata();
                itemsMetadata.populate("./scripts/data/item.csv");

                String clientpath = args[3];
                System.out.println(clientpath);
                BufferedReader reader = new BufferedReader(new FileReader(clientpath));
                TransactionBuilder builder = new TransactionBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    // process the line
                    Transaction t = builder.build(reader, line);
                    System.out.println(t.getClass().toString());
                    t.run(session, itemsMetadata);
                }
                reader.close();
            } else if (cmd.equals(commands[0])) {
                Tables tbl = new Tables();
                tbl.runCqlScript(session, "./src/main/java/cql/schema.cql");
            }

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
        buildWarehouseTable(session);
        // buildDistrictTable(session);
        // buildCustomerTable(session);
        buildItemsTable(session);
    }

    private static void buildWarehouseTable(Session session) {
        String warehouseDataPath = DATAFILE_PATH + "/warehouse.csv";
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
        PreparedStatement preparedStatement = session.prepare(
                "INSERT INTO Warehouse (W_ID, W_NAME, W_STREET_1, W_STREET_2, W_CITY, W_STATE, W_ZIP, W_TAX, W_YTD) VALUES "
                        +
                        "(?, ?, ?, ?, ?, ?, ?, ?, ?);");
        try {
            Scanner sc = new Scanner(new File(warehouseDataPath));
            while (sc.hasNextLine()) {
                String currLine = sc.nextLine();
                String[] tokens = currLine.split(",");
                int id = Integer.parseInt(tokens[0]);
                String name = tokens[1];
                String streetOne = tokens[2];
                String streetTwo = tokens[3];
                String city = tokens[4];
                String state = tokens[5];
                String zip = tokens[6];
                BigDecimal tax = new BigDecimal(tokens[7]);
                BigDecimal ytd = new BigDecimal(tokens[8]);
                BoundStatement boundStatement = preparedStatement.bind(id, name, streetOne, streetTwo, city, state,
                        zip, tax, ytd);
                session.execute(boundStatement);
            }
        } catch (FileNotFoundException e) {
            System.out.println(WAREHOUSE_FILE_NOT_FOUND_ERROR_MESSAGE);
        }
        System.out.println(WAREHOUSE_TABLE_BUILT_SUCC_MESSAGE);
    }

    private static void buildDistrictTable(Session session) {
        String districtDataPath = DATAFILE_PATH + "/district.csv";
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
        PreparedStatement preparedStatement = session.prepare(
                "INSERT INTO District (D_W_ID, D_ID, D_NAME, D_STREET_1, D_STREET_2, D_CITY, D_STATE, D_ZIP, " +
                        "D_TAX, D_YTD, D_NEXT_O_ID) VALUES " +
                        "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
        try {
            Scanner sc = new Scanner(new File(districtDataPath));
            while (sc.hasNextLine()) {
                String currLine = sc.nextLine();
                String[] tokens = currLine.split(",");
                int wId = Integer.parseInt(tokens[0]);
                int dId = Integer.parseInt(tokens[1]);
                System.out.println(tokens[0] + " " + tokens[1]);
                String name = tokens[2];
                String streetOne = tokens[3];
                String streetTwo = tokens[4];
                String city = tokens[5];
                String state = tokens[6];
                String zip = tokens[7];
                BigDecimal tax = new BigDecimal(tokens[8]);
                BigDecimal ytd = new BigDecimal(tokens[9]);
                int nextOId = Integer.parseInt(tokens[10]);
                BoundStatement boundStatement = preparedStatement.bind(wId, dId, name, streetOne, streetTwo, city,
                        state,
                        zip, tax, ytd, nextOId);
                session.execute(boundStatement);
            }
        } catch (FileNotFoundException e) {
            System.out.println(DISTRICT_FILE_NOT_FOUND_ERROR_MESSAGE);
        }
        System.out.println(DISTRICT_TABLE_BUILT_SUCC_MESSAGE);
    }

    private static void buildCustomerTable(Session session) {
        String customerDataPath = DATAFILE_PATH + "/customer.csv";
        String createCustomerTableQuery = "CREATE TABLE IF NOT EXISTS Customer (" +
                "C_W_ID INT," +
                "C_D_ID INT," +
                "C_ID INT PRIMARY KEY," +
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
                "C_DATA VARCHAR" +
                ");";
        session.execute(createCustomerTableQuery);
        PreparedStatement preparedStatement = session.prepare(
                "INSERT INTO Customer (C_W_ID, C_D_ID, C_ID, C_FIRST, C_MIDDLE, C_LAST, C_STREET_1, C_STREET_2, " +
                        "C_CITY, C_STATE, C_ZIP, C_PHONE, C_SINCE, C_CREDIT, C_CREDIT_LIM, C_DISCOUNT, C_BALANCE, " +
                        "C_YTD_PAYMENT, C_PAYMENT_CNT, C_DELIVERY_CNT, C_DATA) VALUES " +
                        "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy h:mm:ss a");
        try {
            Scanner sc = new Scanner(new File(customerDataPath));
            while (sc.hasNextLine()) {
                String currLine = sc.nextLine();
                String[] tokens = currLine.split(",");
                int wId = Integer.parseInt(tokens[0]);
                int dId = Integer.parseInt(tokens[1]);
                int cId = Integer.parseInt(tokens[2]);
                String firstName = tokens[3];
                String middleInitial = tokens[4];
                String lastName = tokens[5];
                String streetOne = tokens[6];
                String streetTwo = tokens[7];
                String city = tokens[8];
                String state = tokens[9];
                String zip = tokens[10];
                String phone = tokens[11];
                Date since = dateFormat.parse(tokens[12]);
                String credit = tokens[13];
                BigDecimal creditLimit = new BigDecimal(tokens[14]);
                BigDecimal discount = new BigDecimal(tokens[15]);
                BigDecimal balance = new BigDecimal(tokens[16]);
                double ytdPayment = Double.parseDouble(tokens[17]);
                int paymentCount = Integer.parseInt(tokens[18]);
                int deliveryCount = Integer.parseInt(tokens[19]);
                String data = tokens[20];

                BoundStatement boundStatement = preparedStatement.bind(wId, dId, cId, firstName, middleInitial,
                        lastName,
                        streetOne, streetTwo, city, state, zip, phone, since, credit, creditLimit, discount, balance,
                        ytdPayment, paymentCount, deliveryCount, data);
                session.execute(boundStatement);
            }
        } catch (FileNotFoundException e) {
            System.out.println(CUSTOMER_FILE_NOT_FOUND_ERROR_MESSAGE);
        } catch (ParseException e) {
            System.out.println(INVALID_DATE_FORMAT_ERROR_MESSAGE);
        }
        System.out.println(CUSTOMER_TABLE_BUILT_SUCC_MESSAGE);
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
