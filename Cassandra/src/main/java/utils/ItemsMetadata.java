package utils;

import java.util.Map;

import com.fasterxml.jackson.databind.util.TypeKey;

import java.util.HashMap;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;

public class ItemsMetadata {
  private Map<Integer, BigDecimal> itemPrices = new HashMap<>();
  private Map<Integer, String> itemNames = new HashMap<>();

  public BigDecimal getItemPrice(int itemId) {
    return itemPrices.get(itemId);
  }

  public String getItemName(int itemId) {
    return itemNames.get(itemId);
  }

  public void populate(String path) throws FileNotFoundException, IOException {
    BufferedReader reader = new BufferedReader(new FileReader(path));
    String line;
    while ((line = reader.readLine()) != null) {
      String[] tokens = line.split(",");
      itemPrices.put(Integer.parseInt(tokens[0]), new BigDecimal(tokens[2]));
      itemNames.put(Integer.parseInt(tokens[0]), tokens[1]);
    }
    reader.close();
  }
}
