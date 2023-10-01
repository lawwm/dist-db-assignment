\COPY Warehouse FROM 'warehouse.csv' DELIMITER ',' CSV;
\COPY District FROM 'district.csv' DELIMITER ',' CSV;
\COPY Customer FROM 'customer.csv' DELIMITER ',' CSV;
\COPY OrderTable FROM 'order.csv' DELIMITER ',' CSV NULL 'null';
\COPY Item FROM 'item.csv' DELIMITER ',' CSV;
\COPY OrderLine FROM 'order-line.csv' DELIMITER ',' CSV NULL 'null';
\COPY Stock FROM 'stock.csv' DELIMITER ',' CSV;