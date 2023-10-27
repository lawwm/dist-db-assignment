package write;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.UserType;

import utils.ItemsMetadata;
import utils.Transaction;

public class NewOrderTxn implements Transaction {
    private final String warehouse_id;
    private final String district_id;
    private final String customer_id;
    private final String[][] items;
    private final String GET_NEXT_ORDER_ID = "select d_next_o_id from CS4224H.district_by_warehouse where w_id = %s and d_id = %s;";
    private final String UPDATE_NEXT_ORDER_ID = "UPDATE CS4224H.district_by_warehouse SET D_NEXT_O_ID = %s WHERE W_ID = %s AND D_ID = %s;";

    private final String UPDATE_CUSTOMER_ITEM_DENORM_QUERY = "Insert into customer_item_denorm " +
            "(C_W_ID, C_D_ID, C_ID, OL_I_ID) values (?, ?, ?, ?);";

    public NewOrderTxn(String customer_id, String district_id, String warehouse_id, String[][] items) {
        this.warehouse_id = warehouse_id;
        this.district_id = district_id;
        this.customer_id = customer_id;
        this.items = items;
    }

    public void run(Session session, ItemsMetadata itemsMetadata) {
        runTransFourFive(session, itemsMetadata);
        runTransEight(session);
    }

    private void runTransFourFive(Session session, ItemsMetadata itemsMetadata) {
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
        List<Integer> updated_s_quantity = new ArrayList<>();

        for (int i = 0; i < items.length; i++) {
            int ol_i_id = Integer.parseInt(items[i][0]);
            int ol_supply_w_id = Integer.parseInt(items[i][1]);
            int ol_quantity = Integer.parseInt(items[i][2]);
            BigDecimal ol_amount = itemsMetadata.getItemPrice(ol_i_id);

            totalAmount += (double) ol_quantity * ol_amount.doubleValue();

            UDTValue item = itemType.newValue()
                    .setInt("OL_I_ID", ol_i_id)
                    .setInt("OL_SUPPLY_W_ID", ol_supply_w_id)
                    .setInt("OL_QUANTITY", ol_quantity)
                    .setDecimal("OL_AMOUNT", ol_amount);
            udtItems.add(item);

            String getStockQty = String.format(
                    "SELECT * FROM CS4224H.stocks_by_warehouse WHERE S_W_ID = %s AND S_I_ID = %s;",
                    ol_supply_w_id, ol_i_id);
            Row row = session.execute(
                    getStockQty)
                    .one();
            int s_qty = row.getDecimal("S_QUANTITY").intValue();
            int s_ytd = row.getDecimal("S_YTD").intValue();
            int s_order_cnt = row.getInt("S_ORDER_CNT");
            int s_remote_cnt = row.getInt("S_REMOTE_CNT");

            int adj_qty = s_qty - ol_quantity;

            if (adj_qty < 10) {
                adj_qty += 100;
            }

            updated_s_quantity.add(adj_qty);
            int update_ytd = s_ytd + ol_quantity;
            int update_order_cnt = s_order_cnt + 1;
            int update_remote_cnt = s_remote_cnt + (Integer.parseInt(warehouse_id) != ol_supply_w_id ? 1 : 0);

            String updateStock = String.format(
                    "UPDATE CS4224H.stocks_by_warehouse SET S_QUANTITY = %s, S_YTD = %s, S_ORDER_CNT = %s, S_REMOTE_CNT = %s "
                            +
                            "WHERE S_W_ID = %s AND S_I_ID = %s;",
                    adj_qty, update_ytd, update_order_cnt, update_remote_cnt, ol_supply_w_id, ol_i_id);
            session.execute(updateStock);

        }

        PreparedStatement ps = session.prepare(
                "INSERT INTO orders_by_customer (C_W_ID, C_D_ID, C_ID, O_ID) VALUES (?, ?, ?, ?)");

        // need to get the OL_AMOUNT for item in a metadata class
        // need to get the O_ID from the order table
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date currDate = new Date();
        BoundStatement bound = ps.bind(Integer.parseInt(warehouse_id), Integer.parseInt(district_id),
                Integer.parseInt(customer_id), order_id);
        session.execute(bound);

        // Customer identifier (W ID, D ID, C ID), lastname C LAST, credit C CREDIT,
        String getCustomer = String.format(
                "SELECT C_FIRST, C_MIDDLE, C_LAST, C_CREDIT, C_DISCOUNT FROM CS4224H.customers WHERE DUMMY_KEY = 1 AND C_W_ID = %s AND C_D_ID = %s AND C_ID = %s;",
                this.warehouse_id, this.district_id, this.customer_id);
        Row customer = session.execute(
                getCustomer)
                .one();

        System.out.printf("1. Customer identifier: %s %s %s, lastname %s, credit %s, discount %.2f\n",
                this.warehouse_id, this.district_id, this.customer_id, customer.getString("C_LAST"),
                customer.getString("C_CREDIT"),
                customer.getDecimal("C_DISCOUNT").doubleValue());

        // transaction 5
        PreparedStatement ps_tx5 = session.prepare(
                "INSERT INTO orders_by_district (D_W_ID, D_ID, O_ID, C_ID, O_ENTRY_D, O_CARRIER_ID, OL_DELIVERY_D, C_FIRST, C_MIDDLE, C_LAST, ITEMS) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        BoundStatement bound_tx5 = ps_tx5.bind(Integer.parseInt(warehouse_id), Integer.parseInt(district_id), order_id, Integer.parseInt(this.customer_id), 
                currDate, null, null, customer.getString("C_FIRST"), customer.getString("C_MIDDLE"), customer.getString("C_LAST"), udtItems);
        session.execute(bound_tx5);

        // 2. Warehouse tax rate W TAX, District tax rate D TAX
        Row district = session.execute(
                "SELECT W_TAX, D_TAX FROM CS4224H.district_by_warehouse WHERE W_ID = " + warehouse_id
                        + " AND D_ID = "
                        + district_id + ";")
                .one();
        System.out.printf("2. Warehouse tax rate %.2f, District tax rate %.2f\n",
                district.getDecimal("W_TAX").doubleValue(),
                district.getDecimal("D_TAX").doubleValue());

        // 3. Order number O ID, entry date O ENTRY D
        System.out.printf("3. Order number %s, entry date %s\n", order_id, sdf.format(currDate));

        // 4. Number of items NUM ITEMS, Total amount for order TOTAL AMOUNT
        totalAmount = totalAmount
                * (1 + district.getDecimal("W_TAX").doubleValue()
                        + district.getDecimal("D_TAX").doubleValue())
                * customer.getDecimal("C_DISCOUNT").doubleValue();
        System.out.printf("4. Number of items %d, Total amount for order %.2f\n", items.length, totalAmount);

        // 5. For each ordered item ITEM NUMBER[i], i in [1, NUM ITEMS]
        for (int i = 0; i < items.length; ++i) {
            int itemId = Integer.parseInt(items[i][0]);
            System.out.printf(
                    "ITEM_NUMBER[i] : %d, I_NAME : %s, SUPPLIER_WAREHOUSE[i]: %s, QUANTITY[i]: %s, OL_AMOUNT: %.2f, S_QUANTITY: %d\n",
                    itemId, itemsMetadata.getItemName(itemId), items[i][1], items[i][2],
                    itemsMetadata.getItemPrice(itemId),
                    updated_s_quantity.get(i));
        }
    }

    private void runTransEight(Session session) {
        int w_id = Integer.parseInt(this.warehouse_id);
        int d_id = Integer.parseInt(this.district_id);
        int c_id = Integer.parseInt(this.customer_id);
        PreparedStatement prepareCustItemDenormInsertQuery = session.prepare(UPDATE_CUSTOMER_ITEM_DENORM_QUERY);
        for (String[] currItem : items) {
            int ol_i_id = Integer.parseInt(currItem[0]);
            BoundStatement orderDenormInsertQuery = prepareCustItemDenormInsertQuery.bind(
                    w_id, d_id, c_id, ol_i_id);
            session.execute(orderDenormInsertQuery);
        }
    }
}
