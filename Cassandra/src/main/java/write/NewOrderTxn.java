package write;

import utils.Transaction;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

public class NewOrderTxn implements Transaction {

  private final String warehouse_id;
  private final String district_id;
  private final String customer_id;
  private final String[][] items;
  private final String UPDATE_CUSTOMER_DENORM_QUERY = "Insert into customer_denorm " +
          "(C_W_ID, C_D_ID, C_ID) values (?, ?, ?);";
  private final String UPDATE_CUSTOMER_ITEM_DENORM_QUERY = "Insert into customer_item_denorm " +
          "(C_W_ID, C_D_ID, C_ID, OL_I_ID) values (?, ?, ?, ?);";

  public NewOrderTxn(String customer_id, String district_id, String warehouse_id, String[][] items) {
    this.warehouse_id = warehouse_id;
    this.district_id = district_id;
    this.customer_id = customer_id;
    this.items = items;
  }

  public void run(Session session) {
    int w_id = Integer.parseInt(this.warehouse_id);
    int d_id = Integer.parseInt(this.district_id);
    int c_id = Integer.parseInt(this.customer_id);
    PreparedStatement prepareCustDenormInsertQuery = session.prepare(UPDATE_CUSTOMER_DENORM_QUERY);
    BoundStatement custDenormInsertQuery = prepareCustDenormInsertQuery.bind(w_id, d_id, c_id);
    session.execute(custDenormInsertQuery);
    PreparedStatement prepareCustItemDenormInsertQuery = session.prepare(UPDATE_CUSTOMER_ITEM_DENORM_QUERY);
    for (String[] currItem : items) {
      int ol_i_id = Integer.parseInt(currItem[0]);
      BoundStatement orderDenormInsertQuery = prepareCustItemDenormInsertQuery.bind(
              w_id, d_id, c_id, ol_i_id);
      session.execute(orderDenormInsertQuery);
    }
  }
}
