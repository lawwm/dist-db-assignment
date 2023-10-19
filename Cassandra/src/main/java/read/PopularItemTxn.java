package read;

import utils.ItemsMetadata;
import utils.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;

public class PopularItemTxn implements Transaction {
    private static final String GET_ORDER_BY_DISTRICT = "SELECT * FROM orders_by_district WHERE D_W_ID = %s AND D_ID = %s LIMIT %s";

    private final String warehouse_id;
    private final String district_id;
    private final String last_l;

    public PopularItemTxn(String warehouse_id, String district_id, String last_l) {
        this.warehouse_id = warehouse_id;
        this.district_id = district_id;
        this.last_l = last_l;
    }

    public void run(Session session, ItemsMetadata itemsMetadata) {

        String query = String.format(GET_ORDER_BY_DISTRICT, this.warehouse_id,
                this.district_id, this.last_l);

        ResultSet rs = session.execute(query);

        System.out.printf("1. District identifier %s, %s\n", this.warehouse_id, this.district_id);
        System.out.printf("2. Number of last orders to be examined %s\n", this.last_l);

        // Store all popular items and their counts
        HashMap<String, Integer> popular_item_hm = new HashMap<>();
        // Find popular items for each order
        for (Row row : rs) {
            int o_id = row.getInt("O_ID");
            Date o_entry_d = row.getTimestamp("O_ENTRY_D");
            String c_first = row.getString("C_FIRST");
            String c_middle = row.getString("C_MIDDLE");
            String c_last = row.getString("C_LAST");
            List<UDTValue> itemList = row.getList("POPULAR_ITEMS", UDTValue.class);

            // Keep track of total qty per item in current order
            HashMap<String, Integer> hm = new HashMap<>();

            // There be more than 1 popular item in an order
            int highestQty = 0;
            List<String> tempItemList = new ArrayList<>();

            for (var udt : itemList) {
                String i_name = udt.getString("I_NAME");
                int ol_qty = udt.getInt("OL_QUANTITY");

                hm.compute(i_name, (key, oldValue) -> (oldValue == null) ? ol_qty : oldValue + ol_qty);

                int curr_qty = hm.get(i_name);
                if (curr_qty > highestQty) {
                    highestQty = curr_qty;
                    tempItemList.clear();
                    tempItemList.add(i_name);

                } else if (curr_qty == highestQty) {
                    tempItemList.add(i_name);
                }
            }

            System.out.printf("3. Order number %d and time %s\n", o_id, o_entry_d.toString());
            System.out.printf("   Customer name %s %s %s\n", c_first, c_middle, c_last);
            String c = "";

            // Updates hashmap and update output
            for (String s : tempItemList) {
                popular_item_hm.compute(s, (key, oldValue) -> (oldValue == null) ? 1 : oldValue + 1);
                c += "   Item name: " + s + ", Quantity: " + hm.get(s) + "\n";
            }

            System.out.printf(c);
        }


        String d = "";
        for (String s : popular_item_hm.keySet()) {
            double percentage = (double) popular_item_hm.get(s) / Integer.parseInt(this.last_l) * 100;
            d += "   Item name: " + s + ", orders containing item " + String.format("%.2f%%", percentage) + "\n";
        }
        System.out.printf("4. Distinct items %n%s", d);
    }
}