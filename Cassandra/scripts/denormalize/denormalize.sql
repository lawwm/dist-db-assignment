-- Transaction 4 denormalization
COPY (SELECT C.C_W_ID, C.C_D_ID, C.C_ID, O.O_ID, O.O_ENTRY_D, O.O_CARRIER_ID, OL.OL_DELIVERY_D,
    '[' || string_agg(
              '{ol_i_id: ''' || OL.OL_I_ID || ''', ol_supply_w_id: ''' || OL.OL_SUPPLY_W_ID || ''', ol_quantity: ''' || OL.OL_QUANTITY || ''', ol_amount: ' || OL.OL_AMOUNT || '}', 
              ','
          ) || ']'
FROM Customer C 
JOIN OrderTable O ON C.C_ID = O.O_C_ID AND C.C_D_ID = O.O_D_ID AND C.C_W_ID = O.O_W_ID
JOIN OrderLine OL ON O.O_ID = OL.OL_O_ID AND O.O_D_ID = OL.OL_D_ID AND O.O_W_ID = OL.OL_W_ID
GROUP BY C.C_W_ID, C.C_D_ID, C.C_ID, O.O_ID, O.O_ENTRY_D, O.O_CARRIER_ID, OL.OL_DELIVERY_D) TO '/container/dir/orders_by_customer.csv' WITH CSV;

-- Transaction 7 denormalization
COPY (SELECT C.C_ID, C.C_FIRST, C.C_MIDDLE, C.C_LAST, C.C_BALANCE, W.W_NAME, D.D_NAME
FROM Customer C
JOIN Warehouse W on C.C_W_ID = W.W_ID
JOIN District D on C.C_D_ID = D.D_ID AND C.C_W_ID = D.D_W_ID) TO '/container/dir/customer_balance.csv' WITH CSV;
