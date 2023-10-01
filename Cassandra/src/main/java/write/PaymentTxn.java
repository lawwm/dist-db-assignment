package write;

import utils.ItemsMetadata;
import utils.Transaction;

import java.math.BigDecimal;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class PaymentTxn implements Transaction {

    private final String warehouse_id;
    private final String district_id;
    private final String customer_id;
    private final BigDecimal payment;

    public PaymentTxn(String warehouse_id, String district_id, String customer_id, String payment) {
        this.warehouse_id = warehouse_id;
        this.district_id = district_id;
        this.customer_id = customer_id;
        this.payment = new BigDecimal(payment);
    }

    public void run(Session session, ItemsMetadata itemsMetadata) {
        // Get customer
        String getCustomerQuery = String.format(
                "SELECT * FROM CS4224H.customers WHERE C_W_ID = %s AND C_D_ID = %s AND C_ID = %s;",
                this.warehouse_id, this.district_id, this.customer_id);
        String getDistrictQuery = String.format(
                "SELECT * FROM CS4224H.district_by_warehouse WHERE W_ID = %s AND D_ID = %s;",
                this.warehouse_id, this.district_id);

        ResultSet customerResult = session.execute(getCustomerQuery);
        ResultSet districtResult = session.execute(getDistrictQuery);
        Row customer = customerResult.one();
        Row district = districtResult.one();

        double new_d_ytd = district.getDecimal("D_YTD").doubleValue() + this.payment.doubleValue();
        double new_c_balance = customer.getDecimal("C_BALANCE").doubleValue() - this.payment.doubleValue();
        double new_c_ytd_payment = customer.getDecimal("C_YTD_PAYMENT").doubleValue() + this.payment.doubleValue();
        int new_c_payment_cnt = customer.getInt("C_PAYMENT_CNT") + 1;
        // update district warehouse
        String updateDistrictWarehouse = String.format(
                "UPDATE CS4224H.district_by_warehouse SET D_YTD = %f WHERE W_ID = %s AND D_ID = %s;",
                new_d_ytd, this.warehouse_id, this.district_id);

        // update customer
        String updateCustomer = String.format(
                "UPDATE CS4224H.customers SET C_BALANCE = %f, C_YTD_PAYMENT = %f, C_PAYMENT_CNT = %d WHERE C_W_ID = %s AND C_D_ID = %s AND C_ID = %s;",
                new_c_balance,
                new_c_ytd_payment,
                new_c_payment_cnt,
                this.warehouse_id, this.district_id,
                this.customer_id);

        session.execute(updateDistrictWarehouse);
        session.execute(updateCustomer);

        // Customer’s identifier (C W ID, C D ID, C ID), name (C FIRST, C MIDDLE, C
        // LAST), address
        // (C STREET 1, C STREET 2, C CITY, C STATE, C ZIP), C PHONE, C SINCE, C CREDIT,
        // C CREDIT LIM, C DISCOUNT, C BALANCE
        System.out.printf(
                "1. Customer's identifier: %s %s %s, Customer's name: %s %s %s, Customer's address: %s %s %s %s %s, C_PHONE: %s, C_SINCE: %s, C_CREDIT: %s, C_CREDIT_LIM: %.2f, C_DISCOUNT: %.2f, C_BALANCE: %.2f\n",
                this.warehouse_id,
                this.district_id, this.customer_id, customer.getString("C_FIRST"), customer.getString("C_MIDDLE"),
                customer.getString("C_LAST"), customer.getString("C_STREET_1"), customer.getString("C_STREET_2"),
                customer.getString("C_CITY"), customer.getString("C_STATE"), customer.getString("C_ZIP"),
                customer.getString("C_PHONE"), customer.getTimestamp("C_SINCE"), customer.getString("C_CREDIT"),
                customer.getDecimal("C_CREDIT_LIM"), customer.getDecimal("C_DISCOUNT"),
                customer.getDecimal("C_BALANCE"));

        // 2. Warehouse’s address (W STREET 1, W STREET 2, W CITY, W STATE, W ZIP)
        System.out.printf("2. Warehouse's address: %s %s %s %s %s\n", district.getString("D_STREET_1"),
                district.getString("D_STREET_2"),
                district.getString("D_CITY"), district.getString("D_STATE"), district.getString("D_ZIP"));

        // 3. District’s address (D STREET 1, D STREET 2, D CITY, D STATE, D ZIP)
        System.out.printf("3. District's address: %s %s %s %s %s\n", district.getString("D_STREET_1"),
                district.getString("D_STREET_2"),
                district.getString("D_CITY"), district.getString("D_STATE"), district.getString("D_ZIP"));

        // 4. Payment amount PAYMENT
        System.out.printf("4. Payment amount: %.2f\n", this.payment);
    }
}