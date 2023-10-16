DROP KEYSPACE IF EXISTS CS4224H;
CREATE KEYSPACE IF NOT EXISTS CS4224H
WITH replication = {'class': 'NetworkTopologyStrategy', 'replication_factor': 3};

USE CS4224H;

-- Creates the tables.
DROP TYPE IF EXISTS CS4224H.Item;
CREATE TYPE Item (
  OL_I_ID int,
  OL_SUPPLY_W_ID int,
  OL_QUANTITY int,
  OL_AMOUNT decimal
);

DROP TYPE IF EXISTS CS4224H.Item_Type;
CREATE TYPE Item_Type (
  OL_I_ID int,
  I_NAME text,
  OL_SUPPLY_W_ID int,
  OL_QUANTITY int,
  OL_AMOUNT decimal,
);

-- Transaction 4, 7
DROP TABLE IF EXISTS CS4224H.district_by_warehouse;
CREATE TABLE district_by_warehouse (
  W_ID int,
  D_ID int,
  W_NAME text,
  W_STREET_1 text,
  W_STREET_2 text,
  W_CITY text,
  W_STATE text,
  W_ZIP text,
  W_TAX decimal,
  D_NAME text,
  D_STREET_1 text,
  D_STREET_2 text,
  D_CITY text,
  D_STATE text,
  D_ZIP text,
  D_TAX decimal,
  D_YTD decimal,
  D_NEXT_O_ID int,
  D_LAST_UNDELIVERED_O_D int,
  PRIMARY KEY ((W_ID, D_ID))
);

DROP TABLE IF EXISTS CS4224H.orders_by_customer;
CREATE TABLE orders_by_customer (
	C_W_ID int,
	C_D_ID int,
	C_ID int,
  O_ID int,
  O_ENTRY_D TIMESTAMP,
  O_CARRIER_ID INT,
  OL_DELIVERY_D TIMESTAMP,
  ITEMS list<frozen<Item>>,
  PRIMARY KEY ((C_W_ID, C_D_ID, C_ID), O_ID)
);

DROP TABLE IF EXISTS CS4224H.customers_by_order;
CREATE TABLE customers_by_order (
  C_W_ID int,
  C_D_ID int,
  C_ID int,
  O_ID int,
  PRIMARY KEY ((C_W_ID, C_D_ID, O_ID))
);

-- Transaction 7
DROP TABLE IF EXISTS customers;
CREATE TABLE customers (
  C_W_ID int,
	C_D_ID int,
	C_ID int,
	C_FIRST text,
	C_MIDDLE text,
	C_LAST text,
  C_STREET_1 text,
  C_STREET_2 text,
  C_CITY text,
  C_STATE text,
  C_ZIP text,
  C_PHONE text,
  C_SINCE TIMESTAMP,
  C_CREDIT text,
  C_CREDIT_LIM decimal,
  C_DISCOUNT decimal,
  C_BALANCE decimal,
  C_YTD_PAYMENT decimal,
  C_PAYMENT_CNT int,
  C_DELIVERY_CNT int,
  C_DATA text,
  W_NAME text,
  D_NAME text,
  DUMMY_KEY int,
	PRIMARY KEY ((DUMMY_KEY, C_W_ID, C_D_ID), C_ID)
);

DROP MATERIALIZED VIEW IF EXISTS CS4224H.customers_by_balance;
CREATE MATERIALIZED VIEW CS4224H.customers_by_balance
AS SELECT C_W_ID, C_D_ID, C_ID, C_FIRST, C_MIDDLE, C_LAST, C_BALANCE, W_NAME, D_NAME, DUMMY_KEY
FROM customers
WHERE C_W_ID IS NOT NULL AND C_D_ID IS NOT NULL AND C_ID IS NOT NULL AND C_BALANCE IS NOT NULL AND DUMMY_KEY IS NOT NULL
PRIMARY KEY ((DUMMY_KEY), C_BALANCE, C_W_ID, C_D_ID, C_ID);

-- Transaction 5, 6
DROP TABLE IF EXISTS CS4224H.orders_by_district;
CREATE TABLE orders_by_district (
	D_W_ID int,
	D_ID int,
	O_ID int,
  O_ENTRY_D TIMESTAMP,
  C_FIRST text,
  C_MIDDLE text,
  C_LAST text,
  POPULAR_ITEMS list<frozen<Item_Type>>,
  PRIMARY KEY ((D_W_ID, D_ID), O_ID)
) WITH CLUSTERING ORDER BY (O_ID DESC);

-- Transaction 5
DROP TABLE IF EXISTS CS4224H.stocks_by_warehouse;
CREATE TABLE stocks_by_warehouse (
	S_W_ID int,
	S_I_ID int,
	S_QUANTITY decimal,
  S_YTD decimal,
  S_ORDER_CNT int,
  S_REMOTE_CNT int,
	PRIMARY KEY ((S_W_ID, S_I_ID))
);
/*
  S_DIST_1 text,
  S_DIST_2 text,
  S_DIST_3 text,
  S_DIST_4 text,
  S_DIST_5 text,
  S_DIST_6 text,
  S_DIST_7 text,
  S_DIST_8 text,
  S_DIST_9 text,
  S_DIST_10 text,
  S_DATA text,
*/
-- Transaction 8
DROP TABLE IF EXISTS CS4224H.customer_denorm;
CREATE TABLE customer_denorm (
    C_W_ID int,
    C_D_ID int,
    C_ID int,
    PRIMARY KEY (C_W_ID, C_D_ID, C_ID)
);

DROP TABLE IF EXISTS CS4224H.customer_item_denorm;
CREATE TABLE customer_item_denorm (
    C_W_ID int,
    C_D_ID int,
    C_ID int,
    OL_I_ID int,
    PRIMARY KEY ((C_W_ID, C_D_ID, C_ID), OL_I_ID)
);
