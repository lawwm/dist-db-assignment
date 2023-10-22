-- Populate data

COPY CS4224H.district_by_warehouse (W_ID, D_ID, W_NAME, W_STREET_1, W_STREET_2, W_CITY, W_STATE, W_ZIP, W_TAX, D_NAME, D_STREET_1, D_STREET_2, D_CITY, D_STATE, D_ZIP, D_TAX, D_YTD, D_NEXT_O_ID, D_LAST_UNDELIVERED_O_D)
FROM 'project_files/data_files/district_by_warehouse.csv' WITH DELIMITER=',';

COPY CS4224H.customers_by_order (C_W_ID, C_D_ID, C_ID, O_ID)
FROM 'project_files/data_files/customer_by_order.csv' WITH DELIMITER=',';

COPY CS4224H.customers(C_W_ID, C_D_ID, C_ID, C_FIRST, C_MIDDLE, C_LAST, C_STREET_1, C_STREET_2, C_CITY, C_STATE, C_ZIP, C_PHONE, C_SINCE, C_CREDIT, C_CREDIT_LIM, C_DISCOUNT, C_BALANCE, C_YTD_PAYMENT, C_PAYMENT_CNT, C_DELIVERY_CNT, C_DATA, W_NAME, D_NAME, DUMMY_KEY)
FROM 'project_files/data_files/customer_balance.csv' WITH DELIMITER=',';

COPY CS4224H.orders_by_district(D_W_ID, D_ID, O_ID, O_ENTRY_D, C_FIRST, C_MIDDLE, C_LAST, POPULAR_ITEMS)
FROM 'project_files/data_files/orders_by_district.csv' WITH DELIMITER=',';

COPY CS4224H.orders_by_customer(C_W_ID, C_D_ID, C_ID, O_ID, O_ENTRY_D, O_CARRIER_ID, OL_DELIVERY_D, ITEMS)
FROM 'project_files/data_files/orders_by_customer.csv' WITH DELIMITER=',';

COPY CS4224H.stocks_by_warehouse(S_W_ID, S_I_ID, S_QUANTITY)
FROM 'project_files/data_files/stocks_by_warehouse.csv' WITH DELIMITER=',';

COPY CS4224H.customer_item_denorm(C_W_ID, C_D_ID, C_ID, OL_I_ID)
FROM 'project_files/data_files/customer_item_denorm.csv' WITH DELIMITER=',';
