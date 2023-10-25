## Compilation

How to compile and run the project

```
.\gradlew shadowJar
java -jar .\build\libs\CassandraProcessor.jar 127.0.0.1 9042 Create
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

On docker, load data files into cassandraDB

```
docker exec -t ecstatic_ellis cqlsh -e "COPY CS4224H.district_by_warehouse (W_ID, D_ID, W_NAME, W_STREET_1, W_STREET_2, W_CITY, W_STATE, W_ZIP, W_TAX, D_NAME, D_STREET_1, D_STREET_2, D_CITY, D_STATE, D_ZIP, D_TAX, D_YTD, D_NEXT_O_ID, D_LAST_UNDELIVERED_O_D) FROM 'container/dir/district_by_warehouse.csv' WITH DELIMITER=',';"

docker exec -t ecstatic_ellis cqlsh -e "COPY CS4224H.orders_by_customer(C_W_ID, C_D_ID, C_ID, O_ID, O_ENTRY_D, O_CARRIER_ID, OL_DELIVERY_D, ITEMS) FROM 'container/dir/orders_by_customer1.csv' WITH DELIMITER=',';"

docker exec -t ecstatic_ellis cqlsh -e "COPY CS4224H.customers_by_order (C_W_ID, C_D_ID, C_ID, O_ID) FROM 'container/dir/customer_by_order.csv' WITH DELIMITER=',';"

docker exec -t ecstatic_ellis cqlsh -e "COPY CS4224H.customers(C_W_ID, C_D_ID, C_ID, C_FIRST, C_MIDDLE, C_LAST, C_STREET_1, C_STREET_2, C_CITY, C_STATE, C_ZIP, C_PHONE, C_SINCE, C_CREDIT, C_CREDIT_LIM, C_DISCOUNT, C_BALANCE, C_YTD_PAYMENT, C_PAYMENT_CNT, C_DELIVERY_CNT, C_DATA, W_NAME, D_NAME, DUMMY_KEY) FROM 'container/dir/customer_balance.csv' WITH DELIMITER=',';"
```

## Slurm

```
srun java -jar ../build/libs/CassandraProcessor.jar 192.168.48.203 9042 Run ../../../project_files/xact_files/shorter.txt


srun java -jar ../build/libs/CassandraProcessor.jar 192.168.48.189 9042 State hello.csv

srun ../../../apache-cassandra-4.1.3/bin/cqlsh 192.168.48.189 -e "select sum(S_QUANTITY), sum(S_YTD), sum(S_ORDER_CNT), sum(S_REMOTE_CNT) from cs4224h.stocks_by_warehouse;"
```


## Initial state

```
SUM(W_YTD) : 3000000.00

SUM(D_YTD) : 3000000.00
SUM(D_NEXT_O_ID) : 300100

SUM(C_BALANCE) : -3000000.00
SUM(C_YTD_PAYMENT) : 3000000
SUM(C_PAYMENT_CNT) : 300000
SUM(C_DELIVERY_CNT) : 0

MAX(O_ID) : 3000
SUM(O_OL_CNT) : 3749856

sum(OL_AMOUNT) : 5882924710.96
sum(OL_QUANTITY) : 18749280

SUM(S_QUANTITY) : 55029630
SUM(S_YTD) : 0.00
SUM(S_ORDER_CNT) : 0 
SUM(S_REMOTE_CNT) : 0
```