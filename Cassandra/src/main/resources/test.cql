USE CS4224H;

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

COPY CS4224H.district_by_warehouse (W_ID, D_ID, W_NAME, W_STREET_1, W_STREET_2, W_CITY, W_STATE, W_ZIP, W_TAX, D_NAME, D_STREET_1, D_STREET_2, D_CITY, D_STATE, D_ZIP, D_TAX, D_YTD, D_NEXT_O_ID, D_LAST_UNDELIVERED_O_D) 
FROM '../scripts/data/district_by_warehouse.csv' WITH DELIMITER=',';