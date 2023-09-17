-- Creates the keyspace.
DROP KEYSPACE IF EXISTS CS4224H;
CREATE KEYSPACE IF NOT EXISTS CS4224H WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};
USE CS4224H;

-- Creates the tables.
DROP TYPE IF EXISTS Item;
CREATE TYPE Item (
  OL_I_ID int,
  OL_SUPPLY_W_ID int,
  OL_QUANTITY int,
  OL_AMOUNT decimal
);

-- Transaction 4, 8
DROP TABLE IF EXISTS orders_by_customer;
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

-- Transaction 5, 6
DROP TABLE IF EXISTS orders_by_district;
CREATE TABLE orders_by_district (
	D_W_ID int,
	D_ID int,
	O_ID int,
  O_ENTRY_D int,
  C_FIRST text,
  C_MIDDLE text,
  C_LAST text,
  POPULAR_ITEMS list<frozen<Item>>,
  PRIMARY KEY ((D_W_ID, D_ID), O_ID)
);

-- Transaction 5
DROP TABLE IF EXISTS stocks_by_warehouse;
CREATE TABLE stocks_by_warehouse (
	S_W_ID int,
	S_I_ID int,
	S_QUANTITY decimal, 
  S_YTD decimal,
  S_ORDER_CNT int,
  S_REMOTE_CNT int,
	PRIMARY KEY ((S_W_ID, S_I_ID))
);


DROP TABLE IF EXISTS stocks_by_warehouse;
CREATE TABLE stocks_by_warehouse (
	S_W_ID int,
	S_I_ID int,
	S_QUANTITY decimal, 
  S_YTD decimal,
  S_ORDER_CNT int,
  S_REMOTE_CNT int,
	PRIMARY KEY ((S_W_ID, S_I_ID))
);

-- Transaction 7
DROP TABLE IF EXISTS customers;
CREATE TABLE customers (
	C_ID int,
	C_FIRST text,
	C_MIDDLE text,
	C_LAST text,
  C_BALANCE decimal,
  W_NAME text,
  D_NAME text,
	PRIMARY KEY ((C_FIRST, C_MIDDLE, C_LAST), C_BALANCE)
);


-- Transaction 8
DROP TABLE IF EXISTS customers_by_item;
CREATE TABLE customers_by_item (
  I_ID int,
	C_ID int,
  W_ID int,
	PRIMARY KEY ((I_ID))
);

INSERT INTO orders_by_customer (C_W_ID, C_D_ID, C_ID, O_ID, O_ENTRY_D, O_CARRIER_ID, OL_DELIVERY_D, ITEMS)
VALUES (
  1,  -- C_W_ID value
  1,  -- C_D_ID value
  1,  -- C_ID value
  2001,  -- O_ID value
  '2023-09-13 12:00:00',  -- O_ENTRY_D value
  101,  -- O_CARRIER_ID value
  '2023-09-14 14:30:00',  -- OL_DELIVERY_D value
  [   -- Start of ITEMS list
    {OL_I_ID: 10, OL_SUPPLY_W_ID: 2, OL_QUANTITY: 5, OL_AMOUNT: 50.75},
    {OL_I_ID: 11, OL_SUPPLY_W_ID: 3, OL_QUANTITY: 3, OL_AMOUNT: 30.50}
  ]   -- End of ITEMS list
);