package org.compiere.model;

import org.idempiere.icommon.model.IPO;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;

public interface IProcessInfo {
    String getTitle();

    int getAD_PInstance_ID();

    int getRecord_ID();

    void setTitle(String string);

    void setClassName(String string);

    void setAD_Process_ID(int anInt);

    void setEstSeconds(int i);

    void setSummary(String translatedSummary, boolean error);
    void setSummary(String processNoProcedure);

    String getClassName();

    void setReportingProcess(boolean b);

    void addSummary(String s);

    void setTransactionName(String trxName);

    String getTransactionName();

    IPO getPO();

    Integer getAD_User_ID();

    Integer getAD_Client_ID();

    int getTable_ID();

    IProcessInfoParameter[] getParameter();

    void setAD_Client_ID(int anInt);

    void setAD_User_ID(int anInt);

    void setParameter(IProcessInfoParameter[] pars);

    void setError(boolean b);

    int getAD_Process_ID();

    boolean isError();

    void addLog(int id, Timestamp date, BigDecimal number, String msg, int tableId, int recordId);

    void addLog(int id, Timestamp date, BigDecimal number, String msg);

    void addLog (IProcessInfoLog logEntry);

    void addLog (int Log_ID, int P_ID, Timestamp P_Date, BigDecimal P_Number, String P_Msg);

    String getSummary();

    IProcessInfoLog[] getLogs();

    void setLogList(ArrayList<IProcessInfoLog> logs);

    void setSerializableObject(Serializable data);

    Serializable getSerializableObject();
}
