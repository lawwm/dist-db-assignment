## Compilation

How to compile and run the project

```
.\gradlew shadowJar
java -jar .\build\libs\CassandraProcessor.jar 127.0.0.1 9042
```

## Denormalize data files

Data is located in `Cassandra/scripts/denormalize` folder.

1. Start postgresql container

```
docker run --name some-postgres -v H:\Desktop\NUS\Y4S1\CS4224\projects\files\project_files\data_files:/container/dir -e POSTGRES_PASSWORD=password -d postgres
```

2. enter via bash

```
docker exec -it some-postgres bash
```

3.  Enter directory

```
cd container/dir
```

4. Running script

```
cat createtable.sql | psql -U postgres -d postgres
cat populate.sql | psql -U postgres -d postgres
cat denormalize.sql | psql -U postgres -d postgres
```

## Run Cassandra on Docker

Start cassandra container

```
docker run -v H:\Desktop\NUS\Y4S1\CS4224\projects\cs4224-ay2324-2-TeamH\Cassandra\scripts\data:/container/dir -p 9042:9042 cassandra:latest
```

## Load data into cassandraDB

On docker

```
docker exec -t ecstatic_ellis cqlsh -e "COPY CS4224H.orders_by_customer(C_W_ID, C_D_ID, C_ID, O_ID, O_ENTRY_D, O_CARRIER_ID, OL_DELIVERY_D, ITEMS) FROM 'container/dir/orders_by_customer1.csv' WITH DELIMITER=',';"

docker exec -t ecstatic_ellis cqlsh -e "COPY CS4224H.customers(C_W_ID, C_D_ID, C_ID, C_FIRST, C_MIDDLE, C_LAST, C_BALANCE, W_NAME, D_NAME) FROM 'container/dir/customer_balance.csv' WITH DELIMITER=',';"

docker exec -t ecstatic_ellis cqlsh -e "COPY CS4224H.district_by_warehouse (W_ID, D_ID, W_NAME, W_STREET_1, W_STREET_2, W_CITY, W_STATE, W_ZIP, W_TAX, D_NAME, D_STREET_1, D_STREET_2, D_CITY, D_STATE, D_ZIP, D_TAX, D_YTD, D_NEXT_O_ID, D_LAST_UNDELIVERED_O_D) FROM 'container/dir/district_by_warehouse.csv' WITH DELIMITER=',';"

docker exec -t ecstatic_ellis cqlsh -e "CREATE TABLE CS4224H.district_by_warehouse (
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
);"

docker exec -t ecstatic_ellis cqlsh -e "DROP TABLE IF EXISTS CS4224H.customers;
CREATE TABLE CS4224H.customers (
  C_W_ID int,
	C_D_ID int,
	C_ID int,
	C_FIRST text,
	C_MIDDLE text,
	C_LAST text,
  C_BALANCE decimal,
  W_NAME text,
  D_NAME text,
	PRIMARY KEY ((C_W_ID, C_D_ID, C_ID), C_BALANCE)
);"

```
