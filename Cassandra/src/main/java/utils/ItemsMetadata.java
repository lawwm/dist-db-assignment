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

  public BigDecimal getItemPrice(int itemId) {
    return itemPrices.get(itemId);
  }

  public void populate(String path) throws FileNotFoundException, IOException {
    BufferedReader reader = new BufferedReader(new FileReader(path));
    String line;
    while ((line = reader.readLine()) != null) {
      String[] tokens = line.split(",");
      itemPrices.put(Integer.parseInt(tokens[0]), new BigDecimal(tokens[2]));
    }
    reader.close();
  }
}
