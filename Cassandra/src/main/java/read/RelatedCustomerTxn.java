package read;

import utils.Transaction;
import com.datastax.driver.core.Session;

public class RelatedCustomerTxn implements Transaction {

  private final String warehouse_id;
  private final String district_id;
  private final String customer_id;

  public RelatedCustomerTxn(String warehouse_id, String district_id, String customer_id) {
    this.warehouse_id = warehouse_id;
    this.district_id = district_id;
    this.customer_id = customer_id;
  }

  public void run(Session session) {

  }
}