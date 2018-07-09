package org.compiere.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public interface IProcessInfoParameter {
    String getParameterName();

    Object getParameter_To();

    Object getParameter();

    boolean getParameterAsBoolean();

    int getParameterAsInt();

    BigDecimal getParameterAsBigDecimal();

    Timestamp getParameterAsTimestamp();

    String getParameterAsString();

    int getParameter_ToAsInt();

    boolean getParameter_ToAsBoolean();
}
