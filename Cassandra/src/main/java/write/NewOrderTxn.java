package write;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;

import utils.ItemsMetadata;
import utils.Transaction;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.UserType;

public class NewOrderTxn implements Transaction {

    private final String warehouse_id;
    private final String district_id;
    private final String customer_id;
    private final String[][] items;

    private final String GET_NEXT_ORDER_ID = "select d_next_o_id from CS4224H.district_by_warehouse where w_id = %s and d_id = %s;";
    private final String UPDATE_NEXT_ORDER_ID = "UPDATE CS4224H.district_by_warehouse SET D_NEXT_O_ID = %s WHERE W_ID = %s AND D_ID = %s;";

    public NewOrderTxn(String customer_id, String warehouse_id, String district_id, String[][] items) {
        this.warehouse_id = warehouse_id;
        this.district_id = district_id;
        this.customer_id = customer_id;
        this.items = items;
    }

    public void run(Session session, ItemsMetadata itemsMetadata) {
        // do atomic CAS
        String getNextOrderIdQuery = String.format(GET_NEXT_ORDER_ID, this.warehouse_id, this.district_id);
        ResultSet result = session.execute(getNextOrderIdQuery);
        Row districtRow = result.one();

        int order_id = districtRow.getInt("D_NEXT_O_ID");
        Statement updateStatement = new SimpleStatement(
                String.format(UPDATE_NEXT_ORDER_ID, order_id + 1, this.warehouse_id, this.district_id));
        session.execute(updateStatement);
        // transaction 4
        List<UDTValue> udtItems = new ArrayList<>();

        UserType itemType = session.getCluster().getMetadata().getKeyspace("CS4224H").getUserType("Item");
        double totalAmount = 0;
        for (int i = 0; i < items.length; i++) {
            totalAmount += Integer.parseInt(items[i][0]);
            UDTValue item = itemType.newValue()
                    .setInt("OL_I_ID", Integer.parseInt(items[i][0]))
                    .setInt("OL_SUPPLY_W_ID", Integer.parseInt(items[i][1]))
                    .setInt("OL_QUANTITY", Integer.parseInt(items[i][2]))
                    .setDecimal("OL_AMOUNT", itemsMetadata.getItemPrice(Integer.parseInt(items[i][0])));
            udtItems.add(item);
        }

        PreparedStatement ps = session.prepare(
                "INSERT INTO orders_by_customer (C_W_ID, C_D_ID, C_ID, O_ID, O_ENTRY_D, O_CARRIER_ID, OL_DELIVERY_D, ITEMS) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

        // need to get the OL_AMOUNT for item in a metadata class
        // need to get the O_ID from the order table
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date currDate = new Date();
        BoundStatement bound = ps.bind(Integer.parseInt(warehouse_id), Integer.parseInt(district_id),
                Integer.parseInt(customer_id), order_id, currDate, null, null, udtItems);

        session.execute(bound);

        // transaction 5
        // Customer identifier (W ID, D ID, C ID), lastname C LAST, credit C CREDIT,
        String getCustomer = String.format(
                "SELECT C_LAST, C_CREDIT, C_DISCOUNT FROM CS4224H.customers WHERE DUMMY_KEY = 1 AND C_W_ID = %s AND C_D_ID = %s AND C_ID = %s;",
                this.warehouse_id, this.district_id, this.customer_id);
        Row row = session.execute(
                getCustomer)
                .one();

        System.out.printf("1. Customer identifier: %s %s %s, lastname %s, credit %s, discount %.2f\n",
                this.warehouse_id, this.district_id, this.customer_id, row.getString("C_LAST"),
                row.getString("C_CREDIT"),
                row.getDecimal("C_DISCOUNT").doubleValue());

        // 2. Warehouse tax rate W TAX, District tax rate D TAX
        row = session.execute(
                "SELECT W_TAX, D_TAX FROM CS4224H.district_by_warehouse WHERE W_ID = " + warehouse_id + " AND D_ID = "
                        + district_id + ";")
                .one();
        System.out.printf("2. Warehouse tax rate %.2f, District tax rate %.2f\n",
                row.getDecimal("W_TAX").doubleValue(),
                row.getDecimal("D_TAX").doubleValue());

        // 3. Order number O ID, entry date O ENTRY D
        System.out.printf("3. Order number %s, entry date %s\n", order_id, sdf.format(currDate));

        // 4. Number of items NUM ITEMS, Total amount for order TOTAL AMOUNT
        System.out.printf("4. Number of items %d, Total amount for order %.2f\n", items.length, totalAmount);

        // 5. For each ordered item ITEM NUMBER[i], i ∈ [1, NUM ITEMS]
        for (int i = 0; i < items.length; ++i) {
            int itemId = Integer.parseInt(items[i][0]);
            System.out.printf(
                    "ITEM_NUMBER[i] : %d, I_NAME : %s, SUPPLIER_WAREHOUSE[i]: %s, QUANTITY[i]: %s, OL_AMOUNT: %.2f, S_QUANTITY: %d\n",
                    i, itemsMetadata.getItemName(itemId), items[i][1], items[i][2], itemsMetadata.getItemPrice(itemId),
                    12345);
        }
    }
}
