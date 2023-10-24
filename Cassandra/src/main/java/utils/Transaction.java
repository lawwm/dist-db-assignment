package utils;

import com.datastax.driver.core.Session;

public interface Transaction {
  void run(Session session, ItemsMetadata itemsMetadata);
}
