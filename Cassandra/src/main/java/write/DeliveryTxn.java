package write;

import utils.ItemsMetadata;
import utils.Transaction;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.datastax.driver.core.Session;
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

public class DeliveryTxn implements Transaction {

  private final String warehouse_id;
  private final String carrier_id;
  private final String GET_NEXT_ORDER_ID = "select D_NEXT_O_ID, D_LAST_UNDELIVERED_O_D from CS4224H.district_by_warehouse where w_id = %s and d_id = %s;";
  private final String UPDATE_NEXT_ORDER_ID = "UPDATE CS4224H.district_by_warehouse SET D_LAST_UNDELIVERED_O_D = %s WHERE W_ID = %s AND D_ID = %s;";

  public DeliveryTxn(String warehouse_id, String carrier_id) {
    this.warehouse_id = warehouse_id;
    this.carrier_id = carrier_id;
  }

  public void run(Session session, ItemsMetadata itemsMetadata) {
    for (int district_id = 1; district_id <= 10; ++district_id) {
      // Get the latest id
      String getNextOrderIdQuery = String.format(GET_NEXT_ORDER_ID, this.warehouse_id, district_id);
      ResultSet result = session.execute(getNextOrderIdQuery);
      Row row = result.one();
      int order_id = row.getInt("D_NEXT_O_ID");
      int last_undelivered_order_id = row.getInt("D_LAST_UNDELIVERED_O_D");
      if (order_id == last_undelivered_order_id) { // This means no undelivered orders
        System.out.printf("No undelivered orders for district %d and warehouse %d\n", district_id, this.warehouse_id);
        continue;
      }

      // Update the next undelivered order
      String updateStatement = String.format(UPDATE_NEXT_ORDER_ID, last_undelivered_order_id + 1, this.warehouse_id,
          district_id);
      session.execute(updateStatement);

      // Get the order by order id
      String getOrderQuery = String.format(
          "SELECT * FROM CS4224H.customers_by_order WHERE C_W_ID = %s AND C_D_ID = %s AND O_ID = %s;",
          this.warehouse_id, district_id, order_id);

      result = session.execute(getOrderQuery);
      row = result.one();

      int customer_id = row.getInt("C_ID");

      // get from orders_by_customer table
      String getOrderByCustomer = String.format(
          "SELECT * FROM CS4224H.orders_by_customer WHERE C_W_ID = %s AND C_D_ID = %s AND C_ID = %s AND O_ID = %s;",
          this.warehouse_id, district_id, customer_id, order_id);

      result = session.execute(getOrderByCustomer);
      row = result.one();

      List<UDTValue> items = row.getList("ITEMS", UDTValue.class);
      double amount = 0;
      for (UDTValue item : items) {
        amount += item.getDecimal("OL_AMOUNT").doubleValue();
      }

      // Update the customers table
      String updateCustomer = String.format(
          "UPDATE CS4224H.customers SET C_BALANCE = C_BALANCE + %f, C_DELIVERY_CNT = C_DELIVERY_CNT + 1 WHERE C_W_ID = %s AND C_D_ID = %s AND C_ID = %s;",
          amount, this.warehouse_id, district_id, customer_id);

      session.execute(updateCustomer);

      // Update orders_by_customer table
      String updateOrdersByCustomer = String.format(
          "UPDATE CS4224H.orders_by_customer SET O_CARRIER_ID = %s, OL_DELIVERY_D =  WHERE C_W_ID = %s AND C_D_ID = %s AND C_ID = %s AND O_ID = %s;",
          this.carrier_id, new Date(), this.warehouse_id, district_id, customer_id, order_id);

      session.execute(updateOrdersByCustomer);
    }
  }
}