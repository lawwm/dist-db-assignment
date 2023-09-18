package read;

import utils.Transaction;

import java.util.HashSet;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

public class StockLevelTxn implements Transaction {

  private static final String GET_ORDER_BY_DISTRICT = "SELECT * FROM orders_by_district WHERE D_W_ID = %d AND D_ID = %d LIMIT %d";

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

  public void run(Session session) {
    String query = String.format(GET_ORDER_BY_DISTRICT, Integer.parseInt(warehouse_id),
        Integer.parseInt(district_id), Integer.parseInt(last_l));

    ResultSet rs = session.execute(query);

    Set<Integer> items_below_stock_level = new HashSet<>();

    for (Row row : rs) {

    }

  }
}