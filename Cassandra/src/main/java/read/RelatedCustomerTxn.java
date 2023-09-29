package read;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import utils.Transaction;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class RelatedCustomerTxn implements Transaction {

  private final String warehouse_id;
  private final String district_id;
  private final String customer_id;
  private final String ALL_CUSTOMERS_QUERY = "Select * from customer_denorm;";
  private final String ALL_ITEMS_QUERY = "Select ol_i_id from customer_item_denorm where " +
          "c_w_id = ? and c_d_id = ? and c_id = ?;";
  private final String CREATE_TEMP_ITEM_ID_INDEX_QUERY = "create index temp on customer_item_denorm (ol_i_id);";
  private final String COUNT_ITEMS_QUERY = "Select c_w_id, c_d_id, c_id, COUNT(ol_i_id) as ol_i_id_count " +
          "from customer_item_denorm where ol_i_id in ? " +
          "group by c_w_id, c_d_id, c_id ALLOW FILTERING;";

  public RelatedCustomerTxn(String warehouse_id, String district_id, String customer_id) {
    this.warehouse_id = warehouse_id;
    this.district_id = district_id;
    this.customer_id = customer_id;
  }

  public void run(Session session) {
    // Two customers, with diff warehouse, for their own order, they have 2 items in common
    int w_id = Integer.parseInt(this.warehouse_id);
    int d_id = Integer.parseInt(this.district_id);
    int c_id = Integer.parseInt(this.customer_id);
    Set<Integer> refDistItemsSet = getDistItemSet(session, w_id, d_id, c_id);
    ResultSet allCustomers = session.execute(ALL_CUSTOMERS_QUERY);
    Set<Row> possibleCustomers = new HashSet<>();
    for (Row currCustRow : allCustomers) {
      int currWId = currCustRow.getInt("c_w_id");
      boolean isDesiredWarehouse = currWId != w_id;
      if (isDesiredWarehouse) {
        possibleCustomers.add(currCustRow);
      }
    }
    session.execute(CREATE_TEMP_ITEM_ID_INDEX_QUERY);
    PreparedStatement prepareCountItemQuery = session.prepare(COUNT_ITEMS_QUERY);
    ArrayList<Integer> refDistItemsList = new ArrayList<>(refDistItemsSet);
    BoundStatement countItemQuery = prepareCountItemQuery.bind(refDistItemsList);
    ResultSet itemCountRes = session.execute(countItemQuery);
    for (Row currItemCountRow : itemCountRes) {
      long currItemCount = currItemCountRow.getLong("ol_i_id_count");
      if (currItemCount >= 2) {
        int c_w_id = currItemCountRow.getInt("c_w_id");
        int c_d_id = currItemCountRow.getInt("c_d_id");
        int c_c_id = currItemCountRow.getInt("c_id");
        System.out.println(c_w_id + " " + c_d_id + " " + c_c_id + "  ROW: " + currItemCountRow.toString());
      }
    }
  }

  private Set<Integer> getDistItemSet(Session session, int w_id, int d_id, int c_id) {
    Set<Integer> result = new HashSet<>();
    PreparedStatement prepareDistItemsQuery = session.prepare(ALL_ITEMS_QUERY);
    BoundStatement distItemsQuery = prepareDistItemsQuery.bind(w_id, d_id, c_id);
    ResultSet distItems = session.execute(distItemsQuery);
    for (Row currItemRow : distItems) {
      int currItemId = currItemRow.getInt("ol_i_id");
      result.add(currItemId);
    }
    return result;
  }
}
