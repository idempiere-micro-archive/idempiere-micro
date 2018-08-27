package org.compiere.orm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import org.compiere.model.I_C_ElementValue;
import org.compiere.util.Msg;
import org.idempiere.common.exceptions.AdempiereException;
import org.idempiere.common.util.AdempiereUserError;
import org.idempiere.common.util.CLogger;
import org.idempiere.common.util.CacheMgt;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;
import org.idempiere.common.util.Trx;
import org.idempiere.common.util.ValueNamePair;
import org.idempiere.icommon.model.IPO;
import org.idempiere.orm.EventManager;
import org.idempiere.orm.IEvent;
import org.idempiere.orm.IEventTopics;
import org.idempiere.orm.Null;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import org.idempiere.common.exceptions.DBException;

public abstract class PO extends org.idempiere.orm.PO {
    public PO(Properties ctx) {
        super(ctx);
    }

    public PO(Properties ctx, int ID, String trxName) {
        super(ctx, ID, trxName);
    }

    public PO(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName, null);
    }
    public PO(Properties ctx, ResultSet rs, String trxName, String columnNamePrefix) {
        super(ctx, rs, trxName, columnNamePrefix);
    }

    public PO(Properties ctx, int ID, String trxName, ResultSet rs) {
        super(ctx, ID, trxName, rs, null);
    }

    /**	Attachment with entries	*/
    protected MAttachment m_attachment = null;

    /**
     * 	Is new record
     *	@return true if new
     */
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

    /**
     * 	Overwrite Client Org if different
     *	@param AD_Client_ID client
     *	@param AD_Org_ID org
     */
    protected void setClientOrg (int AD_Client_ID, int AD_Org_ID)
    {
        if (AD_Client_ID != getAD_Client_ID())
            setAD_Client_ID(AD_Client_ID);
        if (AD_Org_ID != getAD_Org_ID())
            setAD_Org_ID(AD_Org_ID);
    }	//	setClientOrg

    /**************************************************************************
     * 	Set AD_Client
     * 	@param AD_Client_ID client
     */
    protected void setAD_Client_ID (int AD_Client_ID)
    {
        set_ValueNoCheck ("AD_Client_ID", new Integer(AD_Client_ID));
    }	//	setAD_Client_ID

    /**
     * 	Overwrite Client Org if different
     *	@param po persistent object
     */
    protected void setClientOrg (IPO po)
    {
        setClientOrg(po.getAD_Client_ID(), po.getAD_Org_ID());
    }	//	setClientOrg

    /**
     * Update Value or create new record.
     * @throws AdempiereException
     * @see #save()
     */
    public void saveEx() throws AdempiereException
    {
        if (!save()) {
            String msg = null;
            ValueNamePair err = CLogger.retrieveError();
            String val = err != null ? Msg.translate(getCtx(), err.getValue()) : "";
            if (err != null)
                msg = (val != null ? val + ": " : "") + err.getName();
            if (msg == null || msg.length() == 0)
                msg = "SaveError";
            throw new AdempiereException(msg);
        }
    }

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

        boolean ok = doInsert(isLogSQLScript());
        return saveFinish (true, ok);
    }   //  saveNew

    /**
     * 	Insert id data into Tree
     * 	@param treeType MTree TREETYPE_*
     *	@return true if inserted
     */
    protected boolean insert_Tree (String treeType)
    {
        return insert_Tree (treeType, 0);
    }	//	insert_Tree

    /**
     * 	Insert id data into Tree
     * 	@param treeType MTree TREETYPE_*
     * 	@param C_Element_ID element for accounting element values
     *	@return true if inserted
     */
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

        return no > 0;
    }	//	insert_Tree

    /**
     * 	Update parent key and seqno based on value if the tree is driven by value
     * 	@param treeType MTree TREETYPE_*
     *	@return true if inserted
     */
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

        List<X_AD_Tree> trees = new Query(getCtx(), MTree_Base.Table_Name, whereTree, get_TrxName())
                .setClient_ID()
                .setOnlyActiveRecords(true)
                .setParameters(parameters)
                .list();

        for (X_AD_Tree tree : trees) {
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

    /**
     * 	Delete ID Tree Nodes
     *	@param treeType MTree TREETYPE_*
     *	@return true if deleted
     */
    protected boolean delete_Tree (String treeType)
    {
        int id = get_ID();
        if (id == 0)
            id = get_IDOld();

        // IDEMPIERE-2453
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM ")
                .append(MTree_Base.getNodeTableName(treeType))
                .append(" n JOIN AD_Tree t ON n.AD_Tree_ID=t.AD_Tree_ID")
                .append(" WHERE Parent_ID=? AND t.TreeType=?");
        if (MTree_Base.TREETYPE_CustomTable.equals(treeType))
            countSql.append(" AND t.AD_Table_ID=").append(get_Table_ID());
        int cnt = DB.getSQLValueEx( get_TrxName(), countSql.toString(), id, treeType);
        if (cnt > 0)
            throw new AdempiereException(Msg.getMsg(Env.getCtx(),"NoParentDelete", new Object[] {cnt}));

        StringBuilder sb = new StringBuilder ("DELETE FROM ")
                .append(MTree_Base.getNodeTableName(treeType))
                .append(" n WHERE Node_ID=").append(id)
                .append(" AND EXISTS (SELECT * FROM AD_Tree t "
                        + "WHERE t.AD_Tree_ID=n.AD_Tree_ID AND t.TreeType='")
                .append(treeType).append("'");
        if (MTree_Base.TREETYPE_CustomTable.equals(treeType))
            sb.append(" AND t.AD_Table_ID=").append(get_Table_ID());
        sb.append(")");
        int no = DB.executeUpdate(sb.toString(), get_TrxName());
        if (no > 0) {
            if (log.isLoggable(Level.FINE)) log.fine("#" + no + " - TreeType=" + treeType);
        } else {
            if (! MTree_Base.TREETYPE_CustomTable.equals(treeType))
                log.warning("#" + no + " - TreeType=" + treeType);
        }
        return no > 0;
    }	//	delete_Tree

    /**
     *  Update Value or create new record.
     * 	To reload call load() - not updated
     *	@param trxName transaction
     *  @return true if saved
     */
    public boolean save (String trxName)
    {
        set_TrxName(trxName);
        return save();
    }	//	save

    /**************************************************************************
     * 	Delete Current Record
     * 	@param force delete also processed records
     * 	@return true if deleted
     */
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
            setReplication(false); // @Trifon

            try
            {
                //
                deleteTranslations(localTrxName);
                if (get_ColumnIndex("IsSummary") >= 0) {
                    delete_Tree(MTree_Base.TREETYPE_CustomTable);
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
                IEvent event = EventManager.newEvent(IEventTopics.PO_POST_DELETE, this);
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
     * 	Delete Current Record
     * 	@param force delete also processed records
     *	@param trxName transaction
     *	@return true if deleted
     */
    public boolean delete (boolean force, String trxName)
    {
        set_TrxName(trxName);
        return delete (force);
    }	//	delete

    /**
     * Update Value or create new record.
     * @param trxName transaction
     * @throws AdempiereException
     * @see #saveEx(String)
     */
    public void saveEx(String trxName) throws AdempiereException
    {
        set_TrxName(trxName);
        saveEx();
    }

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
     * 	Set UpdatedBy
     * 	@param AD_User_ID user
     */
    protected void setUpdatedBy (int AD_User_ID)
    {
        set_ValueNoCheck ("UpdatedBy", new Integer(AD_User_ID));
    }	//	setAD_User_ID

    /**
     * Set value of Column
     * @param columnName
     * @param value
     */
    public final void set_ValueOfColumn(String columnName, Object value)
    {
        set_ValueOfColumnReturningBoolean(columnName, value);
    }

    /**
     * Set value of Column returning boolean
     * @param columnName
     * @param value
     *  @returns boolean indicating success or failure
     */
    public final boolean set_ValueOfColumnReturningBoolean(String columnName, Object value)
    {
        int AD_Column_ID = p_info.getAD_Column_ID(columnName);
        if (AD_Column_ID > 0)
            return set_ValueOfColumnReturningBoolean(AD_Column_ID, value);
        else
            return false;
    }

    /**
     *  Set Value of Column
     *  @param AD_Column_ID column
     *  @param value value
     */
    public final void set_ValueOfColumn (int AD_Column_ID, Object value)
    {
        set_ValueOfColumnReturningBoolean (AD_Column_ID, value);
    }   //  setValueOfColumn


    /**
     *  Set Value of Column
     *  @param AD_Column_ID column
     *  @param value value
     *  @returns boolean indicating success or failure
     */
    public final boolean set_ValueOfColumnReturningBoolean (int AD_Column_ID, Object value)
    {
        int index = p_info.getColumnIndex(AD_Column_ID);
        if (index < 0)
            throw new AdempiereUserError("Not found - AD_Column_ID=" + AD_Column_ID);
        String ColumnName = p_info.getColumnName(index);
        if (ColumnName.equals("IsApproved"))
            return set_ValueNoCheck(ColumnName, value);
        else
            return set_Value (index, value);
    }   //  setValueOfColumn

    /**
     *  Set Value if updateable and correct class.
     *  (and to NULL if not mandatory)
     *  @param index index
     *  @param value value
     *  @return true if value set
     */
    protected boolean set_Value (int index, Object value)
    {
        return set_Value(index, value, true);
    }

    @Override
    protected boolean set_Value (String ColumnName, Object value) {
        return super.set_Value(ColumnName, value);
    }


    protected void updateUUID(MColumn column, String trxName) {
        MTable table = (MTable) column.getAD_Table();
        if (table.getTableName().startsWith("T_")) {
            // don't update UUID for temporary tables
            return;
        }
        int AD_Column_ID = 0;
        StringBuilder sql = new StringBuilder("SELECT ");
        String keyColumn = null;
        String[] compositeKeys = table.getKeyColumns();
        if (compositeKeys == null || compositeKeys.length == 1) {
            keyColumn = compositeKeys[0];
            AD_Column_ID = table.getColumn(keyColumn).getAD_Column_ID();
            compositeKeys = null;
        }
        if ((compositeKeys == null || compositeKeys.length == 0) && keyColumn == null) {
            // TODO: Update using rowid for oracle or ctid for postgresql
            log.warning("Cannot update orphan table " + table.getTableName() + " (not ID neither parents)");
            return;
        }
        if (compositeKeys == null) {
            sql.append(keyColumn);
        } else {
            for(String s : compositeKeys) {
                sql.append(s).append(",");
            }
            sql.deleteCharAt(sql.length()-1);
        }
        sql.append(" FROM ").append(table.getTableName());
        sql.append(" WHERE ").append(column.getColumnName()).append(" IS NULL ");
        StringBuilder updateSQL = new StringBuilder("UPDATE ");
        updateSQL.append(table.getTableName());
        updateSQL.append(" SET ");
        updateSQL.append(column.getColumnName());
        updateSQL.append("=? WHERE ");
        if (AD_Column_ID > 0) {
            updateSQL.append(keyColumn).append("=?");
        } else {
            for(String s : compositeKeys) {
                updateSQL.append(s).append("=? AND ");
            }
            int length = updateSQL.length();
            updateSQL.delete(length-5, length); // delete last AND
        }

        boolean localTrx = false;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Trx trx = trxName != null ? Trx.get(trxName, false) : null;
        if (trx == null) {
            trx = Trx.get(Trx.createTrxName(), true);
            trx.setDisplayName(PO.class.getName()+"_updateUUID");
            localTrx = true;
        }
        try {
            if (localTrx)
                trx.start();
            stmt = DB.prepareStatement(sql.toString(), trx.getTrxName());
            stmt.setFetchSize(100);
            rs = stmt.executeQuery();
            while (rs.next()) {
                if (AD_Column_ID > 0) {
                    int recordId = rs.getInt(1);
                    // this line is to avoid users generating official UUIDs - comment it to do official migration script work
                    if (recordId > MTable.MAX_OFFICIAL_ID) {
                        UUID uuid = UUID.randomUUID();
                        DB.executeUpdateEx(updateSQL.toString(),new Object[]{uuid.toString(), recordId}, trx.getTrxName());
                    }
                } else {
                    UUID uuid = UUID.randomUUID();
                    List<Object> params = new ArrayList<Object>();
                    params.add(uuid.toString());
                    for (String s : compositeKeys) {
                        params.add(rs.getObject(s));
                    }
                    DB.executeUpdateEx(updateSQL.toString(),params.toArray(),trx.getTrxName());
                }
            }
            if (localTrx) {
                trx.commit(true);
            }
        } catch (SQLException e) {
            if (localTrx) {
                trx.rollback();
            }
            throw new DBException(e);
        } finally {
            DB.close(rs, stmt);
            if (localTrx) {
                trx.close();
            }
        }
    }

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
            updateUUID(column, get_TrxName());
        }
        return no > 0;
    }	//	insert_Accounting

    /** Model Info              */
    protected org.idempiere.orm.POInfo getP_info() {
        return super.p_info;
    }

    /**
     * Delete Current Record
     * @param force delete also processed records
     * @throws AdempiereException
     * @see #delete(boolean)
     */
    public void deleteEx(boolean force) throws AdempiereException
    {
        if (!delete(force)) {
            String msg = null;
            ValueNamePair err = CLogger.retrieveError();
            if (err != null)
                msg = err.getName();
            if (msg == null || msg.length() == 0)
                msg = "DeleteError";
            throw new AdempiereException(msg);
        }
    }

    /**
     * Delete Current Record
     * @param force delete also processed records
     * @param trxName transaction
     * @throws AdempiereException
     * @see {@link #deleteEx(boolean)}
     */
    public void deleteEx(boolean force, String trxName) throws AdempiereException
    {
        set_TrxName(trxName);
        deleteEx(force);
    }

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

    public static <T> T as(Class<T> clazz, Object o){
        if(clazz.isInstance(o)){
            return clazz.cast(o);
        }
        return null;
    }


    /**************************************************************************
     * 	Get Attachments.
     * 	An attachment may have multiple entries
     *	@return Attachment or null
     */
    @JsonIgnore
    public MAttachment getAttachment ()
    {
        return getAttachment(false);
    }	//	getAttachment

    /**
     * 	Get Attachments
     * 	@param requery requery
     *	@return Attachment or null
     */
    public MAttachment getAttachment (boolean requery)
    {
        if (m_attachment == null || requery)
            m_attachment = MAttachment.get (getCtx(), p_info.getAD_Table_ID(), get_ID());
        return m_attachment;
    }	//	getAttachment

    /* FR 2962094 - Finish implementation of weighted average costing
       Fill the column ProcessedOn (if it exists) with a bigdecimal representation of current timestamp (with nanoseconds)
    */
    public void setProcessedOn(String ColumnName, Object value, Object oldValue) {
        if ("Processed".equals(ColumnName)
            && value instanceof Boolean
            && ((Boolean)value).booleanValue() == true
            && (oldValue == null
            || (oldValue instanceof Boolean
            && ((Boolean)oldValue).booleanValue() == false))) {
            if (get_ColumnIndex("ProcessedOn") > 0) {
                // fill processed on column
                //get current time from db
                Timestamp ts = DB.getSQLValueTS(null, "SELECT CURRENT_TIMESTAMP FROM DUAL");
                long mili = ts.getTime();
                int nano = ts.getNanos();
                double doublets = Double.parseDouble(Long.toString(mili) + "." + Integer.toString(nano));
                BigDecimal bdtimestamp = BigDecimal.valueOf(doublets);
                set_Value("ProcessedOn", bdtimestamp);
            }
        }
    }

}
