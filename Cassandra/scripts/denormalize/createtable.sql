/*
 * Schema definition + Citus distribution.
*/

DROP TABLE if exists Warehouse CASCADE;
CREATE TABLE Warehouse (
  W_ID integer PRIMARY KEY,
  W_NAME varchar(10),
  W_STREET_1 varchar(20),
  W_STREET_2 varchar(20),
  W_CITY varchar(20),
  W_STATE char(2),
  W_ZIP char(9),
  W_TAX decimal(4,4),
  W_YTD decimal(12,2)
);

DROP TABLE if exists District CASCADE;
CREATE TABLE District (
  D_W_ID integer,
  D_ID int,
  D_NAME varchar(10),
  D_STREET_1 varchar(20),
  D_STREET_2 varchar(20),
  D_CITY varchar(20),
  D_STATE char(2),
  D_ZIP char(9),
  D_TAX decimal(4,4),
  D_YTD decimal(12,2),
  D_NEXT_O_ID integer,
  PRIMARY KEY (D_W_ID, D_ID)
);

DROP TABLE if exists Customer CASCADE;
CREATE TABLE Customer (
  C_W_ID integer,
  C_D_ID integer,
  C_ID integer,
  C_FIRST varchar(16),
  C_MIDDLE char(2),
  C_LAST varchar(16),
  C_STREET_1 varchar(20),
  C_STREET_2 varchar(20),
  C_CITY varchar(20),
  C_STATE char(2),
  C_ZIP char(9),
  C_PHONE char(16),
  C_SINCE timestamp,
  C_CREDIT char(2),
  C_CREDIT_LIM decimal(12,2),
  C_DISCOUNT decimal(5,4),
  C_BALANCE decimal(12,2),
  C_YTD_PAYMENT float,
  C_PAYMENT_CNT integer,
  C_DELIVERY_CNT integer,
  C_DATA varchar(500),
  PRIMARY KEY (C_W_ID, C_D_ID, C_ID)
);

DROP TABLE if exists OrderTable CASCADE;
CREATE TABLE OrderTable (
  O_W_ID integer,
  O_D_ID integer,
  O_ID integer,
  O_C_ID integer,
  O_CARRIER_ID integer CHECK (0 <= O_CARRIER_ID AND O_CARRIER_ID <= 10),
  O_OL_CNT decimal(2,0),
  O_ALL_LOCAL decimal(1,0),
  O_ENTRY_D timestamp,
  PRIMARY KEY (O_W_ID, O_D_ID, O_ID)
);

DROP TABLE if exists Item CASCADE;
CREATE TABLE Item (
  I_ID integer PRIMARY KEY,
  I_NAME varchar(24),
  I_PRICE decimal(5,2),
  I_IM_ID int,
  I_DATA varchar(50)
);

DROP TABLE if exists OrderLine CASCADE;
CREATE TABLE OrderLine (
  OL_W_ID integer,
  OL_D_ID integer,
  OL_O_ID integer,
  OL_NUMBER integer,
  OL_I_ID integer,
  OL_DELIVERY_D timestamp,
  OL_AMOUNT decimal(7,2),
  OL_SUPPLY_W_ID integer,
  OL_QUANTITY decimal(2,0),
  OL_DIST_INFO char(24),
  PRIMARY KEY (OL_W_ID, OL_D_ID, OL_O_ID, OL_NUMBER)
);

DROP TABLE if exists Stock CASCADE;
CREATE TABLE Stock (
  S_W_ID integer,
  S_I_ID integer,
  S_QUANTITY decimal(4,0),
  S_YTD decimal(8,2),
  S_ORDER_CNT integer,
  S_REMOTE_CNT integer,
  S_DIST_01 char(24),
  S_DIST_02 char(24),
  S_DIST_03 char(24),
  S_DIST_04 char(24),
  S_DIST_05 char(24),
  S_DIST_06 char(24),
  S_DIST_07 char(24),
  S_DIST_08 char(24),
  S_DIST_09 char(24),
  S_DIST_10 char(24),
  S_DATA varchar(50),
  PRIMARY KEY (S_W_ID, S_I_ID)
);

-- CREATE UNIQUE INDEX ON District (D_W_ID, D_ID);

-- CREATE UNIQUE INDEX ON Customer (C_W_ID, C_D_ID, C_ID);

-- CREATE UNIQUE INDEX ON OrderTable(O_W_ID, O_D_ID, O_C_ID);

-- CREATE UNIQUE INDEX ON OrderLine (OL_W_ID, OL_D_ID, OL_O_ID, OL_NUMBER);

-- CREATE UNIQUE INDEX ON Stock (S_W_ID, S_I_ID);

COMMENT ON COLUMN Warehouse.W_ID IS 'Warehouse number';

COMMENT ON COLUMN Warehouse.W_NAME IS 'Warehouse name';

COMMENT ON COLUMN Warehouse.W_STREET_1 IS 'Warehouse address';

COMMENT ON COLUMN Warehouse.W_STREET_2 IS 'Warehouse address';

COMMENT ON COLUMN Warehouse.W_CITY IS 'Warehouse address';

COMMENT ON COLUMN Warehouse.W_STATE IS 'Warehouse address';

COMMENT ON COLUMN Warehouse.W_ZIP IS 'Warehouse address';

COMMENT ON COLUMN Warehouse.W_TAX IS 'Warehouse sales tax rate';

COMMENT ON COLUMN Warehouse.W_YTD IS 'Year to date amount paid to warehouse';

COMMENT ON COLUMN District.D_W_ID IS 'Warehouse number';

COMMENT ON COLUMN District.D_ID IS 'District number';

COMMENT ON COLUMN District.D_NAME IS 'Distract name';

COMMENT ON COLUMN District.D_STREET_1 IS 'District address';

COMMENT ON COLUMN District.D_STREET_2 IS 'District address';

COMMENT ON COLUMN District.D_CITY IS 'District address';

COMMENT ON COLUMN District.D_STATE IS 'District address';

COMMENT ON COLUMN District.D_ZIP IS 'District address';

COMMENT ON COLUMN District.D_TAX IS 'District sales tax rate';

COMMENT ON COLUMN District.D_YTD IS 'Year to date amount paid to district';

COMMENT ON COLUMN District.D_NEXT_O_ID IS 'Next available OrderTablenumber for district';

COMMENT ON COLUMN Customer.C_W_ID IS 'Warehouse number';

COMMENT ON COLUMN Customer.C_D_ID IS 'District number';

COMMENT ON COLUMN Customer.C_ID IS 'Customer number';

COMMENT ON COLUMN Customer.C_FIRST IS 'Customer name';

COMMENT ON COLUMN Customer.C_MIDDLE IS 'Customer name';

COMMENT ON COLUMN Customer.C_LAST IS 'Customer name';

COMMENT ON COLUMN Customer.C_STREET_1 IS 'Customer address';

COMMENT ON COLUMN Customer.C_STREET_2 IS 'Customer address';

COMMENT ON COLUMN Customer.C_CITY IS 'Customer address';

COMMENT ON COLUMN Customer.C_STATE IS 'Customer address';

COMMENT ON COLUMN Customer.C_ZIP IS 'Customer address';

COMMENT ON COLUMN Customer.C_PHONE IS 'Customer phone';

COMMENT ON COLUMN Customer.C_SINCE IS 'Date and time when entry was created';

COMMENT ON COLUMN Customer.C_CREDIT IS 'Customer credit status';

COMMENT ON COLUMN Customer.C_CREDIT_LIM IS 'Customer credit limit';

COMMENT ON COLUMN Customer.C_DISCOUNT IS 'Customer discount rate';

COMMENT ON COLUMN Customer.C_BALANCE IS 'Balance of customers outstanding payment';

COMMENT ON COLUMN Customer.C_YTD_PAYMENT IS 'Year to date payment by customer';

COMMENT ON COLUMN Customer.C_PAYMENT_CNT IS 'Number of payments made';

COMMENT ON COLUMN Customer.C_DELIVERY_CNT IS 'Number of deliveries made to customer';

COMMENT ON COLUMN Customer.C_DATA IS 'Miscellaneous data';

COMMENT ON COLUMN OrderTable.O_W_ID IS 'Warehouse number';

COMMENT ON COLUMN OrderTable.O_D_ID IS 'District number';

COMMENT ON COLUMN OrderTable.O_ID IS 'OrderTablenumber';

COMMENT ON COLUMN OrderTable.O_C_ID IS 'Customer number';

COMMENT ON COLUMN OrderTable.O_CARRIER_id IS 'Identifier of carrier who delivered the order';

COMMENT ON COLUMN OrderTable.O_OL_CNT IS ' Number of items ordered';

COMMENT ON COLUMN OrderTable.O_ALL_LOCAL IS 'Order status (whether OrderTableincludes only home order-lines)';

COMMENT ON COLUMN OrderTable.O_ENTRY_D IS 'Order entry data and time';

COMMENT ON COLUMN Item.I_ID IS 'Item identifier';

COMMENT ON COLUMN Item.I_NAME IS 'Item name';

COMMENT ON COLUMN Item.I_PRICE IS 'Item price';

COMMENT ON COLUMN Item.I_IM_ID IS 'Item image identifier';

COMMENT ON COLUMN Item.I_DATA IS 'Brand information';

COMMENT ON COLUMN OrderLine.OL_W_ID IS 'Warehouse number';

COMMENT ON COLUMN OrderLine.OL_D_ID IS 'District number';

COMMENT ON COLUMN OrderLine.OL_O_ID IS 'OrderTablenumber';

COMMENT ON COLUMN OrderLine.OL_NUMBER IS 'Order-line number';

COMMENT ON COLUMN OrderLine.OL_I_ID IS 'Item number';

COMMENT ON COLUMN OrderLine.OL_DELIVERY_D IS 'Date and time of delivery';

COMMENT ON COLUMN OrderLine.OL_AMOUNT IS 'Total price for ordered item';

COMMENT ON COLUMN OrderLine.OL_SUPPLY_W_ID IS 'Supplying warehouse number';

COMMENT ON COLUMN OrderLine.OL_QUANTITY IS 'Quantity ordered';

COMMENT ON COLUMN OrderLine.OL_DIST_INFO IS 'Miscellaneous data';

COMMENT ON COLUMN Stock.S_W_ID IS 'Warehouse number';

COMMENT ON COLUMN Stock.S_I_ID IS 'Item number';

COMMENT ON COLUMN Stock.S_QUANTITY IS 'Quantity in stock for item';

COMMENT ON COLUMN Stock.S_YTD IS 'Year to date total quantity ordered';

COMMENT ON COLUMN Stock.S_ORDER_CNT IS 'Number of orders';

COMMENT ON COLUMN Stock.S_REMOTE_CNT IS 'Number of remote orders';

COMMENT ON COLUMN Stock.S_DIST_01 IS 'Information on district 1s stock';

COMMENT ON COLUMN Stock.S_DIST_02 IS 'Information on district 2s stock';

COMMENT ON COLUMN Stock.S_DIST_03 IS 'Information on district 3s stock';

COMMENT ON COLUMN Stock.S_DIST_04 IS 'Information on district 4s stock';

COMMENT ON COLUMN Stock.S_DIST_05 IS 'Information on district 5s stock';

COMMENT ON COLUMN Stock.S_DIST_06 IS 'Information on district 6s stock';

COMMENT ON COLUMN Stock.S_DIST_07 IS 'Information on district 7s stock';

COMMENT ON COLUMN Stock.S_DIST_08 IS 'Information on district 8s stock';

COMMENT ON COLUMN Stock.S_DIST_09 IS 'Information on district 9s stock';

COMMENT ON COLUMN Stock.S_DIST_10 IS 'Information on district 10s stock';

COMMENT ON COLUMN Stock.S_DATA IS 'Miscellaneous data';

ALTER TABLE District ADD FOREIGN KEY (D_W_ID) REFERENCES Warehouse (W_ID) ON DELETE CASCADE;

ALTER TABLE Customer ADD FOREIGN KEY (C_W_ID, C_D_ID) REFERENCES District (D_W_ID, D_ID) ON DELETE CASCADE;

ALTER TABLE OrderTable ADD FOREIGN KEY (O_W_ID, O_D_ID, O_C_ID) REFERENCES Customer (C_W_ID, C_D_ID, C_ID) ON DELETE CASCADE;

ALTER TABLE OrderLine ADD FOREIGN KEY (OL_I_ID) REFERENCES Item (I_ID) ON DELETE CASCADE;

ALTER TABLE OrderLine ADD FOREIGN KEY (OL_W_ID, OL_D_ID, OL_O_ID) REFERENCES OrderTable (O_W_ID, O_D_ID, O_ID) ON DELETE CASCADE;

ALTER TABLE Stock ADD FOREIGN KEY (S_I_ID) REFERENCES Item (I_ID) ON DELETE CASCADE;

ALTER TABLE Stock ADD FOREIGN KEY (S_W_ID) REFERENCES Warehouse (W_ID) ON DELETE CASCADE;
