package org.compiere.impl;

import org.compiere.acct.Doc;
import org.compiere.crm.MBPartner;
import org.compiere.model.IDoc;
import org.compiere.model.IPODoc;
import org.compiere.model.I_C_Invoice;
import org.compiere.model.I_M_RMALine;
import org.compiere.order.*;
import org.compiere.order.MOrder;
import org.compiere.orm.MOrg;
import org.compiere.orm.Query;
import org.compiere.process.DocAction;
import org.compiere.process2.DocumentEngine;
import org.compiere.util.Msg;
import org.compiere.validation.ModelValidationEngine;
import org.compiere.validation.ModelValidator;
import org.idempiere.common.exceptions.AdempiereException;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;

import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

public class MRMA extends org.compiere.order.MRMA implements DocAction, IPODoc {
    /**
     * 	Standard Constructor
     *	@param ctx context
     *	@param M_RMA_ID id
     *	@param trxName transaction
     */
    public MRMA (Properties ctx, int M_RMA_ID, String trxName) {
        super(ctx, M_RMA_ID, trxName);
    }

    /**
     * Get the original invoice on which the shipment/receipt defined is based upon.
     * @return invoice
     */
    public I_C_Invoice getOriginalInvoice()
    {
        MInOut shipment = getShipment();
        if (shipment == null)
        {
            return null;
        }

        int invId = 0;

        if (shipment.getC_Invoice_ID() != 0)
        {
            invId = shipment.getC_Invoice_ID();
        }
        else
        {
            String sqlStmt = "SELECT C_Invoice_ID FROM C_Invoice WHERE C_Order_ID=?";
            invId = DB.getSQLValueEx(null, sqlStmt, shipment.getC_Order_ID());
        }

        if (invId <= 0)
        {
            return null;
        }

        return new MInvoice(getCtx(), invId, get_TrxName());
    }

    /**
     * 	Before Save
     *	Set BPartner, Currency
     *	@param newRecord new
     *	@return true
     */
    @Override
    protected boolean beforeSave (boolean newRecord)
    {
        if (newRecord)
            setC_Order_ID(0);
        getShipment();
        //	Set BPartner
        if (getC_BPartner_ID() == 0)
        {
            if (m_inout != null)
                setC_BPartner_ID(m_inout.getC_BPartner_ID());
        }
        //	Set Currency
        if (getC_Currency_ID() == 0)
        {
            if (m_inout != null)
            {
                if (m_inout.getC_Order_ID() != 0)
                {
                    org.compiere.order.MOrder order = new MOrder(getCtx(), m_inout.getC_Order_ID(), get_TrxName());
                    setC_Currency_ID(order.getC_Currency_ID());
                }
                else if (m_inout.getC_Invoice_ID() != 0)
                {
                    MInvoice invoice = new MInvoice (getCtx(), m_inout.getC_Invoice_ID(), get_TrxName());
                    setC_Currency_ID(invoice.getC_Currency_ID());
                }
            }
        }

        // Verification whether Shipment/Receipt matches RMA for sales transaction
        if (m_inout != null && m_inout.isSOTrx() != isSOTrx())
        {
            log.saveError("RMA.IsSOTrx <> InOut.IsSOTrx", "");
            return false;
        }

        return true;
    }	//	beforeSave


    /**************************************************************************
     * 	Process document
     *	@param processAction document action
     *	@return true if performed
     */
    public boolean processIt (String processAction)
    {
        m_processMsg = null;
        DocumentEngine engine = new DocumentEngine (this, getDocStatus());
        return engine.processIt (processAction, getDocAction());
    }	//	process

    /**
     *	Prepare Document
     * 	@return new status (In Progress or Invalid)
     */
    public String prepareIt()
    {
        if (log.isLoggable(Level.INFO)) log.info(toString());
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
        if (m_processMsg != null)
            return DocAction.STATUS_Invalid;

        MRMALine[] lines = getLines(false);
        if (lines.length == 0)
        {
            m_processMsg = "@NoLines@";
            return DocAction.STATUS_Invalid;
        }

        for (MRMALine line : lines)
        {
            if (line.getM_InOutLine_ID() != 0)
            {
                if (!line.checkQty())
                {
                    m_processMsg = "@AmtReturned>Shipped@";
                    return DocAction.STATUS_Invalid;
                }
            }
        }

        // Updates Amount
        if (!calculateTaxTotal())
        {
            m_processMsg = "Error calculating tax";
            return DocAction.STATUS_Invalid;
        }

        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
        if (m_processMsg != null)
            return DocAction.STATUS_Invalid;

        m_justPrepared = true;
        return DocAction.STATUS_InProgress;
    }	//	prepareIt

    /**
     * 	Complete Document
     * 	@return new status (Complete, In Progress, Invalid, Waiting ..)
     */
    public String completeIt()
    {
        //	Re-Check
        if (!m_justPrepared)
        {
            String status = prepareIt();
            m_justPrepared = false;
            if (!DocAction.STATUS_InProgress.equals(status))
                return status;
        }

        // Set the definite document number after completed (if needed)
        setDefiniteDocumentNo();

        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
        if (m_processMsg != null)
            return DocAction.STATUS_Invalid;

        //	Implicit Approval
        if (!isApproved())
            approveIt();
        if (log.isLoggable(Level.INFO)) log.info("completeIt - " + toString());
        //
		/*
		Flow for the creation of the credit memo document changed
        if (true)
		{
			m_processMsg = "Need to code creating the credit memo";
			return DocAction.STATUS_InProgress;
		}
        */

        //		Counter Documents
        org.compiere.order.MRMA counter = createCounterDoc();
        if (counter != null)
            m_processMsg = "@CounterDoc@: RMA=" + counter.getDocumentNo();

        //	User Validation
        String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
        if (valid != null)
        {
            m_processMsg = valid;
            return DocAction.STATUS_Invalid;
        }

        //
        setProcessed(true);
        setDocAction(X_M_RMA.DOCACTION_Close);
        return DocAction.STATUS_Completed;
    }	//	completeIt

    /**
     * 	Create new RMA by copying
     * 	@param from RMA
     * 	@param C_DocType_ID doc type
     * 	@param isSOTrx sales order
     * 	@param counter create counter links
     * 	@param trxName trx
     *	@return MRMA
     */
    public static MRMA copyFrom (MRMA from, int C_DocType_ID, boolean isSOTrx, boolean counter, String trxName) {
        MRMA to = new MRMA(from.getCtx(), 0, null);
        return (MRMA)doCopyFrom (from, C_DocType_ID, isSOTrx, counter, trxName, to );
    }


    /**************************************************************************
     * 	Create Counter Document
     * 	@return InOut
     */
    private org.compiere.order.MRMA createCounterDoc()
    {
        //	Is this a counter doc ?
        if (getRef_RMA_ID() > 0)
            return null;

        //	Org Must be linked to BPartner
        org.compiere.orm.MOrg org = MOrg.get(getCtx(), getAD_Org_ID());
        int counterC_BPartner_ID = org.getLinkedC_BPartner_ID(get_TrxName());
        if (counterC_BPartner_ID == 0)
            return null;
        //	Business Partner needs to be linked to Org
        org.compiere.crm.MBPartner bp = new MBPartner(getCtx(), getC_BPartner_ID(), get_TrxName());
        int counterAD_Org_ID = bp.getAD_OrgBP_ID_Int();
        if (counterAD_Org_ID == 0)
            return null;

        //	Document Type
        int C_DocTypeTarget_ID = 0;
        MDocTypeCounter counterDT = MDocTypeCounter.getCounterDocType(getCtx(), getC_DocType_ID());
        if (counterDT != null)
        {
            if (log.isLoggable(Level.FINE)) log.fine(counterDT.toString());
            if (!counterDT.isCreateCounter() || !counterDT.isValid())
                return null;
            C_DocTypeTarget_ID = counterDT.getCounter_C_DocType_ID();
        }
        else	//	indirect
        {
            C_DocTypeTarget_ID = MDocTypeCounter.getCounterDocType_ID(getCtx(), getC_DocType_ID());
            if (log.isLoggable(Level.FINE)) log.fine("Indirect C_DocTypeTarget_ID=" + C_DocTypeTarget_ID);
            if (C_DocTypeTarget_ID <= 0)
                return null;
        }

        //	Deep Copy
        MRMA counter = copyFrom(this, C_DocTypeTarget_ID, !isSOTrx(), true, get_TrxName());

        //
        counter.setAD_Org_ID(counterAD_Org_ID);
        counter.setC_BPartner_ID(counterC_BPartner_ID);
        counter.saveEx(get_TrxName());

        //	Update copied lines
        MRMALine[] counterLines = counter.getLines(true);
        for (int i = 0; i < counterLines.length; i++)
        {
            MRMALine counterLine = counterLines[i];
            counterLine.setClientOrg(counter);
            //
            counterLine.saveEx(get_TrxName());
        }

        if (log.isLoggable(Level.FINE)) log.fine(counter.toString());

        //	Document Action
        if (counterDT != null)
        {
            if (counterDT.getDocAction() != null)
            {
                counter.setDocAction(counterDT.getDocAction());
                // added AdempiereException by zuhri
                if (!counter.processIt(counterDT.getDocAction()))
                    throw new AdempiereException("Failed when processing document - " + counter.getProcessMsg());
                // end added
                counter.saveEx(get_TrxName());
            }
        }
        return counter;
    }	//	createCounterDoc


    /**
     * 	Void Document.
     * 	@return true if success
     */
    public boolean voidIt()
    {
        if (log.isLoggable(Level.INFO)) log.info("voidIt - " + toString());
        // Before Void
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
        if (m_processMsg != null)
            return false;

        // IDEMPIERE-98 - Implement void for completed RMAs - Diego Ruiz - globalqss
        String validation = "SELECT COUNT(1) "
            +"FROM M_InOut "
            +"WHERE M_RMA_ID=? AND (DocStatus NOT IN ('VO','RE'))";
        int count = DB.getSQLValueEx(get_TrxName(), validation, getM_RMA_ID()) ;

        if (count == 0)
        {
            MRMALine lines[] = getLines(true);
            // Set Qty and Amt on all lines to be Zero
            for (MRMALine rmaLine : lines)
            {
                rmaLine.addDescription(Msg.getMsg(getCtx(), "Voided") + " (" + rmaLine.getQty() + ")");
                rmaLine.setQty(Env.ZERO);
                rmaLine.setAmt(Env.ZERO);
                rmaLine.saveEx();
            }

            addDescription(Msg.getMsg(getCtx(), "Voided"));
            setAmt(Env.ZERO);
        }
        else
        {
            m_processMsg = Msg.getMsg(getCtx(), "RMACannotBeVoided");
            return false;
        }

        // update taxes
        MRMATax[] taxes = getTaxes(true);
        for (MRMATax tax : taxes )
        {
            if ( !(tax.calculateTaxFromLines() && tax.save()) )
                return false;
        }

        // After Void
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
        if (m_processMsg != null)
            return false;

        setProcessed(true);
        setDocAction(X_M_RMA.DOCACTION_None);
        return true;
    }	//	voidIt

    /**
     * 	Close Document.
     * 	Cancel not delivered Qunatities
     * 	@return true if success
     */
    public boolean closeIt()
    {
        if (log.isLoggable(Level.INFO)) log.info("closeIt - " + toString());
        // Before Close
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_CLOSE);
        if (m_processMsg != null)
            return false;
        // After Close
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_CLOSE);
        if (m_processMsg != null)
            return false;

        return true;
    }	//	closeIt

    /**
     * 	Reverse Correction
     * 	@return true if success
     */
    public boolean reverseCorrectIt()
    {
        if (log.isLoggable(Level.INFO)) log.info("reverseCorrectIt - " + toString());
        // Before reverseCorrect
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSECORRECT);
        if (m_processMsg != null)
            return false;

        // After reverseCorrect
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSECORRECT);
        if (m_processMsg != null)
            return false;

        return false;
    }	//	reverseCorrectionIt

    /**
     * 	Reverse Accrual - none
     * 	@return true if success
     */
    public boolean reverseAccrualIt()
    {
        if (log.isLoggable(Level.INFO)) log.info("reverseAccrualIt - " + toString());
        // Before reverseAccrual
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
        if (m_processMsg != null)
            return false;

        // After reverseAccrual
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
        if (m_processMsg != null)
            return false;

        return false;
    }	//	reverseAccrualIt

    /**
     * 	Re-activate
     * 	@return true if success
     */
    public boolean reActivateIt()
    {
        if (log.isLoggable(Level.INFO)) log.info("reActivateIt - " + toString());
        // Before reActivate
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REACTIVATE);
        if (m_processMsg != null)
            return false;

        // After reActivate
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REACTIVATE);
        if (m_processMsg != null)
            return false;

        return false;
    }	//	reActivateIt

    /** Lines					*/
    protected MRMALine[]		m_lines = null;

    /**
     * 	Get Lines
     *	@param requery requery
     *	@return lines
     */
    public MRMALine[] getLines (boolean requery)
    {
        if (m_lines != null && !requery)
        {
            PO.set_TrxName(m_lines, get_TrxName());
            return m_lines;
        }
        List<MRMALine> list = new Query(getCtx(), I_M_RMALine.Table_Name, "M_RMA_ID=?", get_TrxName())
            .setParameters(getM_RMA_ID())
            .setOrderBy(MRMALine.COLUMNNAME_Line)
            .list();

        m_lines = new MRMALine[list.size ()];
        list.toArray (m_lines);
        return m_lines;
    }	//	getLines

    /* Doc - To be used on ModelValidator to get the corresponding Doc from the PO */
    private IDoc m_doc;

    @Override
    public void setDoc(IDoc doc) {
        m_doc = doc;
    }

    /**
     * 	Get Shipment
     *	@return shipment
     */
    public MInOut getShipment()
    {
        if (m_inout == null && getInOut_ID() != 0)
            m_inout = new MInOut (getCtx(), getInOut_ID(), get_TrxName());
        return (MInOut)m_inout;
    }	//	getShipment
}
