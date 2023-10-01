
-- -- Transaction 4 denormalization
-- COPY (SELECT C.C_W_ID, C.C_D_ID, C.C_ID, O.O_ID, O.O_ENTRY_D, O.O_CARRIER_ID, OL.OL_DELIVERY_D,
--     '[' || string_agg(
--               '{ol_i_id: ''' || OL.OL_I_ID || ''', ol_supply_w_id: ''' || OL.OL_SUPPLY_W_ID || ''', ol_quantity: ''' || OL.OL_QUANTITY || ''', ol_amount: ' || OL.OL_AMOUNT || '}', 
--               ','
--           ) || ']'
-- FROM Customer C 
-- JOIN OrderTable O ON C.C_ID = O.O_C_ID AND C.C_D_ID = O.O_D_ID AND C.C_W_ID = O.O_W_ID
-- JOIN OrderLine OL ON O.O_ID = OL.OL_O_ID AND O.O_D_ID = OL.OL_D_ID AND O.O_W_ID = OL.OL_W_ID
-- GROUP BY C.C_W_ID, C.C_D_ID, C.C_ID, O.O_ID, O.O_ENTRY_D, O.O_CARRIER_ID, OL.OL_DELIVERY_D) TO '/container/dir/orders_by_customer.csv' WITH CSV;

-- -- Get customer by order
-- COPY (
-- SELECT C.C_W_ID, C.C_D_ID, C.C_ID, O.O_ID
-- FROM Customer C 
-- JOIN OrderTable O ON C.C_ID = O.O_C_ID AND C.C_D_ID = O.O_D_ID AND C.C_W_ID = O.O_W_ID) TO '/container/dir/customer_by_order.csv' WITH CSV;

-- Transaction 7 denormalization
COPY (SELECT C.C_W_ID, C.C_D_ID, C.C_ID, C.C_FIRST, C.C_MIDDLE, C.C_LAST, C.C_STREET_1, C.C_STREET_2, C.C_CITY, C.C_STATE,  C.C_ZIP, C.C_PHONE, C.C_SINCE, C.C_CREDIT, C.C_CREDIT_LIM, C.C_DISCOUNT, C.C_BALANCE, C.C_YTD_PAYMENT, C.C_PAYMENT_CNT, C.C_DELIVERY_CNT, C.C_DATA, W.W_NAME, D.D_NAME
FROM Customer C
JOIN Warehouse W on C.C_W_ID = W.W_ID
JOIN District D on C.C_D_ID = D.D_ID AND C.C_W_ID = D.D_W_ID) TO '/container/dir/customer_balance.csv' WITH CSV;

-- -- district_by_warehouse denormalization
-- COPY (select W.W_ID, D.D_ID, W.W_NAME, W.W_STREET_1, W.W_STREET_2, W.W_CITY, W.W_STATE, W.W_ZIP, W.W_TAX, D.D_NAME, D.D_STREET_1, D.D_STREET_2, 
-- D.D_CITY, D.D_STATE, D.D_ZIP, D.D_TAX, D.D_YTD, D.D_NEXT_O_ID, (select O.o_id from OrderTable O where O.O_W_ID = W.W_ID and O.O_D_ID = D.D_ID and O.O_CARRIER_ID IS NULL order by O.O_ID asc limit 1) as D_LAST_UNDELIVERED_O_D
-- FROM District D
-- JOIN Warehouse W on D.D_W_ID = W.W_ID) TO '/container/dir/district_by_warehouse.csv' WITH CSV;