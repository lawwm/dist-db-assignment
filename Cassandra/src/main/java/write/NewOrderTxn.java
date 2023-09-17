package write;

import utils.Transaction;
import com.datastax.driver.core.Session;

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

  public void run(Session session) {

  }
}
