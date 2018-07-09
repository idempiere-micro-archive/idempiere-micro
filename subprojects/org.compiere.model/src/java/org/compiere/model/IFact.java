package org.compiere.model;

public interface IFact {
    boolean checkAccounts();

    boolean distribute();

    boolean isSourceBalanced();

    I_Fact_Acct balanceSource();

    boolean isSegmentBalanced();

    void balanceSegments();

    boolean isAcctBalanced();

    boolean save(String trxName);

    I_Fact_Acct balanceAccounting();

    void dispose();
}
