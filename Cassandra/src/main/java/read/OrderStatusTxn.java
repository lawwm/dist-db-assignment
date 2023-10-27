package read;

import utils.ItemsMetadata;
import utils.Transaction;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.Row;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class OrderStatusTxn implements Transaction {
	private static final String GET_ORDER_BY_CUSTOMER = "SELECT * FROM CS4224H.orders_by_customer WHERE C_W_ID = %s AND C_D_ID = %s AND C_ID = %s ORDER BY O_ID DESC LIMIT 1";
	private static final String GET_CUSTOMER = "SELECT * FROM CS4224H.customers WHERE DUMMY_KEY = 1 AND C_W_ID=%s AND C_D_ID=%s AND C_ID = %s;";

	private final String warehouse_id;
	private final String district_id;
	private final String customer_id;

	public OrderStatusTxn(String warehouse_id, String district_id, String customer_id) {
		this.warehouse_id = warehouse_id;
		this.district_id = district_id;
		this.customer_id = customer_id;
	}

	public void run(Session session, ItemsMetadata itemsMetadata) {
		String getCustomerLastOrderQuery = String.format(GET_ORDER_BY_CUSTOMER, warehouse_id, district_id, customer_id);
		String getCustomerQuery = String.format(GET_CUSTOMER, warehouse_id, district_id, customer_id);

		// Get customer's name
		ResultSet customerResult = session.execute(getCustomerQuery);
		ResultSet customerOrderResult = session.execute(getCustomerLastOrderQuery);
		Row customerRow = customerResult.one();
		Row customerOrder = customerOrderResult.one();
		if (customerRow == null || customerOrder == null) {
			System.out.printf("Customer %s not found\n", customer_id);
			return;
		}
		int orderId = customerOrder.getInt("O_ID");
		String getOrder = String.format("SELECT * FROM CS4224H.orders_by_district WHERE D_W_ID = %s AND D_ID = %s AND O_ID = %d;", warehouse_id, district_id, orderId);
		ResultSet orderResult = session.execute(getOrder);
		Row order = orderResult.one();
		if (order == null) {
			System.out.printf("No orders found for customer %s\n", customer_id);
			return;
		}

		// Customer's name
		String c_first = customerRow.getString("C_FIRST");
		String c_middle = customerRow.getString("C_MIDDLE");
		String c_last = customerRow.getString("C_LAST");
		String c_balance = customerRow.getDecimal("C_BALANCE").toString();
		System.out.printf("1. Customer name: %s %s %s %s\n", c_first, c_middle, c_last, c_balance);

		// Get Customer's Last Order
		int o_id = order.getInt("O_ID");
		Integer o_carrier_id_Integer = order.getInt("O_CARRIER_ID");
		Date o_entry_d_Date = order.getTimestamp("O_ENTRY_D");
		Date ol_delivery_d_Date = order.getTimestamp("OL_DELIVERY_D");

		String o_entry_d = o_entry_d_Date == null ? "NULL" : o_entry_d_Date.toString();
		String ol_delivery_d = ol_delivery_d_Date == null ? "NULL" : ol_delivery_d_Date.toString();
		String o_carrier_id = o_carrier_id_Integer == 0 ? "NULL" : o_carrier_id_Integer.toString();
		System.out.printf("2. O_ID: %s O_ENTRY_D: %s O_CARRIER_ID: %s\n", o_id, o_entry_d, o_carrier_id);

		// Get each Item in customer's last order
		List<UDTValue> items = order.getList("ITEMS", UDTValue.class);
		for (var item : items) {
			int ol_i_id = item.getInt("OL_I_ID");
			int ol_supply_w_id = item.getInt("OL_SUPPLY_W_ID");
			int ol_quantity = item.getInt("OL_QUANTITY");
			BigDecimal ol_amount = item.getDecimal("OL_AMOUNT");
			System.out.printf("3. OL_I_ID: %d OL_SUPPLY_W_ID: %d OL_QUANTITY: %d OL_AMOUNT: %s OL_DELIVERY_D %s\n",
					ol_i_id,
					ol_supply_w_id,
					ol_quantity, ol_amount.toString(), ol_delivery_d);
		}
	}
}
