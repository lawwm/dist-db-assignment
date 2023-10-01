package utils;

import com.datastax.driver.core.Session;
import utils.ItemsMetadata;

public interface Transaction {
  void run(Session session, ItemsMetadata itemsMetadata);
}
