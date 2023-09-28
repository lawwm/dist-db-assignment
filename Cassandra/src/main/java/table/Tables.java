package table;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.datastax.driver.core.Session;

public class Tables {
  public void runCqlScript(Session session, String scriptPath) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(scriptPath));
    StringBuilder stringBuilder = new StringBuilder();
    char[] buffer = new char[10];
    while (reader.read(buffer) != -1) {
      stringBuilder.append(new String(buffer));
      buffer = new char[10];
    }
    reader.close();
    String cqlContent = stringBuilder.toString();
    String[] cqls = cqlContent.split(";");
    for (String statement : cqls) {
      if (statement.trim().length() > 0) {
        System.out.println("Executing CQL statement: " + statement);
        session.execute(statement);
      }
    }
  }
}