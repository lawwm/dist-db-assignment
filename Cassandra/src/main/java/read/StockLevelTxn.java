package read;

import utils.ItemsMetadata;
import utils.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;

public class StockLevelTxn implements Transaction {

    private static final String GET_ORDER_BY_DISTRICT = "SELECT POPULAR_ITEMS FROM orders_by_district WHERE D_W_ID = %s AND D_ID = %s LIMIT %s";
    private static final String GET_STOCKS_BY_WAREHOUSE = "SELECT S_QUANTITY FROM stocks_by_warehouse WHERE S_W_ID = %s AND S_I_ID = %s";

    private final String warehouse_id;
    private final String district_id;
    private final String stock_level;
    private final String last_l;

    public StockLevelTxn(String warehouse_id, String district_id, String stock_level, String last_l) {
        this.warehouse_id = warehouse_id;
        this.district_id = district_id;
        this.stock_level = stock_level;
        this.last_l = last_l;
    }

    public void run(Session session, ItemsMetadata itemsMetadata) {
        int total_number_of_items = 0;
        String output = String.format("Total number of items below threshold of %s: ", this.stock_level);

        String query = String.format(GET_ORDER_BY_DISTRICT, this.warehouse_id,
                this.district_id, this.last_l);

        ResultSet rs = session.execute(query);

        Set<Integer> items_below_stock_level = new HashSet<>();

        for (Row row : rs) {
            List<UDTValue> itemList = row.getList("POPULAR_ITEMS", UDTValue.class);

            for (UDTValue udt : itemList) {
                int ol_i_id = udt.getInt("OL_I_ID");

                int ol_supply_w_id = udt.getInt("OL_SUPPLY_W_ID");

                // If supply_w_id is same as warehouse_id? I think don't need this condition
                if (ol_supply_w_id == Integer.parseInt(this.warehouse_id)) {
                    items_below_stock_level.add(ol_i_id);
                }
            }
        }

        // Process each item in set
        for (Integer i : items_below_stock_level) {
            int ol_i_id = i;
            String query_2 = String.format(GET_STOCKS_BY_WAREHOUSE, this.warehouse_id, ol_i_id);
            Row row_2 = session.execute(query_2).one();

            int s_qty = row_2.getDecimal("S_QUANTITY").intValue();
            if (s_qty < Integer.parseInt(this.stock_level)) {
                total_number_of_items += 1;
            }
        }

        output += total_number_of_items;

        System.out.printf(output + "\n");
    }
}