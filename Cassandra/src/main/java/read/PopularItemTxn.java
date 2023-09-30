package read;

import utils.ItemsMetadata;
import utils.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;

public class PopularItemTxn implements Transaction {
    private static final String GET_ORDER_BY_DISTRICT = "SELECT POPULAR_ITEMS FROM orders_by_district WHERE D_W_ID = %d AND D_ID = %d LIMIT %d";

    private final int warehouse_id;
    private final int district_id;
    private final int last_l;

    public PopularItemTxn(String warehouse_id, String district_id, String last_l) {
        this.warehouse_id = Integer.parseInt(warehouse_id);
        this.district_id = Integer.parseInt(district_id);
        this.last_l = Integer.parseInt(last_l);
    }

    public void run(Session session, ItemsMetadata itemsMetadata) {

        String query = String.format(GET_ORDER_BY_DISTRICT, this.warehouse_id,
                this.district_id, this.last_l);

        ResultSet rs = session.execute(query);

        String output = String.format("(%d, %d)%n%d%n", this.warehouse_id, this.district_id, this.last_l);

        // Store all popular items and their counts
        HashMap<String, Integer> popular_item_hm = new HashMap<>();

        // Find popular items for each order
        for (Row row : rs) {
            int o_id = row.getInt("O_ID");
            int o_entry_d = row.getInt("O_ENTRY_D");
            String c_first = row.getString("C_FIRST");
            String c_middle = row.getString("C_MIDDLE");
            String c_last = row.getString("C_LAST");

            List<UDTValue> itemList = row.getList("POPULAR_ITEMS", UDTValue.class);

            // Keep track of total qty per item in current order
            HashMap<String, Integer> hm = new HashMap<>();

            // There be more than 1 popular item in an order
            int highestQty = 0;
            List<String> tempItemList = new ArrayList<>();

            for (UDTValue udt : itemList) {
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

            String a = String.format("%d %d%n", o_id, o_entry_d);
            String b = String.format("(%s %s %s)%n", c_first, c_middle, c_last);
            String c = "";

            // Updates hashmap and update output
            for (String s : tempItemList) {
                popular_item_hm.compute(s, (key, oldValue) -> (oldValue == null) ? 1 : oldValue + 1);
                c += s + "\n" + hm.get(s) + "\n";
            }

            output += a + b + c;
        }

        String d = "";

        for (String s : popular_item_hm.keySet()) {
            double percentage = (double) popular_item_hm.get(s) / this.last_l * 100;
            d += s + "" + String.format("%.2f%%", percentage) + "/n";

        }

        output += d;
        System.out.println(output);
    }
}