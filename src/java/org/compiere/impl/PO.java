package org.compiere.impl;

import org.compiere.acct.Doc;
import org.compiere.model.IDoc;
import org.compiere.model.IPODoc;
import org.compiere.model.I_C_ElementValue;
import org.compiere.orm.*;
import org.compiere.product.MAttributeInstance;
import org.compiere.product.UUIDGenerator;
import org.compiere.util.DisplayType;
import org.compiere.util.Msg;
import org.compiere.validation.ModelValidationEngine;
import org.compiere.validation.ModelValidator;
import org.compiere.wf.MMessage;
import org.idempiere.common.exceptions.AdempiereException;
import org.idempiere.common.exceptions.DBException;
import org.idempiere.common.util.*;
import org.idempiere.orm.EventManager;
import org.idempiere.orm.IEventTopics;
import org.idempiere.orm.Lookup;
import org.idempiere.orm.Null;
import org.osgi.service.event.Event;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public abstract class PO extends org.compiere.orm.PO implements IPODoc {

    public PO(Properties ctx) {
        super(ctx);
    }

    public PO(Properties ctx, int ID, String trxName) {
        super(ctx, ID, trxName);
    }

    public PO(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    public PO(Properties ctx, int ID, String trxName, ResultSet rs) {
        super(ctx, ID, trxName, rs);
    }

    /** Model Info              */
    protected org.idempiere.orm.POInfo getP_info() {
        return super.p_info;
    }
    /** Model Info              */
    protected POInfo getP_info2() {
        return (POInfo)super.p_info;
    }

    /**
     * 	Create/return Attachment for PO.
     * 	If not exist, create new
     *	@return attachment
     */
    public MAttachment createAttachment()
    {
        getAttachment (false);
        if (m_attachment == null)
            m_attachment = new MAttachment (getCtx(), p_info.getAD_Table_ID(), get_ID(), null);
        return m_attachment;
    }	//	createAttachment


    /**
     * 	Do we have a Attachment of type
     * 	@param extension extension e.g. .pdf
     * 	@return true if there is a attachment of type
     */
    public boolean isAttachment (String extension)
    {
        getAttachment (false);
        if (m_attachment == null)
            return false;
        for (int i = 0; i < m_attachment.getEntryCount(); i++)
        {
            if (m_attachment.getEntryName(i).endsWith(extension))
            {
                if (log.isLoggable(Level.FINE)) log.fine("#" + i + ": " + m_attachment.getEntryName(i));
                return true;
            }
        }
        return false;
    }	//	isAttachment

    /**
     * 	Get Attachment Data of type
     * 	@param extension extension e.g. .pdf
     *	@return data or null
     */
    public byte[] getAttachmentData (String extension)
    {
        getAttachment(false);
        if (m_attachment == null)
            return null;
        for (int i = 0; i < m_attachment.getEntryCount(); i++)
        {
            if (m_attachment.getEntryName(i).endsWith(extension))
            {
                if (log.isLoggable(Level.FINE)) log.fine("#" + i + ": " + m_attachment.getEntryName(i));
                return m_attachment.getEntryData(i);
            }
        }
        return null;
    }	//	getAttachmentData

    /**
     * 	Do we have a PDF Attachment
     * 	@return true if there is a PDF attachment
     */
    public boolean isPdfAttachment()
    {
        return isAttachment(".pdf");
    }	//	isPdfAttachment

    /**
     * 	Get PDF Attachment Data
     *	@return data or null
     */    
    public byte[] getPdfAttachment()
    {
        return getAttachmentData(".pdf");
    }	//	getPDFAttachment

    /**
     *  Set Value if updateable and correct class.
     *  (and to NULL if not mandatory)
     *  @param index index
     *  @param value value
     *  @param checkWritable
     *  @return true if value set
     */
    @Override
    protected boolean set_Value (int index, Object value, boolean checkWritable) {
        boolean result = super.set_Value(index, value, checkWritable );

        if (result) {
            String ColumnName = p_info.getColumnName(index);

            if (value != null) {
                // Validate reference list [1762461]
                if (p_info.getColumn(index).DisplayType == DisplayType.List &&
                        p_info.getColumn(index).AD_Reference_Value_ID > 0 &&
                        value instanceof String) {
                    if (MRefList.get(getCtx(), p_info.getColumn(index).AD_Reference_Value_ID,
                            (String) value, get_TrxName()) != null)
                        ;
                    else {
                        StringBuilder validValues = new StringBuilder();
                        for (ValueNamePair vp : MRefList.getList(getCtx(), p_info.getColumn(index).AD_Reference_Value_ID, false))
                            validValues.append(" - ").append(vp.getValue());
                        String errmsg = ColumnName + " Invalid value - "
                                + value + " - Reference_ID=" + p_info.getColumn(index).AD_Reference_Value_ID + validValues.toString();
                        log.saveError("Validate", errmsg);
                        m_setErrors[index] = new ValueNamePair("Validate", errmsg);
                        return false;
                    }
                }
                if (log.isLoggable(Level.FINEST))
                    log.finest(ColumnName + " = " + m_newValues[index] + " (OldValue=" + m_oldValues[index] + ")");
            }

            // FR 2962094 Fill ProcessedOn when the Processed column is changing from N to Y
            setProcessedOn(ColumnName, value, m_oldValues[index]);

            return true;
        }
        return false;
    }


    /**
     *  Set Encrypted Value
     *  @param ColumnName column name
     *  @param value value
     *  @return true if value set
     */
    protected final boolean set_ValueE (String ColumnName, Object value)
    {
        return set_Value (ColumnName, value);
    }   //  setValueE


    /**
     *  Set Encrypted Value w/o check (update, r/o, ..).
     * 	Used when Column is R/O
     *  Required for key and parent values
     *  @param ColumnName column name
     *  @param value value
     *  @return true if value set
     */
    protected final boolean set_ValueNoCheckE (String ColumnName, Object value)
    {
        return set_ValueNoCheck (ColumnName, value);
    }	//	set_ValueNoCheckE



    /**
     * 	Set Custom Column
     *	@param columnName column
     *	@param value value
     */
    public final void set_CustomColumn (String columnName, Object value)
    {
        set_CustomColumnReturningBoolean (columnName, value);
    }	//	set_CustomColumn

    /**
     * 	Set Custom Column returning boolean
     *	@param columnName column
     *	@param value value
     *  @returns boolean indicating success or failure
     */
    public final boolean set_CustomColumnReturningBoolean (String columnName, Object value)
    {
        // [ 1845793 ] PO.set_CustomColumn not updating correctly m_newValues
        // this is for columns not in PO - verify and call proper method if exists
        int poIndex = get_ColumnIndex(columnName);
        if (poIndex > 0) {
            // is not custom column - it exists in the PO
            return set_Value(columnName, value);
        }
        if (m_custom == null)
            m_custom = new HashMap<String,String>();
        String valueString = "NULL";
        if (value == null)
            ;
        else if (value instanceof Number)
            valueString = value.toString();
        else if (value instanceof Boolean)
            valueString = ((Boolean)value).booleanValue() ? "'Y'" : "'N'";
        else if (value instanceof Timestamp)
            valueString = DB.TO_DATE((Timestamp)value, false);
        else //	if (value instanceof String)
            valueString = DB.TO_STRING(value.toString());
        //	Save it
        if (log.isLoggable(Level.INFO))log.log(Level.INFO, columnName + "=" + valueString);
        m_custom.put(columnName, valueString);
        return true;
    }	//	set_CustomColumn

    /**
     * 	Copy old values of From to new values of To.
     *  Does not copy Keys and AD_Client_ID/AD_Org_ID
     * 	@param from old, existing & unchanged PO
     *  @param to new, not saved PO
     */
    public static void copyValues (PO from, PO to)
    {
        if (s_log.isLoggable(Level.FINE)) s_log.fine("From ID=" + from.get_ID() + " - To ID=" + to.get_ID());
        //	Different Classes
        if (from.getClass() != to.getClass())
        {
            for (int i1 = 0; i1 < from.m_oldValues.length; i1++)
            {
                String colName = from.p_info.getColumnName(i1);
                MColumn column = MColumn.get(from.getCtx(), from.p_info.getAD_Column_ID(colName));
                if (   column.isVirtualColumn()
                        || column.isKey()		//	KeyColumn
                        || column.isUUIDColumn() // IDEMPIERE-67
                        || column.isStandardColumn()
                        || ! column.isAllowCopy())
                    continue;
                for (int i2 = 0; i2 < to.m_oldValues.length; i2++)
                {
                    if (to.p_info.getColumnName(i2).equals(colName))
                    {
                        to.m_newValues[i2] = from.m_oldValues[i1];
                        break;
                    }
                }
            }	//	from loop
        }
        else	//	same class
        {
            for (int i = 0; i < from.m_oldValues.length; i++)
            {
                String colName = from.p_info.getColumnName(i);
                MColumn column = MColumn.get(from.getCtx(), from.p_info.getAD_Column_ID(colName));
                if (   column.isVirtualColumn()
                        || column.isKey()		//	KeyColumn
                        || column.isUUIDColumn()
                        || column.isStandardColumn()
                        || ! column.isAllowCopy())
                    continue;
                to.m_newValues[i] = from.m_oldValues[i];
            }
        }	//	same class
    }	//	copy

    /**
     * 	Copy old values of From to new values of To.
     *  Does not copy Keys
     * 	@param from old, existing & unchanged PO
     *  @param to new, not saved PO
     * 	@param AD_Client_ID client
     * 	@param AD_Org_ID org
     */
    protected static void copyValues (PO from, PO to, int AD_Client_ID, int AD_Org_ID)
    {
        copyValues (from, to);
        to.setAD_Client_ID(AD_Client_ID);
        to.setAD_Org_ID(AD_Org_ID);
    }	//	copyValues


    /**
     * 	Is new record
     *	@return true if new
     */
    @Override
    public boolean is_new()
    {
        if (m_createNew)
            return true;
        //
        for (int i = 0; i < m_IDs.length; i++)
        {
            if (m_IDs[i].equals(I_ZERO) || m_IDs[i] == Null.NULL)
                continue;
            return false;	//	one value is non-zero
        }
        if (MTable.isZeroIDTable(get_TableName()))
            return false;
        return true;
    }	//	is_new

    /*
     * Classes which override save() method:
     * DocActionTemplate
     * MClient
     * MClientInfo
     * MSystem
     */
    /**************************************************************************
     *  Update Value or create new record.
     * 	To reload call load() - not updated
     *  @return true if saved
     */
    @Override
    public boolean save()
    {
        checkValidContext();
        CLogger.resetLast();
        boolean newRecord = is_new();	//	save locally as load resets
        if (!newRecord && !is_Changed())
        {
            if (log.isLoggable(Level.FINE)) log.fine("Nothing changed - " + p_info.getTableName());
            return true;
        }

        for (int i = 0; i < m_setErrors.length; i++) {
            ValueNamePair setError = m_setErrors[i];
            if (setError != null) {
                log.saveError(setError.getValue(), Msg.getElement(getCtx(), p_info.getColumnName(i)) + " - " + setError.getName());
                return false;
            }
        }

        //	Organization Check
        if (getAD_Org_ID() == 0
                && (get_AccessLevel() == ACCESSLEVEL_ORG
                || (get_AccessLevel() == ACCESSLEVEL_CLIENTORG
                && MClientShare.isOrgLevelOnly(getAD_Client_ID(), get_Table_ID()))))
        {
            log.saveError("FillMandatory", Msg.getElement(getCtx(), "AD_Org_ID"));
            return false;
        }
        //	Should be Org 0
        if (getAD_Org_ID() != 0)
        {
            boolean reset = get_AccessLevel() == ACCESSLEVEL_SYSTEM;
            if (!reset && MClientShare.isClientLevelOnly(getAD_Client_ID(), get_Table_ID()))
            {
                reset = get_AccessLevel() == ACCESSLEVEL_CLIENT
                        || get_AccessLevel() == ACCESSLEVEL_SYSTEMCLIENT
                        || get_AccessLevel() == ACCESSLEVEL_ALL
                        || get_AccessLevel() == ACCESSLEVEL_CLIENTORG;
            }
            if (reset)
            {
                log.warning("Set Org to 0");
                setAD_Org_ID(0);
            }
        }

        Trx localTrx = null;
        Trx trx = null;
        Savepoint savepoint = null;
        if (m_trxName == null)
        {
            StringBuilder l_trxname = new StringBuilder(LOCAL_TRX_PREFIX)
                    .append(get_TableName());
            if (l_trxname.length() > 23)
                l_trxname.setLength(23);
            m_trxName = Trx.createTrxName(l_trxname.toString());
            localTrx = Trx.get(m_trxName, true);
            localTrx.setDisplayName(getClass().getName()+"_save");
            localTrx.getConnection();
        }
        else
        {
            trx = Trx.get(m_trxName, false);
            if (trx == null)
            {
                // Using a trx that was previously closed or never opened
                // Creating and starting the transaction right here, but please note
                // that this is not a good practice
                trx = Trx.get(m_trxName, true);
                log.severe("Transaction closed or never opened ("+m_trxName+") => starting now --> " + toString());
            }
        }

        //	Before Save
        try
        {
            // If not a localTrx we need to set a savepoint for rollback
            if (localTrx == null)
                savepoint = trx.setSavepoint(null);

            if (!beforeSave(newRecord))
            {
                log.warning("beforeSave failed - " + toString());
                if (localTrx != null)
                {
                    localTrx.rollback();
                    localTrx.close();
                    m_trxName = null;
                }
                else
                {
                    trx.rollback(savepoint);
                    savepoint = null;
                }
                return false;
            }
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "beforeSave - " + toString(), e);
            String msg = org.idempiere.common.exceptions.DBException.getDefaultDBExceptionMessage(e);
            log.saveError(msg != null ? msg : "Error", e, false);
            if (localTrx != null)
            {
                localTrx.rollback();
                localTrx.close();
                m_trxName = null;
            }
            else if (savepoint != null)
            {
                try
                {
                    trx.rollback(savepoint);
                } catch (SQLException e1){}
                savepoint = null;
            }
            return false;
        }

        try
        {
            // Call ModelValidators TYPE_NEW/TYPE_CHANGE
            String errorMsg = ModelValidationEngine.get().fireModelChange
                    (this, newRecord ? ModelValidator.TYPE_NEW : ModelValidator.TYPE_CHANGE);
            if (errorMsg != null)
            {
                log.warning("Validation failed - " + errorMsg);
                log.saveError("Error", errorMsg);
                if (localTrx != null)
                {
                    localTrx.rollback();
                    m_trxName = null;
                }
                else
                {
                    trx.rollback(savepoint);
                }
                return false;
            }
            //	Save
            if (newRecord)
            {
                boolean b = saveNew();
                if (b)
                {
                    if (localTrx != null)
                        return localTrx.commit();
                    else
                        return b;
                }
                else
                {
                    validateUniqueIndex();
                    if (localTrx != null)
                        localTrx.rollback();
                    else
                        trx.rollback(savepoint);
                    return b;
                }
            }
            else
            {
                boolean b = saveUpdate();
                if (b)
                {
                    if (localTrx != null)
                        return localTrx.commit();
                    else
                        return b;
                }
                else
                {
                    validateUniqueIndex();
                    if (localTrx != null)
                        localTrx.rollback();
                    else
                        trx.rollback(savepoint);
                    return b;
                }
            }
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "afterSave - " + toString(), e);
            String msg = DBException.getDefaultDBExceptionMessage(e);
            log.saveError(msg != null ? msg : "Error", e);
            if (localTrx != null)
            {
                localTrx.rollback();
            }
            else if (savepoint != null)
            {
                try
                {
                    trx.rollback(savepoint);
                } catch (SQLException e1){}
                savepoint = null;
            }
            return false;
        }
        finally
        {
            if (localTrx != null)
            {
                localTrx.close();
                m_trxName = null;
            }
            else
            {
                if (savepoint != null)
                {
                    try {
                        trx.releaseSavepoint(savepoint);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                savepoint = null;
                trx = null;
            }
        }
    }	//	save

    /**
     * 	Finish Save Process
     *	@param newRecord new
     *	@param success success
     *	@return true if saved
     */
    @Override
    protected boolean saveFinish (boolean newRecord, boolean success)
    {
        //	Translations
        if (success)
        {
            if (newRecord)
                insertTranslations();
            else
                updateTranslations();

            // table with potential tree
            if (get_ColumnIndex("IsSummary") >= 0) {
                if (newRecord)
                    insert_Tree(MTree_Base.TREETYPE_CustomTable);
                int idxValue = get_ColumnIndex("Value");
                if (newRecord || (idxValue >= 0 && is_ValueChanged(idxValue)))
                    update_Tree(MTree_Base.TREETYPE_CustomTable);
            }
        }
        //
        try
        {
            success = afterSave (newRecord, success);
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "afterSave", e);
            log.saveError("Error", e, false);
            success = false;
            //	throw new DBException(e);
        }
        // Call ModelValidators TYPE_AFTER_NEW/TYPE_AFTER_CHANGE - teo_sarca [ 1675490 ]
        if (success) {
            String errorMsg = ModelValidationEngine.get().fireModelChange
                    (this, newRecord ?
                            (isReplication() ? ModelValidator.TYPE_AFTER_NEW_REPLICATION : ModelValidator.TYPE_AFTER_NEW)
                            :
                            (isReplication() ? ModelValidator.TYPE_AFTER_CHANGE_REPLICATION : ModelValidator.TYPE_AFTER_CHANGE)
                    );
            setReplication(false);
            if (errorMsg != null) {
                log.saveError("Error", errorMsg);
                success = false;
            }
        }
        //	OK
        if (success)
        {
            //post osgi event
            String topic = newRecord ? IEventTopics.PO_POST_CREATE : IEventTopics.PO_POST_UPADTE;
            Event event = EventManager.newEvent(topic, this);
            EventManager.getInstance().postEvent(event);

            if (s_docWFMgr == null)
            {
                try
                {
                    Class.forName("org.compiere.wf.DocWorkflowManager");
                }
                catch (Exception e)
                {
                }
            }
            if (s_docWFMgr != null)
                s_docWFMgr.process (this, p_info.getAD_Table_ID());

            //	Copy to Old values
            int size = p_info.getColumnCount();
            for (int i = 0; i < size; i++)
            {
                if (m_newValues[i] != null)
                {
                    if (m_newValues[i] == Null.NULL)
                        m_oldValues[i] = null;
                    else
                        m_oldValues[i] = m_newValues[i];
                }
            }
            m_newValues = new Object[size];
            m_createNew = false;
        }
        if (!newRecord)
            CacheMgt.get().reset(p_info.getTableName());
        else if (get_ID() > 0 && success)
            CacheMgt.get().newRecord(p_info.getTableName(), get_ID());

        return success;
    }	//	saveFinish

    public void saveReplica (boolean isFromReplication) throws AdempiereException
    {
        setReplication(isFromReplication);
        saveEx();
    }

    @Override
    protected boolean doUpdate(boolean withValues) {
        //params for insert statement
        List<Object> params = new ArrayList<Object>();

        String where = get_WhereClause(true);
        //
        boolean changes = false;
        StringBuilder sql = new StringBuilder ("UPDATE ");
        sql.append(p_info.getTableName()).append( " SET ");
        boolean updated = false;
        boolean updatedBy = false;
        lobReset();

        //	Change Log
        MSession session = MSession.get (p_ctx, false);
        if (session == null)
            log.fine("No Session found");
        int AD_ChangeLog_ID = 0;

        int size = get_ColumnCount();
        for (int i = 0; i < size; i++)
        {
            Object value = m_newValues[i];
            if (value == null
                    || p_info.isVirtualColumn(i))
                continue;
            //  we have a change
            Class<?> c = p_info.getColumnClass(i);
            int dt = p_info.getColumnDisplayType(i);
            String columnName = p_info.getColumnName(i);
            //
            //	updated/by
            if (columnName.equals("UpdatedBy"))
            {
                if (updatedBy)	//	explicit
                    continue;
                updatedBy = true;
            }
            else if (columnName.equals("Updated"))
            {
                if (updated)
                    continue;
                updated = true;
            }
            if (DisplayType.isLOB(dt))
            {
                lobAdd (value, i, dt);
                //	If no changes set UpdatedBy explicitly to ensure commit of lob
                if (!changes && !updatedBy)
                {
                    int AD_User_ID = Env.getContextAsInt(p_ctx, "#AD_User_ID");
                    set_ValueNoCheck("UpdatedBy", new Integer(AD_User_ID));
                    sql.append("UpdatedBy=").append(AD_User_ID);
                    changes = true;
                    updatedBy = true;
                }
                continue;
            }
            //	Update Document No
            if (columnName.equals("DocumentNo"))
            {
                String strValue = (String)value;
                if (strValue.startsWith("<") && strValue.endsWith(">"))
                {
                    value = null;
                    int AD_Client_ID = getAD_Client_ID();
                    int index = p_info.getColumnIndex("C_DocTypeTarget_ID");
                    if (index == -1)
                        index = p_info.getColumnIndex("C_DocType_ID");
                    if (index != -1)		//	get based on Doc Type (might return null)
                        value = MSequence.getDocumentNo(get_ValueAsInt(index), m_trxName, false, this);
                    if (value == null)	//	not overwritten by DocType and not manually entered
                        value = MSequence.getDocumentNo(AD_Client_ID, p_info.getTableName(), m_trxName, this);
                }
                else
                if (log.isLoggable(Level.INFO)) log.info("DocumentNo updated: " + m_oldValues[i] + " -> " + value);
            }

            if (changes)
                sql.append(", ");
            changes = true;
            sql.append(columnName).append("=");

            if (withValues)
            {
                //  values
                if (value == Null.NULL)
                    sql.append("NULL");
                else if (value instanceof Integer || value instanceof BigDecimal)
                    sql.append(value);
                else if (c == Boolean.class)
                {
                    boolean bValue = false;
                    if (value instanceof Boolean)
                        bValue = ((Boolean)value).booleanValue();
                    else
                        bValue = "Y".equals(value);
                    sql.append(encrypt(i,bValue ? "'Y'" : "'N'"));
                }
                else if (value instanceof Timestamp)
                    sql.append(DB.TO_DATE((Timestamp)encrypt(i,value),p_info.getColumnDisplayType(i) == DisplayType.Date));
                else {
                    if (value.toString().length() == 0) {
                        // [ 1722057 ] Encrypted columns throw error if saved as null
                        // don't encrypt NULL
                        sql.append(DB.TO_STRING(value.toString()));
                    } else {
                        sql.append(encrypt(i,DB.TO_STRING(value.toString())));
                    }
                }
            }
            else
            {
                if (value instanceof Timestamp && dt == DisplayType.Date)
                    sql.append("trunc(cast(? as date))");
                else
                    sql.append("?");

                if (value == Null.NULL)
                {
                    params.add(null);
                }
                else if (c == Boolean.class)
                {
                    boolean bValue = false;
                    if (value instanceof Boolean)
                        bValue = ((Boolean)value).booleanValue();
                    else
                        bValue = "Y".equals(value);
                    params.add(encrypt(i,bValue ? "Y" : "N"));
                }
                else if (c == String.class)
                {
                    if (value.toString().length() == 0) {
                        // [ 1722057 ] Encrypted columns throw error if saved as null
                        // don't encrypt NULL
                        params.add(null);
                    } else {
                        params.add(encrypt(i,value));
                    }
                }
                else
                {
                    params.add(value);
                }
            }

            //	Change Log	- Only
            if (session != null
                    && m_IDs.length == 1
                    && p_info.isAllowLogging(i)		//	logging allowed
                    && !p_info.isEncrypted(i)		//	not encrypted
                    && !p_info.isVirtualColumn(i)	//	no virtual column
                    && !"Password".equals(columnName)
                    )
            {
                Object oldV = m_oldValues[i];
                Object newV = value;
                if (oldV != null && oldV == Null.NULL)
                    oldV = null;
                if (newV != null && newV == Null.NULL)
                    newV = null;
                // change log on update
                MChangeLog cLog = session.changeLog (
                        m_trxName, AD_ChangeLog_ID,
                        p_info.getAD_Table_ID(), p_info.getColumn(i).AD_Column_ID,
                        get_ID(), getAD_Client_ID(), getAD_Org_ID(), oldV, newV, MChangeLog.EVENTCHANGELOG_Update);
                if (cLog != null)
                    AD_ChangeLog_ID = cLog.getAD_ChangeLog_ID();
            }
        }	//   for all fields

        //	Custom Columns (cannot be logged as no column)
        if (m_custom != null)
        {
            Iterator<String> it = m_custom.keySet().iterator();
            while (it.hasNext())
            {
                if (changes)
                    sql.append(", ");
                changes = true;
                //
                String column = (String)it.next();
                String value = (String)m_custom.get(column);
                int index = p_info.getColumnIndex(column);
                if (withValues)
                {
                    sql.append(column).append("=").append(encrypt(index,value));
                }
                else
                {
                    sql.append(column).append("=?");
                    if (value == null || value.toString().length() == 0)
                    {
                        params.add(null);
                    }
                    else
                    {
                        params.add(encrypt(index,value));
                    }
                }
            }
            m_custom = null;
        }

        //	Something changed
        if (changes)
        {
            if (m_trxName == null) {
                if (log.isLoggable(Level.FINE)) log.fine(p_info.getTableName() + "." + where);
            } else {
                if (log.isLoggable(Level.FINE)) log.fine("[" + m_trxName + "] - " + p_info.getTableName() + "." + where);
            }
            if (!updated)	//	Updated not explicitly set
            {
                Timestamp now = new Timestamp(System.currentTimeMillis());
                set_ValueNoCheck("Updated", now);
                if (withValues)
                {
                    sql.append(",Updated=").append(DB.TO_DATE(now, false));
                }
                else
                {
                    sql.append(",Updated=?");
                    params.add(now);
                }
            }
            if (!updatedBy)	//	UpdatedBy not explicitly set
            {
                int AD_User_ID = Env.getContextAsInt(p_ctx, "#AD_User_ID");
                set_ValueNoCheck("UpdatedBy", new Integer(AD_User_ID));
                if (withValues)
                {
                    sql.append(",UpdatedBy=").append(AD_User_ID);
                }
                else
                {
                    sql.append(",UpdatedBy=?");
                    params.add(AD_User_ID);
                }
            }
            sql.append(" WHERE ").append(where);
            /** @todo status locking goes here */

            if (log.isLoggable(Level.FINEST)) log.finest(sql.toString());
            int no = 0;
            if (isUseTimeoutForUpdate())
                no = withValues ? DB.executeUpdateEx(sql.toString(), m_trxName, QUERY_TIME_OUT)
                        : DB.executeUpdateEx(sql.toString(), params.toArray(), m_trxName, QUERY_TIME_OUT);
            else
                no = withValues ? DB.executeUpdate(sql.toString(), m_trxName)
                        : DB.executeUpdate(sql.toString(), params.toArray(), false, m_trxName);
            boolean ok = no == 1;
            if (ok)
                ok = lobSave();
            else
            {
                if (m_trxName == null)
                    log.saveError("SaveError", "Update return " + no + " instead of 1"
                            + " - " + p_info.getTableName() + "." + where);
                else
                    log.saveError("SaveError", "Update return " + no + " instead of 1"
                            + " - [" + m_trxName + "] - " + p_info.getTableName() + "." + where);
            }
            return ok;
        }
        else
        {
            // nothing changed, so OK
            return true;
        }
    }


    /**
     *  Create New Record
     *  @return true if new record inserted
     */
    @Override
    protected boolean saveNew()
    {
        //  Set ID for single key - Multi-Key values need explicitly be set previously
        if (m_IDs.length == 1 && p_info.hasKeyColumn()
                && m_KeyColumns[0].endsWith("_ID"))	//	AD_Language, EntityType
        {
            int no = saveNew_getID();
            if (no <= 0)
                no = MSequence.getNextID(getAD_Client_ID(), p_info.getTableName(), m_trxName);
            // the primary key is not overwrite with the local sequence
            if (isReplication())
            {
                if (get_ID() > 0)
                {
                    no = get_ID();
                }
            }
            if (no <= 0)
            {
                log.severe("No NextID (" + no + ")");
                return saveFinish (true, false);
            }
            m_IDs[0] = new Integer(no);
            set_ValueNoCheck(m_KeyColumns[0], m_IDs[0]);
        }
        //uuid secondary key
        int uuidIndex = p_info.getColumnIndex(getUUIDColumnName());
        if (uuidIndex >= 0)
        {
            String value = (String)get_Value(uuidIndex);
            if (p_info.getColumn(uuidIndex).FieldLength == 36 && (value == null || value.length() == 0))
            {
                UUID uuid = UUID.randomUUID();
                set_ValueNoCheck(p_info.getColumnName(uuidIndex), uuid.toString());
            }
        }
        if (m_trxName == null) {
            if (log.isLoggable(Level.FINE)) log.fine(p_info.getTableName() + " - " + get_WhereClause(true));
        } else {
            if (log.isLoggable(Level.FINE)) log.fine("[" + m_trxName + "] - " + p_info.getTableName() + " - " + get_WhereClause(true));
        }

        //	Set new DocumentNo
        String columnName = "DocumentNo";
        int index = p_info.getColumnIndex(columnName);
        if (index != -1)
        {
            String value = (String)get_Value(index);
            if (value != null && value.startsWith("<") && value.endsWith(">"))
                value = null;
            if (value == null || value.length() == 0)
            {
                int dt = p_info.getColumnIndex("C_DocTypeTarget_ID");
                if (dt == -1)
                    dt = p_info.getColumnIndex("C_DocType_ID");
                if (dt != -1)		//	get based on Doc Type (might return null)
                    value = MSequence.getDocumentNo(get_ValueAsInt(dt), m_trxName, false, this);
                if (value == null)	//	not overwritten by DocType and not manually entered
                    value = MSequence.getDocumentNo(getAD_Client_ID(), p_info.getTableName(), m_trxName, this);
                set_ValueNoCheck(columnName, value);
            }
        }
        // ticket 1007459 - exclude M_AttributeInstance from filling Value column
        if (! MAttributeInstance.Table_Name.equals(get_TableName())) {
            //	Set empty Value
            columnName = "Value";
            index = p_info.getColumnIndex(columnName);
            if (index != -1)
            {
                if (!p_info.isVirtualColumn(index))
                {
                    String value = (String)get_Value(index);
                    if (value == null || value.length() == 0)
                    {
                        value = MSequence.getDocumentNo (getAD_Client_ID(), p_info.getTableName(), m_trxName, this);
                        set_ValueNoCheck(columnName, value);
                    }
                }
            }
        }

        boolean ok = doInsert(isLogSQLScript());
        return saveFinish (true, ok);
    }   //  saveNew

    @Override
    protected boolean doInsert(boolean withValues) {
        int index;
        lobReset();

        //	Change Log
        MSession session = MSession.get (p_ctx, false);
        if (session == null)
            log.fine("No Session found");
        int AD_ChangeLog_ID = 0;

        //params for insert statement
        List<Object> params = new ArrayList<Object>();

        //	SQL
        StringBuilder sqlInsert = new StringBuilder("INSERT INTO ");
        sqlInsert.append(p_info.getTableName()).append(" (");
        StringBuilder sqlValues = new StringBuilder(") VALUES (");
        int size = get_ColumnCount();
        boolean doComma = false;
        for (int i = 0; i < size; i++)
        {
            Object value = get_Value(i);
            //	Don't insert NULL values (allows Database defaults)
            if (value == null
                    || p_info.isVirtualColumn(i))
                continue;

            //	Display Type
            int dt = p_info.getColumnDisplayType(i);
            if (DisplayType.isLOB(dt))
            {
                lobAdd (value, i, dt);
                continue;
            }

            //	** add column **
            if (doComma)
            {
                sqlInsert.append(",");
                sqlValues.append(",");
            }
            else
                doComma = true;
            sqlInsert.append(p_info.getColumnName(i));
            //
            //  Based on class of definition, not class of value
            Class<?> c = p_info.getColumnClass(i);
            if (withValues)
            {
                try
                {
                    if (c == Object.class) //  may have need to deal with null values differently
                        sqlValues.append (saveNewSpecial (value, i));
                    else if (value == null || value.equals (Null.NULL))
                        sqlValues.append ("NULL");
                    else if (value instanceof Integer || value instanceof BigDecimal)
                        sqlValues.append (value);
                    else if (c == Boolean.class)
                    {
                        boolean bValue = false;
                        if (value instanceof Boolean)
                            bValue = ((Boolean)value).booleanValue();
                        else
                            bValue = "Y".equals(value);
                        sqlValues.append (encrypt(i,bValue ? "'Y'" : "'N'"));
                    }
                    else if (value instanceof Timestamp)
                        sqlValues.append (DB.TO_DATE ((Timestamp)encrypt(i,value), p_info.getColumnDisplayType (i) == DisplayType.Date));
                    else if (c == String.class)
                        sqlValues.append (encrypt(i,DB.TO_STRING ((String)value)));
                    else if (DisplayType.isLOB(dt))
                        sqlValues.append("null");		//	no db dependent stuff here
                    else
                        sqlValues.append (saveNewSpecial (value, i));
                }
                catch (Exception e)
                {
                    String msg = "";
                    if (m_trxName != null)
                        msg = "[" + m_trxName + "] - ";
                    msg += p_info.toString(i)
                            + " - Value=" + value
                            + "(" + (value==null ? "null" : value.getClass().getName()) + ")";
                    log.log(Level.SEVERE, msg, e);
                    throw new DBException(e);	//	fini
                }
            }
            else
            {
                if (value instanceof Timestamp && dt == DisplayType.Date)
                    sqlValues.append("trunc(cast(? as date))");
                else
                    sqlValues.append("?");

                if (DisplayType.isLOB(dt))
                {
                    params.add(null);
                }
                else if (value == null || value.equals (Null.NULL))
                {
                    params.add(null);
                }
                else if (c == Boolean.class)
                {
                    boolean bValue = false;
                    if (value instanceof Boolean)
                        bValue = ((Boolean)value).booleanValue();
                    else
                        bValue = "Y".equals(value);
                    params.add(encrypt(i,bValue ? "Y" : "N"));
                }
                else if (c == String.class)
                {
                    if (value.toString().length() == 0)
                    {
                        params.add(null);
                    }
                    else
                    {
                        params.add(encrypt(i,value));
                    }
                }
                else
                {
                    params.add(value);
                }
            }

            //	Change Log	- Only
            String insertLog = MSysConfig.getValue(MSysConfig.SYSTEM_INSERT_CHANGELOG, "Y", getAD_Client_ID());
            if (   session != null
                    && m_IDs.length == 1
                    && p_info.isAllowLogging(i)		//	logging allowed
                    && !p_info.isEncrypted(i)		//	not encrypted
                    && !p_info.isVirtualColumn(i)	//	no virtual column
                    && !"Password".equals(p_info.getColumnName(i))
                    && (insertLog.equalsIgnoreCase("Y")
                    || (insertLog.equalsIgnoreCase("K") && p_info.getColumn(i).IsKey))
                    )
            {
                // change log on new
                MChangeLog cLog = session.changeLog (
                        m_trxName, AD_ChangeLog_ID,
                        p_info.getAD_Table_ID(), p_info.getColumn(i).AD_Column_ID,
                        get_ID(), getAD_Client_ID(), getAD_Org_ID(), null, value, MChangeLog.EVENTCHANGELOG_Insert);
                if (cLog != null)
                    AD_ChangeLog_ID = cLog.getAD_ChangeLog_ID();
            }

        }
        //	Custom Columns
        if (m_custom != null)
        {
            Iterator<String> it = m_custom.keySet().iterator();
            while (it.hasNext())
            {
                String column = (String)it.next();
                index = p_info.getColumnIndex(column);
                String value = (String)m_custom.get(column);
                if (value == null)
                    continue;
                if (doComma)
                {
                    sqlInsert.append(",");
                    sqlValues.append(",");
                }
                else
                    doComma = true;
                sqlInsert.append(column);
                if (withValues)
                {
                    sqlValues.append(encrypt(index, value));
                }
                else
                {
                    sqlValues.append("?");
                    if (value == null || value.toString().length() == 0)
                    {
                        params.add(null);
                    }
                    else
                    {
                        params.add(encrypt(index, value));
                    }
                }
            }
            m_custom = null;
        }
        sqlInsert.append(sqlValues)
                .append(")");
        //
        int no = withValues ? DB.executeUpdate(sqlInsert.toString(), m_trxName)
                : DB.executeUpdate(sqlInsert.toString(), params.toArray(), false, m_trxName);
        boolean ok = no == 1;
        if (ok)
        {
            ok = lobSave();
            if (!load(m_trxName))		//	re-read Info
            {
                if (m_trxName == null)
                    log.log(Level.SEVERE, "reloading");
                else
                    log.log(Level.SEVERE, "[" + m_trxName + "] - reloading");
                ok = false;;
            }
        }
        else
        {
            String msg = "Not inserted - ";
            if (CLogMgt.isLevelFiner())
                msg += sqlInsert.toString();
            else
                msg += get_TableName();
            if (m_trxName == null)
                log.log(Level.WARNING, msg);
            else
                log.log(Level.WARNING, "[" + m_trxName + "]" + msg);
        }
        return ok;
    }


    /**************************************************************************
     * 	Delete Current Record
     * 	@param force delete also processed records
     * 	@return true if deleted
     */
    @Override
    public boolean delete (boolean force)
    {
        checkValidContext();
        CLogger.resetLast();
        if (is_new())
            return true;

        int AD_Table_ID = p_info.getAD_Table_ID();
        int Record_ID = get_ID();

        if (!force)
        {
            int iProcessed = get_ColumnIndex("Processed");
            if  (iProcessed != -1)
            {
                Boolean processed = (Boolean)get_Value(iProcessed);
                if (processed != null && processed.booleanValue())
                {
                    log.warning("Record processed");	//	CannotDeleteTrx
                    log.saveError("Processed", "Processed", false);
                    return false;
                }
            }	//	processed
        }	//	force

        // Carlos Ruiz - globalqss - IDEMPIERE-111
        // Check if the role has access to this client
        // Don't check role System as webstore works with this role - see IDEMPIERE-401
        if ((Env.getAD_Role_ID(getCtx()) != 0) && !MRole.getDefault().isClientAccess(getAD_Client_ID(), true))
        {
            log.warning("You cannot delete this record, role doesn't have access");
            log.saveError("AccessCannotDelete", "", false);
            return false;
        }

        Trx localTrx = null;
        Trx trx = null;
        Savepoint savepoint = null;
        boolean success = false;
        try
        {

            String localTrxName = m_trxName;
            if (localTrxName == null)
            {
                localTrxName = Trx.createTrxName("POdel");
                localTrx = Trx.get(localTrxName, true);
                localTrx.setDisplayName(getClass().getName()+"_delete");
                localTrx.getConnection();
                m_trxName = localTrxName;
            }
            else
            {
                trx = Trx.get(m_trxName, false);
                if (trx == null)
                {
                    // Using a trx that was previously closed or never opened
                    // Creating and starting the transaction right here, but please note
                    // that this is not a good practice
                    trx = Trx.get(m_trxName, true);
                    log.severe("Transaction closed or never opened ("+m_trxName+") => starting now --> " + toString());
                }
            }

            try
            {
                // If not a localTrx we need to set a savepoint for rollback
                if (localTrx == null)
                    savepoint = trx.setSavepoint(null);

                if (!beforeDelete())
                {
                    log.warning("beforeDelete failed");
                    if (localTrx != null)
                    {
                        localTrx.rollback();
                    }
                    else if (savepoint != null)
                    {
                        try {
                            trx.rollback(savepoint);
                        } catch (SQLException e) {}
                        savepoint = null;
                    }
                    return false;
                }
            }
            catch (Exception e)
            {
                log.log(Level.WARNING, "beforeDelete", e);
                String msg = DBException.getDefaultDBExceptionMessage(e);
                log.saveError(msg != null ? msg : "Error", e, false);
                if (localTrx != null)
                {
                    localTrx.rollback();
                }
                else if (savepoint != null)
                {
                    try {
                        trx.rollback(savepoint);
                    } catch (SQLException e1) {}
                    savepoint = null;
                }
                return false;
            }
            //	Delete Restrict AD_Table_ID/Record_ID (Requests, ..)
            String errorMsg = PO_Record.exists(AD_Table_ID, Record_ID, m_trxName);
            if (errorMsg != null)
            {
                log.saveError("CannotDelete", errorMsg);
                if (localTrx != null)
                {
                    localTrx.rollback();
                }
                else if (savepoint != null)
                {
                    try {
                        trx.rollback(savepoint);
                    } catch (SQLException e) {}
                    savepoint = null;
                }
                return false;
            }
            // Call ModelValidators TYPE_DELETE
            errorMsg = ModelValidationEngine.get().fireModelChange
                    (this, isReplication() ? ModelValidator.TYPE_BEFORE_DELETE_REPLICATION : ModelValidator.TYPE_DELETE);
            setReplication(false); // @Trifon
            if (errorMsg != null)
            {
                log.saveError("Error", errorMsg);
                if (localTrx != null)
                {
                    localTrx.rollback();
                }
                else if (savepoint != null)
                {
                    try {
                        trx.rollback(savepoint);
                    } catch (SQLException e) {}
                    savepoint = null;
                }
                return false;
            }

            try
            {
                //
                deleteTranslations(localTrxName);
                if (get_ColumnIndex("IsSummary") >= 0) {
                    delete_Tree(MTree_Base.TREETYPE_CustomTable);
                }
                //	Delete Cascade AD_Table_ID/Record_ID (Attachments, ..)
                PO_Record.deleteCascade(AD_Table_ID, Record_ID, localTrxName);

                //delete cascade only for single key column record
                if (m_KeyColumns != null && m_KeyColumns.length == 1) {
                    PO_Record.deleteModelCascade(p_info.getTableName(), Record_ID, localTrxName);
                }

                //	The Delete Statement
                StringBuilder sql = new StringBuilder ("DELETE FROM ") //jz why no FROM??
                        .append(p_info.getTableName())
                        .append(" WHERE ")
                        .append(get_WhereClause(true));
                int no = 0;
                if (isUseTimeoutForUpdate())
                    no = DB.executeUpdateEx(sql.toString(), localTrxName, QUERY_TIME_OUT);
                else
                    no = DB.executeUpdate(sql.toString(), localTrxName);
                success = no == 1;
            }
            catch (Exception e)
            {
                String msg = DBException.getDefaultDBExceptionMessage(e);
                log.saveError(msg != null ? msg : e.getLocalizedMessage(), e);
                success = false;
            }

            //	Save ID
            m_idOld = get_ID();
            //
            if (!success)
            {
                log.warning("Not deleted");
                if (localTrx != null)
                {
                    localTrx.rollback();
                }
                else if (savepoint != null)
                {
                    try {
                        trx.rollback(savepoint);
                    } catch (SQLException e) {}
                    savepoint = null;
                }
            }
            else
            {
                if (success)
                {
                    if( p_info.isChangeLog())
                    {
                        //	Change Log
                        MSession session = MSession.get (p_ctx, false);
                        if (session == null)
                            log.fine("No Session found");
                        else if (m_IDs.length == 1)
                        {
                            int AD_ChangeLog_ID = 0;
                            int size = get_ColumnCount();
                            for (int i = 0; i < size; i++)
                            {
                                Object value = m_oldValues[i];
                                if (value != null
                                        && p_info.isAllowLogging(i)		//	logging allowed
                                        && !p_info.isEncrypted(i)		//	not encrypted
                                        && !p_info.isVirtualColumn(i)	//	no virtual column
                                        && !"Password".equals(p_info.getColumnName(i))
                                        )
                                {
                                    // change log on delete
                                    MChangeLog cLog = session.changeLog (
                                            m_trxName != null ? m_trxName : localTrxName, AD_ChangeLog_ID,
                                            AD_Table_ID, p_info.getColumn(i).AD_Column_ID,
                                            Record_ID, getAD_Client_ID(), getAD_Org_ID(), value, null, MChangeLog.EVENTCHANGELOG_Delete);
                                    if (cLog != null)
                                        AD_ChangeLog_ID = cLog.getAD_ChangeLog_ID();
                                }
                            }	//   for all fields
                        }

                        //	Housekeeping
                        m_IDs[0] = I_ZERO;
                        if (m_trxName == null)
                            log.fine("complete");
                        else
                        if (log.isLoggable(Level.FINE)) log.fine("[" + m_trxName + "] - complete");
                        m_attachment = null;
                    }
                }
                else
                {
                    log.warning("Not deleted");
                }
            }

            try
            {
                success = afterDelete (success);
            }
            catch (Exception e)
            {
                log.log(Level.WARNING, "afterDelete", e);
                String msg = DBException.getDefaultDBExceptionMessage(e);
                log.saveError(msg != null ? msg : "Error", e, false);
                success = false;
                //	throw new DBException(e);
            }

            // Call ModelValidators TYPE_AFTER_DELETE - teo_sarca [ 1675490 ]
            if (success) {
                errorMsg = ModelValidationEngine.get().fireModelChange(this, ModelValidator.TYPE_AFTER_DELETE);
                if (errorMsg != null) {
                    log.saveError("Error", errorMsg);
                    success = false;
                }
            }

            if (!success)
            {
                if (localTrx != null)
                {
                    localTrx.rollback();
                }
                else if (savepoint != null)
                {
                    try {
                        trx.rollback(savepoint);
                    } catch (SQLException e) {}
                    savepoint = null;
                }
            }
            else
            {
                if (localTrx != null)
                {
                    try {
                        localTrx.commit(true);
                    } catch (SQLException e) {
                        String msg = DBException.getDefaultDBExceptionMessage(e);
                        log.saveError(msg != null ? msg : "Error", e);
                        success = false;
                    }
                }
            }

            //	Reset
            if (success)
            {
                //osgi event handler
                Event event = EventManager.newEvent(IEventTopics.PO_POST_DELETE, this);
                EventManager.getInstance().postEvent(event);

                m_idOld = 0;
                int size = p_info.getColumnCount();
                m_oldValues = new Object[size];
                m_newValues = new Object[size];
                CacheMgt.get().reset(p_info.getTableName());
            }
        }
        finally
        {
            if (localTrx != null)
            {
                localTrx.close();
                m_trxName = null;
            }
            else
            {
                if (savepoint != null)
                {
                    try {
                        trx.releaseSavepoint(savepoint);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                savepoint = null;
                trx = null;
            }
        }
        return success;
    }	//	delete


    /**
     * 	Insert (missing) Translation Records
     * 	@return false if error (true if no translation or success)
     */
    protected boolean insertTranslations()
    {
        //	Not a translation table
        if (m_IDs.length > 1
                || m_IDs[0].equals(I_ZERO)
                || !(m_IDs[0] instanceof Integer)
                || !p_info.isTranslated())
            return true;
        //
        StringBuilder iColumns = new StringBuilder();
        StringBuilder sColumns = new StringBuilder();
        for (int i = 0; i < p_info.getColumnCount(); i++)
        {
            if (p_info.isColumnTranslated(i))
            {
                iColumns.append(p_info.getColumnName(i))
                        .append(",");
                sColumns.append("t.")
                        .append(p_info.getColumnName(i))
                        .append(",");
            }
        }
        if (iColumns.length() == 0)
            return true;

        String tableName = p_info.getTableName();
        String keyColumn = m_KeyColumns[0];

        //check whether db have working generate_uuid function.
        boolean uuidFunction = DB.isGenerateUUIDSupported();

        //uuid column
        int uuidColumnId = DB.getSQLValue(get_TrxName(), "SELECT col.AD_Column_ID FROM AD_Column col INNER JOIN AD_Table tbl ON col.AD_Table_ID = tbl.AD_Table_ID WHERE tbl.TableName=? AND col.ColumnName=?",
                tableName+"_Trl", org.idempiere.orm.PO.getUUIDColumnName(tableName+"_Trl"));

        StringBuilder sql = new StringBuilder ("INSERT INTO ")
                .append(tableName).append("_Trl (AD_Language,")
                .append(keyColumn).append(", ")
                .append(iColumns)
                .append(" IsTranslated,AD_Client_ID,AD_Org_ID,Created,Createdby,Updated,UpdatedBy");
        if (uuidColumnId > 0 && uuidFunction)
            sql.append(",").append(org.idempiere.orm.PO.getUUIDColumnName(tableName+"_Trl")).append(" ) ");
        else
            sql.append(" ) ");
        sql.append("SELECT l.AD_Language,t.")
                .append(keyColumn).append(", ")
                .append(sColumns)
                .append(" 'N',t.AD_Client_ID,t.AD_Org_ID,t.Created,t.Createdby,t.Updated,t.UpdatedBy");
        if (uuidColumnId > 0 && uuidFunction)
            sql.append(",Generate_UUID() ");
        else
            sql.append(" ");
        sql.append("FROM AD_Language l, ").append(tableName).append(" t ")
                .append("WHERE l.IsActive='Y' AND l.IsSystemLanguage='Y' AND l.IsBaseLanguage='N' AND t.")
                .append(keyColumn).append("=").append(get_ID())
                .append(" AND NOT EXISTS (SELECT * FROM ").append(tableName)
                .append("_Trl tt WHERE tt.AD_Language=l.AD_Language AND tt.")
                .append(keyColumn).append("=t.").append(keyColumn).append(")");
        int no = DB.executeUpdate(sql.toString(), m_trxName);
        if (uuidColumnId > 0 && !uuidFunction) {
            MColumn column = new MColumn(getCtx(), uuidColumnId, get_TrxName());
            UUIDGenerator.updateUUID(column, get_TrxName());
        }
        if (log.isLoggable(Level.FINE)) log.fine("#" + no);
        return no > 0;
    }	//	insertTranslations

    /**
     * 	Update Translations.
     * 	@return false if error (true if no translation or success)
     */
    protected boolean updateTranslations()
    {
        //	Not a translation table
        if (m_IDs.length > 1
                || m_IDs[0].equals(I_ZERO)
                || !(m_IDs[0] instanceof Integer)
                || !p_info.isTranslated())
            return true;

        String tableName = p_info.getTableName();
        if (tableName.startsWith("AD") && getAD_Client_ID() == 0)
            return true;

        //
        boolean trlColumnChanged = false;
        for (int i = 0; i < p_info.getColumnCount(); i++)
        {
            if (p_info.isColumnTranslated(i)
                    && is_ValueChanged(p_info.getColumnName(i)))
            {
                trlColumnChanged = true;
                break;
            }
        }
        if (!trlColumnChanged)
            return true;
        //
        MClient client = MClient.get(getCtx());
        //
        String keyColumn = m_KeyColumns[0];
        StringBuilder sqlupdate = new StringBuilder("UPDATE ")
                .append(tableName).append("_Trl SET ");

        //
        StringBuilder sqlcols = new StringBuilder();
        for (int i = 0; i < p_info.getColumnCount(); i++)
        {
            String columnName = p_info.getColumnName(i);
            if (p_info.isColumnTranslated(i)
                    && is_ValueChanged(columnName))
            {
                sqlcols.append(columnName).append("=");
                Object value = get_Value(columnName);
                if (value == null)
                    sqlcols.append("NULL");
                else if (value instanceof String)
                    sqlcols.append(DB.TO_STRING((String)value));
                else if (value instanceof Boolean)
                    sqlcols.append(((Boolean)value).booleanValue() ? "'Y'" : "'N'");
                else if (value instanceof Timestamp)
                    sqlcols.append(DB.TO_DATE((Timestamp)value));
                else
                    sqlcols.append(value.toString());
                sqlcols.append(",");

                // Reset of related translation cache entries
                String[] availableLanguages = Language.getNames();
                for (String langName : availableLanguages) {
                    Language language = Language.getLanguage(langName);
                    String key = getTrlCacheKey(columnName, language.getAD_Language());
                    trl_cache.remove(key);
                }
            }
        }
        StringBuilder whereid = new StringBuilder(" WHERE ").append(keyColumn).append("=").append(get_ID());
        StringBuilder andlang = new StringBuilder(" AND AD_Language=").append(DB.TO_STRING(client.getAD_Language()));
        StringBuilder andnotlang = new StringBuilder(" AND AD_Language!=").append(DB.TO_STRING(client.getAD_Language()));
        int no = -1;

        if (client.isMultiLingualDocument()) {
            String baselang = Language.getBaseAD_Language();
            if (client.getAD_Language().equals(baselang)) {
                // tenant language = base language
                // set all translations as untranslated
                StringBuilder sqlexec = new StringBuilder()
                        .append(sqlupdate)
                        .append("IsTranslated='N'")
                        .append(whereid);
                no = DB.executeUpdate(sqlexec.toString(), m_trxName);
                if (log.isLoggable(Level.FINE)) log.fine("#" + no);
            } else {
                // tenant language <> base language
                // auto update translation for tenant language
                StringBuilder sqlexec = new StringBuilder()
                        .append(sqlupdate)
                        .append(sqlcols)
                        .append("IsTranslated='Y'")
                        .append(whereid)
                        .append(andlang);
                no = DB.executeUpdate(sqlexec.toString(), m_trxName);
                if (log.isLoggable(Level.FINE)) log.fine("#" + no);
                if (no >= 0) {
                    // set other translations as untranslated
                    sqlexec = new StringBuilder()
                            .append(sqlupdate)
                            .append("IsTranslated='N'")
                            .append(whereid)
                            .append(andnotlang);
                    no = DB.executeUpdate(sqlexec.toString(), m_trxName);
                    if (log.isLoggable(Level.FINE)) log.fine("#" + no);
                }
            }

        } else {
            // auto update all translations
            StringBuilder sqlexec = new StringBuilder()
                    .append(sqlupdate)
                    .append(sqlcols)
                    .append("IsTranslated='Y'")
                    .append(whereid);
            no = DB.executeUpdate(sqlexec.toString(), m_trxName);
            if (log.isLoggable(Level.FINE)) log.fine("#" + no);
        }
        return no >= 0;
    }	//	updateTranslations

    /**
     * 	Insert Accounting Records
     *	@param acctTable accounting sub table
     *	@param acctBaseTable acct table to get data from
     *	@param whereClause optional where clause with alias "p" for acctBaseTable
     *	@return true if records inserted
     */
    protected boolean insert_Accounting (String acctTable,
                                         String acctBaseTable, String whereClause)
    {
        if (s_acctColumns == null	//	cannot cache C_BP_*_Acct as there are 3
                || acctTable.startsWith("C_BP_"))
        {
            s_acctColumns = new ArrayList<String>();
            String sql = "SELECT c.ColumnName "
                    + "FROM AD_Column c INNER JOIN AD_Table t ON (c.AD_Table_ID=t.AD_Table_ID) "
                    + "WHERE t.TableName=? AND c.IsActive='Y' AND c.AD_Reference_ID=25 ORDER BY c.ColumnName";
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try
            {
                pstmt = DB.prepareStatement (sql, null);
                pstmt.setString (1, acctTable);
                rs = pstmt.executeQuery ();
                while (rs.next ())
                    s_acctColumns.add (rs.getString(1));
            }
            catch (Exception e)
            {
                log.log(Level.SEVERE, acctTable, e);
            }
            finally {
                DB.close(rs, pstmt);
                rs = null; pstmt = null;
            }
            if (s_acctColumns.size() == 0)
            {
                log.severe ("No Columns for " + acctTable);
                return false;
            }
        }

        //	Create SQL Statement - INSERT
        StringBuilder sb = new StringBuilder("INSERT INTO ")
                .append(acctTable)
                .append(" (").append(get_TableName())
                .append("_ID, C_AcctSchema_ID, AD_Client_ID,AD_Org_ID,IsActive, Created,CreatedBy,Updated,UpdatedBy ");
        for (int i = 0; i < s_acctColumns.size(); i++)
            sb.append(",").append(s_acctColumns.get(i));

        //check whether db have working generate_uuid function.
        boolean uuidFunction = DB.isGenerateUUIDSupported();

        //uuid column
        int uuidColumnId = DB.getSQLValue(get_TrxName(), "SELECT col.AD_Column_ID FROM AD_Column col INNER JOIN AD_Table tbl ON col.AD_Table_ID = tbl.AD_Table_ID WHERE tbl.TableName=? AND col.ColumnName=?",
                acctTable, org.idempiere.orm.PO.getUUIDColumnName(acctTable));
        if (uuidColumnId > 0 && uuidFunction)
            sb.append(",").append(org.idempiere.orm.PO.getUUIDColumnName(acctTable));
        //	..	SELECT
        sb.append(") SELECT ").append(get_ID())
                .append(", p.C_AcctSchema_ID, p.AD_Client_ID,0,'Y', SysDate,")
                .append(getUpdatedBy()).append(",SysDate,").append(getUpdatedBy());
        for (int i = 0; i < s_acctColumns.size(); i++)
            sb.append(",p.").append(s_acctColumns.get(i));
        //uuid column
        if (uuidColumnId > 0 && uuidFunction)
            sb.append(",generate_uuid()");
        //	.. 	FROM
        sb.append(" FROM ").append(acctBaseTable)
                .append(" p WHERE p.AD_Client_ID=").append(getAD_Client_ID());
        if (whereClause != null && whereClause.length() > 0)
            sb.append (" AND ").append(whereClause);
        sb.append(" AND NOT EXISTS (SELECT * FROM ").append(acctTable)
                .append(" e WHERE e.C_AcctSchema_ID=p.C_AcctSchema_ID AND e.")
                .append(get_TableName()).append("_ID=").append(get_ID()).append(")");
        //
        int no = DB.executeUpdate(sb.toString(), get_TrxName());
        if (no > 0) {
            if (log.isLoggable(Level.FINE)) log.fine("#" + no);
        } else {
            log.warning("#" + no
                    + " - Table=" + acctTable + " from " + acctBaseTable);
        }

        //fall back to the slow java client update code
        if (uuidColumnId > 0 && !uuidFunction) {
            MColumn column = new MColumn(getCtx(), uuidColumnId, get_TrxName());
            UUIDGenerator.updateUUID(column, get_TrxName());
        }
        return no > 0;
    }	//	insert_Accounting

    /**
     * 	Insert id data into Tree
     * 	@param treeType MTree TREETYPE_*
     * 	@param C_Element_ID element for accounting element values
     *	@return true if inserted
     */
    @Override
    protected boolean insert_Tree (String treeType, int C_Element_ID)
    {
        String tableName = MTree_Base.getNodeTableName(treeType);

        //check whether db have working generate_uuid function.
        boolean uuidFunction = DB.isGenerateUUIDSupported();

        //uuid column
        int uuidColumnId = DB.getSQLValue(get_TrxName(), "SELECT col.AD_Column_ID FROM AD_Column col INNER JOIN AD_Table tbl ON col.AD_Table_ID = tbl.AD_Table_ID WHERE tbl.TableName=? AND col.ColumnName=?",
                tableName, org.idempiere.orm.PO.getUUIDColumnName(tableName));

        StringBuilder sb = new StringBuilder ("INSERT INTO ")
                .append(tableName)
                .append(" (AD_Client_ID,AD_Org_ID, IsActive,Created,CreatedBy,Updated,UpdatedBy, "
                        + "AD_Tree_ID, Node_ID, Parent_ID, SeqNo");
        if (uuidColumnId > 0 && uuidFunction)
            sb.append(", ").append(org.idempiere.orm.PO.getUUIDColumnName(tableName)).append(") ");
        else
            sb.append(") ");
        sb.append("SELECT t.AD_Client_ID, 0, 'Y', SysDate, "+getUpdatedBy()+", SysDate, "+getUpdatedBy()+","
                + "t.AD_Tree_ID, ").append(get_ID()).append(", 0, 999");
        if (uuidColumnId > 0 && uuidFunction)
            sb.append(", Generate_UUID() ");
        else
            sb.append(" ");
        sb.append("FROM AD_Tree t "
                + "WHERE t.AD_Client_ID=").append(getAD_Client_ID()).append(" AND t.IsActive='Y'");
        //	Account Element Value handling
        if (C_Element_ID != 0)
            sb.append(" AND EXISTS (SELECT * FROM C_Element ae WHERE ae.C_Element_ID=")
                    .append(C_Element_ID).append(" AND t.AD_Tree_ID=ae.AD_Tree_ID)");
        else	//	std trees
            sb.append(" AND t.IsAllNodes='Y' AND t.TreeType='").append(treeType).append("'");
        if (MTree_Base.TREETYPE_CustomTable.equals(treeType))
            sb.append(" AND t.AD_Table_ID=").append(get_Table_ID());
        //	Duplicate Check
        sb.append(" AND NOT EXISTS (SELECT * FROM " + MTree_Base.getNodeTableName(treeType) + " e "
                + "WHERE e.AD_Tree_ID=t.AD_Tree_ID AND Node_ID=").append(get_ID()).append(")");
        int no = DB.executeUpdate(sb.toString(), get_TrxName());
        if (no > 0) {
            if (log.isLoggable(Level.FINE)) log.fine("#" + no + " - TreeType=" + treeType);
        } else {
            if (! MTree_Base.TREETYPE_CustomTable.equals(treeType))
                log.warning("#" + no + " - TreeType=" + treeType);
        }

        if (uuidColumnId > 0 && !uuidFunction )
        {
            MColumn column = new MColumn(getCtx(), uuidColumnId, get_TrxName());
            UUIDGenerator.updateUUID(column, get_TrxName());
        }
        return no > 0;
    }	//	insert_Tree

    /**
     * 	Update parent key and seqno based on value if the tree is driven by value
     * 	@param treeType MTree TREETYPE_*
     *	@return true if inserted
     */
    @Override
    public void update_Tree (String treeType)
    {
        int idxValueCol = get_ColumnIndex("Value");
        if (idxValueCol < 0)
            return;
        int idxValueIsSummary = get_ColumnIndex("IsSummary");
        if (idxValueIsSummary < 0)
            return;
        String value = get_Value(idxValueCol).toString();
        if (value == null)
            return;

        String tableName = MTree_Base.getNodeTableName(treeType);
        String sourceTableName;
        String whereTree;
        Object[] parameters;
        if (MTree_Base.TREETYPE_CustomTable.equals(treeType)) {
            sourceTableName = this.get_TableName();
            whereTree = "TreeType=? AND AD_Table_ID=?";
            parameters = new Object[]{treeType, this.get_Table_ID()};
        } else {
            sourceTableName = MTree_Base.getSourceTableName(treeType);
            if (MTree_Base.TREETYPE_ElementValue.equals(treeType) && this instanceof I_C_ElementValue) {
                whereTree = "TreeType=? AND AD_Tree_ID=?";
                parameters = new Object[]{treeType, ((I_C_ElementValue)this).getC_Element().getAD_Tree_ID()};
            } else {
                whereTree = "TreeType=?";
                parameters = new Object[]{treeType};
            }
        }
        String updateSeqNo = "UPDATE " + tableName + " SET SeqNo=SeqNo+1 WHERE Parent_ID=? AND SeqNo>=? AND AD_Tree_ID=?";
        String update = "UPDATE " + tableName + " SET SeqNo=?, Parent_ID=? WHERE Node_ID=? AND AD_Tree_ID=?";
        String selMinSeqNo = "SELECT COALESCE(MIN(tn.SeqNo),-1) FROM AD_TreeNode tn JOIN " + sourceTableName + " n ON (tn.Node_ID=n." + sourceTableName + "_ID) WHERE tn.Parent_ID=? AND tn.AD_Tree_ID=? AND n.Value>?";
        String selMaxSeqNo = "SELECT COALESCE(MAX(tn.SeqNo)+1,999) FROM AD_TreeNode tn JOIN " + sourceTableName + " n ON (tn.Node_ID=n." + sourceTableName + "_ID) WHERE tn.Parent_ID=? AND tn.AD_Tree_ID=? AND n.Value<?";

        List<MTree_Base> trees = new Query(getCtx(), MTree_Base.Table_Name, whereTree, get_TrxName())
                .setClient_ID()
                .setOnlyActiveRecords(true)
                .setParameters(parameters)
                .list();

        for (MTree_Base tree : trees) {
            if (tree.isTreeDrivenByValue()) {
                int newParentID = -1;
                if (I_C_ElementValue.Table_Name.equals(sourceTableName)) {
                    newParentID = retrieveIdOfElementValue(value, getAD_Client_ID(), ((I_C_ElementValue)this).getC_Element().getC_Element_ID(), get_TrxName());
                } else {
                    newParentID = retrieveIdOfParentValue(value, sourceTableName, getAD_Client_ID(), get_TrxName());
                }
                int seqNo = DB.getSQLValueEx(get_TrxName(), selMinSeqNo, newParentID, tree.getAD_Tree_ID(), value);
                if (seqNo == -1)
                    seqNo = DB.getSQLValueEx(get_TrxName(), selMaxSeqNo, newParentID, tree.getAD_Tree_ID(), value);
                DB.executeUpdateEx(updateSeqNo, new Object[] {newParentID, seqNo, tree.getAD_Tree_ID()}, get_TrxName());
                DB.executeUpdateEx(update, new Object[] {seqNo, newParentID, get_ID(), tree.getAD_Tree_ID()}, get_TrxName());
            }
        }
    }	//	update_Tree




    /* Doc - To be used on ModelValidator to get the corresponding Doc from the PO */
    private IDoc m_doc;

    /**
     *      Set the accounting document associated to the PO - for use in POST ModelValidator
     *      @param doc Document
     */
    public void setDoc(IDoc doc) {
        m_doc = doc;
    }

    /**
     *      Set the accounting document associated to the PO - for use in POST ModelValidator
     *      @return Doc Document
     */
    public IDoc getDoc() {
        return m_doc;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        PO clone = (PO) super.clone();
        clone.m_doc = null;
        clone.m_attachment = null;
        return clone;
    }

    protected void validateUniqueIndex()
    {
        ValueNamePair ppE = CLogger.retrieveError();
        if (ppE != null)
        {
            String msg = ppE.getValue();
            String info = ppE.getName();
            if ("DBExecuteError".equals(msg))
                info = "DBExecuteError:" + info;
            //	Unique Constraint
            Exception e = CLogger.retrieveException();
            if (DBException.isUniqueContraintError(e))
            {
                boolean found = false;
                String dbIndexName = DB.getDatabase().getNameOfUniqueConstraintError(e);
                if (log.isLoggable(Level.FINE)) log.fine("dbIndexName=" + dbIndexName);
                MTableIndex[] indexes = MTableIndex.get(MTable.get(getCtx(), get_Table_ID()));
                for (MTableIndex index : indexes)
                {
                    if (dbIndexName.equalsIgnoreCase(index.getName()))
                    {
                        if (index.getAD_Message_ID() > 0)
                        {
                            MMessage message = MMessage.get(getCtx(), index.getAD_Message_ID());
                            log.saveError("SaveError", Msg.getMsg(getCtx(), message.getValue()));
                            found = true;
                        }
                        break;
                    }
                }

                if (!found)
                    log.saveError(msg, info);
            }
            else
                log.saveError(msg, info);
        }
    }

    /**
     * 	Create New PO by Copying existing (key not copied).
     * 	@param ctx context
     * 	@param source source object
     * 	@param AD_Client_ID client
     * 	@param AD_Org_ID org
     */
    public PO(Properties ctx, PO source, int AD_Client_ID, int AD_Org_ID)
    {
        this (ctx, 0, null, null);	//	create new
        //
        if (source != null)
            copyValues (source, this);
        setAD_Client_ID(AD_Client_ID);
        setAD_Org_ID(AD_Org_ID);
    }	//	PO



    /**
     *  Get Lookup
     *  @param index index
     *  @return Lookup or null
     */
    protected Lookup get_ColumnLookup(int index)
    {
        return ((POInfo)p_info).getColumnLookup(index);
    }   //  getColumnLookup

    /**
     * 	Get Display Value of value
     *	@param columnName columnName
     *	@param currentValue current value
     *	@return String value with "./." as null
     */
    @Override
    public String get_DisplayValue(String columnName, boolean currentValue)
    {
        Object value = currentValue ? get_Value(columnName) : get_ValueOld(columnName);
        if (value == null)
            return "./.";
        String retValue = value.toString();
        int index = get_ColumnIndex(columnName);
        if (index < 0)
            return retValue;
        int dt = get_ColumnDisplayType(index);
        if (DisplayType.isText(dt) || DisplayType.YesNo == dt)
            return retValue;
        //	Lookup
        Lookup lookup = get_ColumnLookup(index);
        if (lookup != null)
            return lookup.getDisplay(value);
        //	Other
        return retValue;
    }	//	get_DisplayValue

    @Override
    protected boolean set_Value (String ColumnName, Object value) {
        return super.set_Value(ColumnName, value);
    }

    @Override
    protected void setClientOrg (int AD_Client_ID, int AD_Org_ID) {
        super.setClientOrg (AD_Client_ID, AD_Org_ID);
    }

    protected void setClientOrg (org.compiere.orm.PO po) {
        super.setClientOrg (po);
    }

    protected void setClientOrg (PO po) {
        super.setClientOrg (po);
    }

    /**************************************************************************
     * 	Set AD_Client
     * 	@param AD_Client_ID client
     */
    protected void setAD_Client_ID (int AD_Client_ID)
    {
        super.setAD_Client_ID(AD_Client_ID);
    }	//	setAD_Client_ID

    protected boolean set_Value (int index, Object value) {
        return super.set_Value(index, value);
    }

}
