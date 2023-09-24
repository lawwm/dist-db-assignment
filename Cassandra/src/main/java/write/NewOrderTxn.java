package write;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.math.BigDecimal;

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
    Row row = result.one();
    int order_id = row.getInt("D_NEXT_O_ID");
    Statement updateStatement = new SimpleStatement(
        String.format(UPDATE_NEXT_ORDER_ID, order_id + 1, this.warehouse_id, this.district_id));
    session.execute(updateStatement);

    // Atomic compare and swap. Do not removed, tbc for 5 cluster. Unable to test
    // locally due to insufficient replicas.
    // while (order_id == -1) {
    // System.out.printf("Running CAS %d", order_id);
    // ResultSet result = session
    // .execute(getNextOrderIdQuery);
    // Row row = result.one();
    // if (row == null) {
    // System.out.println("UH OH");
    // continue;
    // }
    // int new_order_id = row.getInt("D_NEXT_O_ID");
    // Statement updateStatement = new
    // SimpleStatement(String.format(UPDATE_NEXT_ORDER_ID, new_order_id + 1,
    // this.warehouse_id, this.district_id, new_order_id));
    // updateStatement.setConsistencyLevel(ConsistencyLevel.ONE);
    // var lwt = session
    // .execute(updateStatement);
    // if (lwt.wasApplied()) {
    // order_id = new_order_id;
    // }
    // }

    // transaction 4
    List<UDTValue> udtItems = new ArrayList<>();

    UserType itemType = session.getCluster().getMetadata().getKeyspace("CS4224H").getUserType("Item");
    for (int i = 0; i < items.length; i++) {
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
    BoundStatement bound = ps.bind(Integer.parseInt(warehouse_id), Integer.parseInt(district_id),
        Integer.parseInt(customer_id), order_id, new Date(), null, null, udtItems);

    session.execute(bound);
    // transaction 5

  }
}
