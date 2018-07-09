package org.compiere.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public interface IProcessInfoLog {
    Timestamp getP_Date();
    BigDecimal getP_Number();
    String getP_Msg();

    int getP_ID();

    int getLog_ID();

    int getAD_Table_ID();

    int getRecord_ID();
}
