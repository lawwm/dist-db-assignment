
-- Transaction 4 denormalization
\COPY (SELECT
C.C_W_ID, C.C_D_ID, C.C_ID, O.O_ID, O.O_ENTRY_D, O.O_CARRIER_ID, OL.OL_DELIVERY_D,
    '[' || string_agg(
              '{OL_I_ID: ''' || OL.OL_I_ID || ''', OL_SUPPLY_W_ID: ''' || OL.OL_SUPPLY_W_ID || ''', OL_QUANTITY: ''' || OL.OL_QUANTITY || ''', OL_AMOUNT: ' || OL.OL_AMOUNT || '}', 
              ','
          ) || ']'
FROM Customer C 
JOIN OrderTable O ON C.C_ID = O.O_C_ID AND C.C_D_ID = O.O_D_ID AND C.C_W_ID = O.O_W_ID
JOIN OrderLine OL ON O.O_ID = OL.OL_O_ID AND O.O_D_ID = OL.OL_D_ID AND O.O_W_ID = OL.OL_W_ID
GROUP BY C.C_W_ID, C.C_D_ID, C.C_ID, O.O_ID, O.O_ENTRY_D, O.O_CARRIER_ID, OL.OL_DELIVERY_D) TO 'testlearn.csv' WITH CSV;
