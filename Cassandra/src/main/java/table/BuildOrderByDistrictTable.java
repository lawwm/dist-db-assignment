import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.schemabuilder.UDTType;

public class BuildOrderByDistrictTable {
    private static final String GET_ORDER = "SELECT O_W_ID, O_D_ID, O_ID, O_C_ID, O_OL_CNT, O_ENTRY_D FROM Order";
    private static final String GET_CUSTOMER = "SELECT C_FIRST, C_MIDDLE, C_LAST FROM CUSTOMER WHERE C_W_ID = %d AND C_D_ID = %d AND C_ID = %d";
    private static final String GET_ORDERLINE = "SELECT OL_I_ID, OL_SUPPLY_W_ID, OL_QUANTITY, OL_AMOUNT FROM Order-Line WHERE OL_W_ID = %d AND OL_D_ID = %d AND OL_O_ID = %d AND OL_NUMBER = %d";

    // Build based on each order
    public void build(Session session) {
        String q_1 = String.format(GET_ORDER);

        ResultSet rs_1 = session.execute(q_1);

        // For each order
        for (Row row_1 : rs_1) {
            int o_w_id = row_1.getInt("O_W_ID");
            int o_d_id = row_1.getInt("O_D_ID");
            int o_id = row_1.getInt("O_ID");
            int o_c_id = row_1.getInt("O_C_ID");
            int o_ol_cnt = row_1.getInt("O_OL_CNT");
            int o_entry_d = row_1.getTimestamp("O_ENTRY_D");

            String q_2 = String.format(GET_CUSTOMER, o_w_id, o_id, o_c_id);
            String c_first = "";
            String c_middle = "";
            String c_last = "";

            List<UDTValue> itemList = new ArrayList<>();

            ResultSet rs_2 = session.execute(q_2);

            // Grab Customer information
            for (Row row_2 : rs_2) {
                c_first = row_2.getString("C_FIRST");
                c_middle = row_2.getString("C_MIDDLE");
                c_last = row_2.getString("C_LAST");
            }

            // For each item in orderline
            for (int ol_number = 0; ol_number < o_ol_cnt; ol_number++) {

                String q_3 = String.format(GET_ORDERLINE, o_w_id, o_d_id, o_id, ol_number);
                ResultSet rs_3 = session.execute(q_3);

                // Create orderline frozen list
                for (Row row_3 : rs_3) {
                    int ol_i_id = row_3.getInt("OL_I_ID");
                    int ol_supply_w_id = row_3.getInt("OL_SUPPLY_W_ID");
                    int ol_quantity = row_3.getInt("OL_QUANTITY");
                    int ol_amount = row_3.getInt("OL_AMOUNT");

                    UserDefinedType Item = new UserDefinedTypeBuilder("Item_Type")
                            .withField("OL_I_ID", DataTypes.INT)
                            .withField("OL_SUPPLY_W_ID", DataTypes.INT)
                            .withField("OL_QUANTITY", DataTypes.INT)
                            .withField("OL_AMOUNT", DataTypes.INT);

                    UDTValue udtItem = Item.newValue(ol_i_id,
                            ol_supply_w_id,
                            ol_quantity,
                            ol_amount);
                    itemList.add(udtItem);
                }
            }

            // Insert into order_by_district table
        }

    }

}
