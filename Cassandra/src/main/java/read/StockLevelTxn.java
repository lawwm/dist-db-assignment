package read;

import utils.ItemsMetadata;
import utils.Transaction;

import com.datastax.driver.core.Session;

public class StockLevelTxn implements Transaction {

  private final String warehouse_id;
  private final String district_id;
  private final int stock_level;
  private final int last_l;

  public StockLevelTxn(String warehouse_id, String district_id, String stock_level, String last_l) {
    this.warehouse_id = warehouse_id;
    this.district_id = district_id;
    this.stock_level = Integer.parseInt(stock_level);
    this.last_l = Integer.parseInt(last_l);
  }

  public void run(Session session, ItemsMetadata itemsMetadata) {

  }
}