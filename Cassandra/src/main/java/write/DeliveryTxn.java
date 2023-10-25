package write;

import utils.ItemsMetadata;
import utils.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.UDTValue;

public class DeliveryTxn implements Transaction {
    private final String warehouse_id;
    private final String carrier_id;

    public DeliveryTxn(String warehouse_id, String carrier_id) {
        this.warehouse_id = warehouse_id;
        this.carrier_id = carrier_id;
    }

    public void run(Session session, ItemsMetadata itemsMetadata) {
        for (int district_id = 1; district_id <= 10; ++district_id) {
            // Get the latest id
            String getNextOrderIdQuery = String.format(
                    "select D_NEXT_O_ID, D_LAST_UNDELIVERED_O_D from CS4224H.district_by_warehouse where w_id = %s and d_id = %s;",
                    this.warehouse_id, district_id);
            ResultSet result = session.execute(getNextOrderIdQuery);
            Row district = result.one();
            if (district == null) { 
                System.out.printf("No district %d for warehouse %d\n", district_id, this.warehouse_id);
                continue;
            }
            int next_order_id = district.getInt("D_NEXT_O_ID");
            int last_undelivered_order_id = district.getInt("D_LAST_UNDELIVERED_O_D");
            if (next_order_id == last_undelivered_order_id) { // This means no undelivered orders
                System.out.printf("No undelivered orders for district %d and warehouse %s\n", district_id,
                        this.warehouse_id);
                continue;
            }

            // Get the order by order id
            String getOrderQuery = String.format(
                    "SELECT * FROM CS4224H.orders_by_district WHERE D_W_ID = %s AND D_ID = %d AND O_ID = %d;",
                    this.warehouse_id, district_id, last_undelivered_order_id);

            result = session.execute(getOrderQuery);
            Row lastOrderOfCustomer = result.one();

            if (lastOrderOfCustomer == null) {
                System.out.printf("No customer for order %d from district %d and warehouse %s\n",
                        last_undelivered_order_id,
                        district_id,
                        this.warehouse_id);
                continue;
            }

            // Count the total amount
            int customer_id = lastOrderOfCustomer.getInt("C_ID");
            List<UDTValue> items = lastOrderOfCustomer.getList("ITEMS", UDTValue.class);
            double amount = 0;
            for (UDTValue item : items) {
                amount += item.getDecimal("OL_AMOUNT").doubleValue();
            }

            String getCustomer = String.format(
                    "SELECT * FROM CS4224H.customers WHERE DUMMY_KEY = 1 AND C_W_ID = %s AND C_D_ID = %d and C_ID = %d;\n",
                    this.warehouse_id, district_id, customer_id);
            result = session.execute(getCustomer);
            Row customer = result.one();

            int c_delivery_cnt = customer.getInt("C_DELIVERY_CNT") + 1;
            double c_balance = customer.getDecimal("C_BALANCE").doubleValue() + amount;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = sdf.format(new Date());

            String updateNextUndeliveredOrder = String.format(
                    "UPDATE CS4224H.district_by_warehouse SET D_LAST_UNDELIVERED_O_D = %d WHERE W_ID = %s AND D_ID = %d;",
                    last_undelivered_order_id + 1, this.warehouse_id, district_id);

            String updateCustomer = String.format(
                    "UPDATE CS4224H.customers SET C_BALANCE = %f, C_DELIVERY_CNT = %d WHERE DUMMY_KEY = 1 AND C_W_ID = %s AND C_D_ID = %d AND C_ID = %d;",
                    c_balance, c_delivery_cnt, this.warehouse_id, district_id, customer_id);

            String updateOrdersByCustomer = String.format(
                    "UPDATE CS4224H.orders_by_district SET O_CARRIER_ID = %s, OL_DELIVERY_D = '%s' WHERE D_W_ID = %s AND D_ID = %d AND O_ID = %d;",
                    this.carrier_id, formattedDate, this.warehouse_id, district_id, last_undelivered_order_id);

            session.execute(updateNextUndeliveredOrder);
            session.execute(updateCustomer);
            session.execute(updateOrdersByCustomer);
        }
    }
}