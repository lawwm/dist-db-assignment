package read;

import utils.Transaction;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;

public class StockLevelTxn implements Transaction {

    private static final String GET_ORDER_BY_DISTRICT = "SELECT POPULAR_ITEMS FROM orders_by_district WHERE D_W_ID = %d AND D_ID = %d LIMIT %d";
    private static final String GET_STOCKS_BY_WAREHOUSE = "SELECT S_QUANTITY FROM stocks_by_warehouse WHERE S_W_ID = %d AND S_I_ID = %d";

    private final int warehouse_id;
    private final int district_id;
    private final int stock_level;
    private final int last_l;

    public StockLevelTxn(String warehouse_id, String district_id, String stock_level, String last_l) {
        this.warehouse_id = Integer.parseInt(warehouse_id);
        this.district_id = Integer.parseInt(district_id);
        this.stock_level = Integer.parseInt(stock_level);
        this.last_l = Integer.parseInt(last_l);
    }

    public void run(Session session) {
        int total_number_of_items = 0;
        String output = "Total number of items ";

        String query = String.format(GET_ORDER_BY_DISTRICT, this.warehouse_id,
                this.district_id, this.last_l);

        ResultSet rs = session.execute(query);

        // Stores ol_i_id, ol_supply_w_id
        Set<List<Integer>> items_below_stock_level = new HashSet<>();

        // Retrieve (s_w_id, ol_i_id) from all L orders
        for (Row row : rs) {
            List<UDTValue> itemList = row.getList("POPULAR_ITEMS", UDTValue.class);

            for (UDTValue udt : itemList) {
                int ol_i_id = udt.getInt("OL_I_ID");
                int ol_supply_w_id = udt.getInt("OL_SUPPLY_W_ID");

                // If supply_w_id is same as warehouse_id?
                if (ol_supply_w_id == this.warehouse_id) {
                    List<Integer> item = new ArrayList<>();
                    item.add(ol_i_id);
                    item.add(ol_supply_w_id);
                    items_below_stock_level.add(item);
                }
            }
        }

        // Process each item in set
        for (List<Integer> i : items_below_stock_level) {
            int ol_i_id = i.get(0);
            int ol_supply_w_id = i.get(1);
            String query_2 = String.format(GET_STOCKS_BY_WAREHOUSE, ol_supply_w_id, ol_i_id);
            ResultSet rs_q2 = session.execute(query_2);

            // Based on schema, it should return one row
            for (Row row_2 : rs_q2) {
                int s_qty = row_2.getInt("S_QUANTITY");

                if (s_qty < stock_level) {
                    total_number_of_items += 1;
                }
            }
        }

        output += total_number_of_items;

        System.out.println(output);
    }
}