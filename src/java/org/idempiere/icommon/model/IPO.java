package org.idempiere.icommon.model;

import java.util.Properties;

// to be used instead of PO
public interface IPO {
    String[] get_KeyColumns();
    String get_TrxName();
    boolean is_new();
    String get_TableName();
    Object get_Value (String columnName);

    int getAD_Client_ID();
    int getAD_Org_ID();

    Properties getCtx();

    int get_Table_ID();

    int get_ID();

    Object get_ValueOfColumn(int ad_column_id);

    int get_ColumnIndex(String token);
}