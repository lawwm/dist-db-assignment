package read;

import utils.ItemsMetadata;
import utils.Transaction;

import com.datastax.driver.core.Session;

public class PopularItemTxn implements Transaction {

  private final String warehouse_id;
  private final String district_id;
  private final int last_l;

  public PopularItemTxn(String warehouse_id, String district_id, String last_l) {
    this.warehouse_id = warehouse_id;
    this.district_id = district_id;
    this.last_l = Integer.parseInt(last_l);
  }

  public void run(Session session, ItemsMetadata itemsMetadata) {

  }
}