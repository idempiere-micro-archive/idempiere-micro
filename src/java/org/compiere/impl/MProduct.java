package org.compiere.impl;

import org.compiere.model.I_M_CostDetail;
import org.compiere.model.I_M_Product;
import org.compiere.orm.MTree_Base;
import org.compiere.orm.Query;
import org.compiere.orm.X_AD_Tree;
import org.compiere.product.MAttributeSet;
import org.compiere.product.MAttributeSetInstance;
import org.compiere.product.X_I_Product;
import org.compiere.product.X_M_Product;
import org.compiere.util.Msg;
import org.idempiere.common.exceptions.AdempiereException;
import org.idempiere.common.util.CCache;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;
import org.idempiere.common.util.Util;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

public class MProduct extends org.compiere.product.MProduct {

    /**
     * 	Import Constructor
     *	@param impP import
     */
    public MProduct (X_I_Product impP) {
        super(impP);
    }



        /**************************************************************************
         * 	Standard Constructor
         *	@param ctx context
         *	@param M_Product_ID id
         *	@param trxName transaction
         */
    public MProduct (Properties ctx, int M_Product_ID, String trxName) {
        super(ctx, M_Product_ID, trxName);
    }

    /**
     * 	Load constructor
     *	@param ctx context
     *	@param rs result set
     *	@param trxName transaction
     */
    public MProduct (Properties ctx, ResultSet rs, String trxName)
    {
        super(ctx, rs, trxName);
    }	//	MProduct

    private String verifyStorage() {
        BigDecimal qtyOnHand = Env.ZERO;
        BigDecimal qtyOrdered = Env.ZERO;
        BigDecimal qtyReserved = Env.ZERO;
        for (MStorageOnHand ohs: MStorageOnHand.getOfProduct(getCtx(), get_ID(), get_TrxName()))
        {
            qtyOnHand = qtyOnHand.add(ohs.getQtyOnHand());
        }
        for (MStorageReservation rs : MStorageReservation.getOfProduct(getCtx(), get_ID(), get_TrxName()))
        {
            if (rs.isSOTrx())
                qtyReserved = qtyReserved.add(rs.getQty());
            else
                qtyOrdered = qtyOrdered.add(rs.getQty());
        }

        StringBuilder errMsg = new StringBuilder();
        if (qtyOnHand.signum() != 0)
            errMsg.append("@QtyOnHand@ = ").append(qtyOnHand);
        if (qtyOrdered.signum() != 0)
            errMsg.append(" - @QtyOrdered@ = ").append(qtyOrdered);
        if (qtyReserved.signum() != 0)
            errMsg.append(" - @QtyReserved@").append(qtyReserved);
        return errMsg.toString();
    }

    /**
     * 	HasInventoryOrCost
     *	@return true if it has Inventory or Cost
     */
    protected boolean hasInventoryOrCost ()
    {
        //check if it has transactions
        boolean hasTrx = new Query(getCtx(), MTransaction.Table_Name,
            MTransaction.COLUMNNAME_M_Product_ID+"=?", get_TrxName())
            .setOnlyActiveRecords(true)
            .setParameters(new Object[]{get_ID()})
            .match();
        if (hasTrx)
        {
            return true;
        }

        //check if it has cost
        boolean hasCosts = new Query(getCtx(), I_M_CostDetail.Table_Name,
            I_M_CostDetail.COLUMNNAME_M_Product_ID+"=?", get_TrxName())
            .setOnlyActiveRecords(true)
            .setParameters(get_ID())
            .match();
        if (hasCosts)
        {
            return true;
        }

        return false;
    }

    @Override
    protected boolean afterSave (boolean newRecord, boolean success)
    {
        if (!success)
            return success;

        //	Value/Name change in Account
        if (!newRecord && (is_ValueChanged("Value") || is_ValueChanged("Name")))
            MAccount.updateValueDescription(getCtx(), "M_Product_ID=" + getM_Product_ID(), get_TrxName());

        //	Name/Description Change in Asset	MAsset.setValueNameDescription
        if (!newRecord && (is_ValueChanged("Name") || is_ValueChanged("Description")))
        {
            String sql = "UPDATE A_Asset a "
                + "SET (Name, Description)="
                + "(SELECT SUBSTR((SELECT bp.Name FROM C_BPartner bp WHERE bp.C_BPartner_ID=a.C_BPartner_ID) || ' - ' || p.Name,1,60), p.Description "
                + "FROM M_Product p "
                + "WHERE p.M_Product_ID=a.M_Product_ID) "
                + "WHERE IsActive='Y'"
                //	+ " AND GuaranteeDate > SysDate"
                + "  AND M_Product_ID=" + getM_Product_ID();
            int no = DB.executeUpdate(sql, get_TrxName());
            if (log.isLoggable(Level.FINE)) log.fine("Asset Description updated #" + no);
        }

        //	New - Acct, Tree, Old Costing
        if (newRecord)
        {
            insert_Accounting("M_Product_Acct", "M_Product_Category_Acct",
                "p.M_Product_Category_ID=" + getM_Product_Category_ID());
            insert_Tree(X_AD_Tree.TREETYPE_Product);
        }
        if (newRecord || is_ValueChanged(I_M_Product.COLUMNNAME_Value))
            update_Tree(MTree_Base.TREETYPE_Product);

        //	New Costing
        if (newRecord || is_ValueChanged("M_Product_Category_ID"))
            MCost.create(this);

        return success;
    }	//	afterSave

    @Override
    protected boolean beforeDelete ()
    {
        if (X_M_Product.PRODUCTTYPE_Resource.equals(getProductType()) && getS_Resource_ID() > 0)
        {
            throw new AdempiereException("@S_Resource_ID@<>0");
        }
        //	Check Storage
        if (isStocked() || X_M_Product.PRODUCTTYPE_Item.equals(getProductType()))
        {
            String errMsg = verifyStorage();
            if (! Util.isEmpty(errMsg))
            {
                log.saveError("Error", Msg.parseTranslation(getCtx(), errMsg));
                return false;
            }

        }
        //	delete costing
        MCost.delete(this);

        // [ 1674225 ] Delete Product: Costing deletion error
		/*MAcctSchema[] mass = MAcctSchema.getClientAcctSchema(getCtx(),getAD_Client_ID(), get_TrxName());
		for(int i=0; i<mass.length; i++)
		{
			// Get Cost Elements
			MCostElement[] ces = MCostElement.getMaterialWithCostingMethods(this);
			MCostElement ce = null;
			for(int j=0; j<ces.length; j++)
			{
				if(MCostElement.COSTINGMETHOD_StandardCosting.equals(ces[i].getCostingMethod()))
				{
					ce = ces[i];
					break;
				}
			}

			if(ce == null)
				continue;

			MCost mcost = MCost.get(this, 0, mass[i], 0, ce.getM_CostElement_ID());
			mcost.delete(true, get_TrxName());
		}*/

        //
        return true;
    }	//	beforeDelete

    /**
     * Check if ASI is mandatory
     * @param isSOTrx is outgoing trx?
     * @return true if ASI is mandatory, false otherwise
     */
    public boolean isASIMandatory(boolean isSOTrx) {
        //
        //	If CostingLevel is BatchLot ASI is always mandatory - check all client acct schemas
        MAcctSchema[] mass = MAcctSchema.getClientAcctSchema(getCtx(), getAD_Client_ID(), get_TrxName());
        for (MAcctSchema as : mass)
        {
            String cl = getCostingLevel(as);
            if (MAcctSchema.COSTINGLEVEL_BatchLot.equals(cl)) {
                return true;
            }
        }
        //
        // Check Attribute Set settings
        int M_AttributeSet_ID = getM_AttributeSet_ID();
        if (M_AttributeSet_ID != 0)
        {
            MAttributeSet mas = MAttributeSet.get(getCtx(), M_AttributeSet_ID);
            if (mas == null || !mas.isInstanceAttribute())
                return false;
                // Outgoing transaction
            else if (isSOTrx)
                return mas.isMandatory();
                // Incoming transaction
            else // isSOTrx == false
                return mas.isMandatoryAlways();
        }
        //
        // Default not mandatory
        return false;
    }

    /**
     * Get Product Costing Level
     * @param as accounting schema
     * @return product costing level
     */
    public String getCostingLevel(MAcctSchema as)
    {
        MProductCategoryAcct pca = MProductCategoryAcct.get(getCtx(), getM_Product_Category_ID(), as.get_ID(), get_TrxName());
        String costingLevel = pca.getCostingLevel();
        if (costingLevel == null)
        {
            costingLevel = as.getCostingLevel();
        }
        return costingLevel;
    }

    /**
     * Get Product Costing Method
     * @param C_AcctSchema_ID accounting schema ID
     * @return product costing method
     */
    public String getCostingMethod(MAcctSchema as)
    {
        MProductCategoryAcct pca = MProductCategoryAcct.get(getCtx(), getM_Product_Category_ID(), as.get_ID(), get_TrxName());
        String costingMethod = pca.getCostingMethod();
        if (costingMethod == null)
        {
            costingMethod = as.getCostingMethod();
        }
        return costingMethod;
    }



    public MCost getCostingRecord(MAcctSchema as, int AD_Org_ID, int M_ASI_ID)
    {
        return getCostingRecord(as, AD_Org_ID, M_ASI_ID, getCostingMethod(as));
    }

    public MCost getCostingRecord(MAcctSchema as, int AD_Org_ID, int M_ASI_ID, String costingMethod)
    {

        String costingLevel = getCostingLevel(as);
        if (MAcctSchema.COSTINGLEVEL_Client.equals(costingLevel))
        {
            AD_Org_ID = 0;
            M_ASI_ID = 0;
        }
        else if (MAcctSchema.COSTINGLEVEL_Organization.equals(costingLevel))
            M_ASI_ID = 0;
        else if (MAcctSchema.COSTINGLEVEL_BatchLot.equals(costingLevel))
        {
            AD_Org_ID = 0;
            if (M_ASI_ID == 0)
                return null;
        }
        MCostElement ce = MCostElement.getMaterialCostElement(getCtx(), costingMethod, AD_Org_ID);
        if (ce == null) {
            return null;
        }
        MCost cost = MCost.get(this, M_ASI_ID, as, AD_Org_ID, ce.getM_CostElement_ID(), get_TrxName());
        return cost.is_new() ? null : cost;
    }

    @Override
    protected boolean beforeSave (boolean newRecord)
    {
        //	Check Storage
        if (!newRecord && 	//
            ((is_ValueChanged("IsActive") && !isActive())		//	now not active
                || (is_ValueChanged("IsStocked") && !isStocked())	//	now not stocked
                || (is_ValueChanged("ProductType") 					//	from Item
                && X_M_Product.PRODUCTTYPE_Item.equals(get_ValueOld("ProductType")))))
        {
            String errMsg = verifyStorage();
            if (! Util.isEmpty(errMsg))
            {
                log.saveError("Error", Msg.parseTranslation(getCtx(), errMsg));
                return false;
            }
        }	//	storage

        // it checks if UOM has been changed , if so disallow the change if the condition is true.
        if ((!newRecord) && is_ValueChanged("C_UOM_ID") && hasInventoryOrCost ()) {
            log.saveError("Error", Msg.getMsg(getCtx(), "SaveUomError"));
            return false;
        }

        //	Reset Stocked if not Item
        //AZ Goodwill: Bug Fix isStocked always return false
        //if (isStocked() && !PRODUCTTYPE_Item.equals(getProductType()))
        if (!X_M_Product.PRODUCTTYPE_Item.equals(getProductType()))
            setIsStocked(false);

        //	UOM reset
        if (m_precision != null && is_ValueChanged("C_UOM_ID"))
            m_precision = null;

        // AttributeSetInstance reset
        if (getM_AttributeSetInstance_ID() > 0 && is_ValueChanged(I_M_Product.COLUMNNAME_M_AttributeSet_ID))
        {
            MAttributeSetInstance asi = new MAttributeSetInstance(getCtx(), getM_AttributeSetInstance_ID(), get_TrxName());
            if (asi.getM_AttributeSet_ID() != getM_AttributeSet_ID())
                setM_AttributeSetInstance_ID(0);
        }
        if (!newRecord && is_ValueChanged(I_M_Product.COLUMNNAME_M_AttributeSetInstance_ID))
        {
            // IDEMPIERE-2752 check if the ASI is referenced in other products before trying to delete it
            int oldasiid = get_ValueOldAsInt(I_M_Product.COLUMNNAME_M_AttributeSetInstance_ID);
            if (oldasiid > 0) {
                MAttributeSetInstance oldasi = new MAttributeSetInstance(getCtx(), get_ValueOldAsInt(I_M_Product.COLUMNNAME_M_AttributeSetInstance_ID), get_TrxName());
                int cnt = DB.getSQLValueEx(get_TrxName(), "SELECT COUNT(*) FROM M_Product WHERE M_AttributeSetInstance_ID=?", oldasi.getM_AttributeSetInstance_ID());
                if (cnt == 1) {
                    // Delete the old m_attributesetinstance
                    try {
                        oldasi.deleteEx(true, get_TrxName());
                    } catch (AdempiereException ex)
                    {
                        log.saveError("Error", "Error deleting the AttributeSetInstance");
                        return false;
                    }
                }
            }
        }

        return true;
    }	//	beforeSave

    /**	Cache						*/
    private static CCache<Integer,org.compiere.product.MProduct> s_cache	= new CCache<Integer, org.compiere.product.MProduct>(I_M_Product.Table_Name, 40, 5);	//	5 minutes

    /**
     * 	Get MProduct from Cache
     *	@param ctx context
     *	@param M_Product_ID id
     *	@return MProduct or null
     */
    public static MProduct get (Properties ctx, int M_Product_ID)
    {
        if (M_Product_ID <= 0)
        {
            return null;
        }
        Integer key = new Integer (M_Product_ID);
        MProduct retValue = (MProduct) s_cache.get (key);
        if (retValue != null)
        {
            return retValue;
        }
        retValue = new MProduct(ctx, M_Product_ID, null);
        if (retValue.get_ID () != 0)
        {
            s_cache.put (key, retValue);
        }
        return retValue;
    }	//	get

}
