package write;

import utils.ItemsMetadata;
import utils.Transaction;

import java.math.BigDecimal;

import com.datastax.driver.core.Session;

public class DeliveryTxn implements Transaction {

  private final String warehouse_id;
  private final String carrier_id;

  public DeliveryTxn(String warehouse_id, String carrier_id) {
    this.warehouse_id = warehouse_id;
    this.carrier_id = carrier_id;
  }

  public void run(Session session, ItemsMetadata itemsMetadata) {

  }
}