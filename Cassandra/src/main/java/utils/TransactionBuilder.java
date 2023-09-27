package utils;

import java.io.BufferedReader;
import java.io.IOException;

import write.NewOrderTxn;
import write.PaymentTxn;
import write.DeliveryTxn;

import read.OrderStatusTxn;
import read.PopularItemTxn;
import read.TopBalanceTxn;
import read.RelatedCustomerTxn;
import read.StockLevelTxn;

public class TransactionBuilder {
  public Transaction build(BufferedReader reader, String row) throws IOException {
    String[] tokens = row.split(",");
    // write a switch statement
    Transaction txn = null;
    switch (tokens[0]) {
      case "N":
        String[][] items = buildItems(reader, Integer.parseInt(tokens[4]));
        txn = new NewOrderTxn(tokens[1], tokens[2], tokens[3], items);
        break;
      case "P":
        txn = new PaymentTxn(tokens[1], tokens[2], tokens[3], tokens[4]);
        break;
      case "D":
        txn = new DeliveryTxn(tokens[1], tokens[2]);
        break;
      case "O":
        txn = new OrderStatusTxn(tokens[1], tokens[2], tokens[3]);
        break;
      case "S":
        txn = new StockLevelTxn(tokens[1], tokens[2], tokens[3], tokens[4]);
        break;
      case "I":
        txn = new PopularItemTxn(tokens[1], tokens[2], tokens[3]);
        break;
      case "T":
        txn = new TopBalanceTxn();
        break;
      case "R":
        txn = new RelatedCustomerTxn(tokens[1], tokens[2], tokens[3]);
        break;
      default:
        throw new IllegalArgumentException("Invalid transaction type: " + tokens[0]);
    }
    return txn;
  }

  public String[][] buildItems(BufferedReader reader, int rows) throws IOException {
    String[][] items = new String[rows][3];
    for (int i = 0; i < rows; i++) {
      String row = reader.readLine();
      String[] tokens = row.split(",");
      items[i][0] = tokens[0];
      items[i][1] = tokens[1];
      items[i][2] = tokens[2];
    }
    return items;
  }
}
