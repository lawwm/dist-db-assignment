package read;

import utils.ItemsMetadata;
import utils.Transaction;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

public class TopBalanceTxn implements Transaction {

  public TopBalanceTxn() {
  }

  public void run(Session session, ItemsMetadata itemsMetadata) {
    String getTopTen = "SELECT * FROM CS4224H.customers_by_balance WHERE DUMMY_KEY = 1 ORDER BY C_BALANCE DESC LIMIT 10;";
    ResultSet result = session.execute(getTopTen);
    for (Row row : result.all()) {
      System.out.printf("Customer name: %s %s %s, balance: %.2f, warehouse: %s, district: %s\n",
          row.getString("C_FIRST"),
          row.getString("C_MIDDLE"), row.getString("C_LAST"),
          row.getDecimal("C_BALANCE").doubleValue(), row.getString("W_NAME"), row.getString("D_NAME"));
    }
  }
}