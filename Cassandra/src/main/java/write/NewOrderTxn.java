package write;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.math.BigDecimal;

import utils.ItemsMetadata;
import utils.Transaction;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.UserType;

public class NewOrderTxn implements Transaction {

  private final String warehouse_id;
  private final String district_id;
  private final String customer_id;
  private final String[][] items;

  public NewOrderTxn(String warehouse_id, String district_id, String customer_id, String[][] items) {
    this.warehouse_id = warehouse_id;
    this.district_id = district_id;
    this.customer_id = customer_id;
    this.items = items;
  }

  public void run(Session session, ItemsMetadata itemsMetadata) {
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
        Integer.parseInt(customer_id), 101, new Date(), null, null, udtItems);

    session.execute(bound);
    // transaction 5

  }
}
