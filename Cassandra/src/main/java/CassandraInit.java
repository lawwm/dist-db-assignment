import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import table.Tables;
import utils.Transaction;
import utils.TransactionBuilder;

public class CassandraInit {
    private static final String KEYSPACE_REF = "CS4224H";
    private static final String CREATE_TEMP_ITEM_ID_INDEX_QUERY = "create index temp on customer_item_denorm (ol_i_id);";

    private static final String INVALID_ARGUMENTS_ERROR_MESSAGE = "Arguments Invalid. Enter: Host Port";

    private static final String SESSION_CONN_SUCC_MESSAGE = "Connected to session";
    private static final String CLEARED_DB_SUCC_MESSAGE = "Cleared DB";

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
            session = preprocess(session, cluster);
            //session = cluster.connect(KEYSPACE_REF);
            processTransactions(session);
            session.close();
            session = cluster.connect();
            clearDB(session);
            session.close();
            cluster.close();
        } catch (Exception e) {
            System.out.println(e);
            return;
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
        table.runCqlScript(session, "./schema.cql");
        execute();
        session.execute(CREATE_TEMP_ITEM_ID_INDEX_QUERY);
        return session;
    }

    /**
     * Builds the transactions and executes it.
     * @param session Session to execute the transaction in
     * @throws IOException
     */
    private static void processTransactions(Session session) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("./project_files/xact_files/0.txt"));
        TransactionBuilder builder = new TransactionBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            // process the line
            Transaction t = builder.build(reader, line);
            if (t == null) {
                continue;
            }
            System.out.println(t.getClass().getName());
            t.run(session);
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

    /**
     * Runs the commands stored in the given file.
     */
    private static void execute() {
        String command = "apache-cassandra-4.1.3/bin/cqlsh -f command.txt";
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
}
