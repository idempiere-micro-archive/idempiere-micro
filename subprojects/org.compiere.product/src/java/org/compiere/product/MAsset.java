package org.compiere.product;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.I_A_Asset;
import org.compiere.model.I_C_BPartner_Location;
import org.compiere.orm.MTable;
import org.compiere.orm.PO;
import org.compiere.orm.Query;
import org.compiere.orm.SetGetUtil;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;
import org.compiere.util.Msg;
 

/**
 * Asset Model
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 */
@SuppressWarnings("serial")
public class MAsset extends X_A_Asset
	//implements MAssetType.Model //commented by @win
{
	/** ChangeType - Asset Group changed */
	public static final int CHANGETYPE_setAssetGroup = I_A_Asset.Table_ID * 100 + 1;
	
	/**
	 * Get Asset
	 * @param ctx context
	 * @param A_Asset_ID asset
	 * @param trxName
	 */
	public static MAsset get (Properties ctx, int A_Asset_ID, String trxName)
	{
		return (MAsset)MTable.get(ctx, MAsset.Table_Name).getPO(A_Asset_ID, trxName);
	}	//	get
	
	/**
	 * Get Assets from given M_Product_ID and M_ASI_ID.
	 * <p>Note: The A_Asset_Product table is not checked !!!
	 * @param ctx
	 * @param M_Product_ID (optional)
	 * @param M_ASI_ID
	 * @return array of MAsset
	 */
	public static Collection<MAsset> forASI(Properties ctx, int M_Product_ID, int M_ASI_ID)
	{
		ArrayList<Object> params = new ArrayList<Object>();
		String whereClause = I_A_Asset.COLUMNNAME_M_AttributeSetInstance_ID + "=?";
		params.add(M_ASI_ID);
		if (M_Product_ID > 0) {
			whereClause += " AND " + I_A_Asset.COLUMNNAME_M_Product_ID + "=?";
			params.add(M_Product_ID);
		}
		//
		return new Query(ctx, MAsset.Table_Name, whereClause, null)
					.setParameters(params)
					.list();
	}
	
	/** Create constructor */
	public MAsset (Properties ctx, int A_Asset_ID, String trxName)
	{
		super (ctx, A_Asset_ID,trxName);
		if (A_Asset_ID == 0)
		{
			setA_Asset_Status(X_A_Asset.A_ASSET_STATUS_New);
			//commented out by @win
			/*
			setA_Asset_Type("MFX");
			setA_Asset_Type_ID(1); // MFX
			*/
			//end comment by @win
		}
	}	//	MAsset

	/**
	 * Load Constructor
	 * @param ctx context
	 * @param rs result set record
	 */
	public MAsset (Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}	//	MAsset
	

	
	/**
	 * Set Asset Group; also it sets other default fields
	 * @param assetGroup
	 */
	public void setAssetGroup(MAssetGroup assetGroup) {
		setA_Asset_Group_ID(assetGroup.getA_Asset_Group_ID());
		
		/* commented out by @win
		setA_Asset_Type_ID(assetGroup.getA_Asset_Type_ID());
		setGZ_TipComponenta(assetGroup.getGZ_TipComponenta()); // TODO: move to GZ
		MAssetType assetType = MAssetType.get(getCtx(), assetGroup.getA_Asset_Type_ID());
		assetType.update(SetGetUtil.wrap(this), true);
		*/ //end commet by @win
	}
	
	public MAssetGroup getAssetGroup() {
		return MAssetGroup.get(getCtx(), getA_Asset_Group_ID());
	}
	
	/**
	 * Set ASI Info (M_AttributeSetInstance_ID, Lot, SerNo)
	 * @param asi
	 */
	public void setASI(MAttributeSetInstance asi) {
		setM_AttributeSetInstance_ID(asi.getM_AttributeSetInstance_ID());
		setLot(asi.getLot());
		setSerNo(asi.getSerNo());
	}

	/**
	 * Before Save
	 * @param newRecord new
	 * @return true
	 */
	
	protected boolean beforeSave (boolean newRecord)
	{
		// Set parent asset:
		if (getA_Parent_Asset_ID() <= 0)
		{
			setA_Parent_Asset_ID(getA_Asset_ID());
		}	
		// Fix inventory number:
		String invNo = getInventoryNo();
		if(invNo != null)
		{
			setInventoryNo(invNo.trim());
		}
		// If no asset group, than set the default one:
		if(getA_Asset_Group_ID() <= 0)
		{
			setA_Asset_Group_ID(MAssetGroup.getDefault_ID(SetGetUtil.wrap(this)));
		}
		/* @win temporary commented out
		
		if (getA_Asset_Class_ID() <= 0 && getA_Asset_Group_ID() > 0)
		{
			MAssetGroup.updateAsset(SetGetUtil.wrap(this), getA_Asset_Group_ID());
		}
		*/
		//end @win comment
		
		// Copy fields from C_BPartner_Location
		if (is_ValueChanged(I_A_Asset.COLUMNNAME_C_BPartner_Location_ID) && getC_BPartner_Location_ID() > 0)
		{
			// Goodwill BF: Error: MAsset cannot be cast to SetGetModel
			SetGetUtil.copyValues(SetGetUtil.wrap(this), I_C_BPartner_Location.Table_Name, getC_BPartner_Location_ID(),
					new String[]{I_C_BPartner_Location.COLUMNNAME_C_Location_ID}
			);
		}
		//
		// Create ASI if not exist:
		if (getM_Product_ID() > 0 && getM_AttributeSetInstance_ID() <= 0)
		{
			MProduct product = MProduct.get(getCtx(), getM_Product_ID());
			MAttributeSetInstance asi = new MAttributeSetInstance(getCtx(), 0, product.getM_AttributeSet_ID(), get_TrxName());
			asi.setSerNo(getSerNo());
			asi.setDescription();
			asi.saveEx();
			setM_AttributeSetInstance_ID(asi.getM_AttributeSetInstance_ID());
		}
		// TODO: With the lines below, after creating the asset, the whole system goes much slower ??? 
//		else if (is_ValueChanged(COLUMNNAME_SerNo) && getM_AttributeSetInstance_ID() > 0) {
//			asi = new MAttributeSetInstance(getCtx(), getM_AttributeSetInstance_ID(), get_TrxName());
//			asi.setSerNo(getSerNo());
//			asi.setDescription();
//			asi.saveEx();
//		}
//		else if ((newRecord || is_ValueChanged(COLUMNNAME_M_AttributeSetInstance_ID)) && getM_AttributeSetInstance_ID() > 0) {
//			asi = new MAttributeSetInstance(getCtx(), getM_AttributeSetInstance_ID(), get_TrxName());
//			setASI(asi);
//		}
		//
		
		// Update status
		updateStatus();
		
		// Validate AssetType
		//@win commented out
		//MAssetType.validate(this);
		//@win end
		//
		
		return true;
	}	//	beforeSave
	
	
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if(!success)
		{
			return success;
		}
		
		//
		// Set parent
		if(getA_Parent_Asset_ID() <= 0)
		{
			int A_Asset_ID = getA_Asset_ID();
			setA_Parent_Asset_ID(A_Asset_ID);
			DB.executeUpdateEx("UPDATE A_Asset SET A_Parent_Asset_ID=A_Asset_ID WHERE A_Asset_ID=" + A_Asset_ID, get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("A_Parent_Asset_ID=" + getA_Parent_Asset_ID());
		}
		
		//
		// Set inventory number:
		String invNo = getInventoryNo();
		if(invNo == null || invNo.trim().length() == 0)
		{
			invNo = "" + get_ID();
			setInventoryNo(invNo);
			DB.executeUpdateEx("UPDATE A_Asset SET InventoryNo=" + DB.TO_STRING(invNo) + " WHERE A_Asset_ID=" + getA_Asset_ID(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("InventoryNo=" + getInventoryNo());
		}
		
		
		// If new record, create accounting and workfile
		if (newRecord)
		{
			//@win: set value at asset group as default value for asset
			MAssetGroup assetgroup = new MAssetGroup(getCtx(), getA_Asset_Group_ID(), get_TrxName());
			String isDepreciated = (assetgroup.isDepreciated()) ? "Y" : "N";
			String isOwned = (assetgroup.isOwned()) ? "Y" : "N";
			setIsDepreciated(assetgroup.isDepreciated());
			setIsOwned(assetgroup.isOwned());
			DB.executeUpdateEx("UPDATE A_Asset SET IsDepreciated='" + isDepreciated + "', isOwned ='" + isOwned + "' WHERE A_Asset_ID=" + getA_Asset_ID(), get_TrxName());
			//end @win

			
		}
		else
		{
			MAssetChange.createAndSave(getCtx(), "UPD", new PO[]{this}, null);
		}

		return true;
	}	//	afterSave
	
	
	protected boolean beforeDelete()
	{
		return true;
	}       //      beforeDelete
	
	/**
	 * 
	 * @see #beforeSave(boolean)
	 */
	public void updateStatus()
	{
		String status = getA_Asset_Status();
		setProcessed(!status.equals(X_A_Asset.A_ASSET_STATUS_New));
//		setIsDisposed(!status.equals(A_ASSET_STATUS_New) && !status.equals(A_ASSET_STATUS_Activated));
		setIsDisposed(status.equals(X_A_Asset.A_ASSET_STATUS_Disposed));
		setIsFullyDepreciated(status.equals(X_A_Asset.A_ASSET_STATUS_Depreciated));
		if(isFullyDepreciated() || status.equals(X_A_Asset.A_ASSET_STATUS_Disposed))
		{
			setIsDepreciated(false);
		}
		/* commented by @win 
		MAssetClass assetClass = MAssetClass.get(getCtx(), getA_Asset_Class_ID());
		if (assetClass != null && assetClass.isDepreciated())
		{
			setIsDepreciated(true);
		}
		*/ //end comment by @win
		if (status.equals(X_A_Asset.A_ASSET_STATUS_Activated) || getAssetActivationDate() == null)
		{
			setAssetActivationDate(getAssetServiceDate());
		}
	}
	
	/**
	 * Change asset status to newStatus
	 * @param newStatus see A_ASSET_STATUS_
	 * @param date state change date; if null context "#Date" will be used
	 */
	public void changeStatus(String newStatus, Timestamp date)
	{
		String status = getA_Asset_Status();
		if (log.isLoggable(Level.FINEST)) log.finest("Entering: " + status + "->" + newStatus + ", date=" + date);
		
		//
		// If date is null, use context #Date
		if(date == null) {
			date = Env.getContextAsDate(getCtx(), "#Date");
		}
		
		//
		//	Activation/Addition
		if(newStatus.equals(X_A_Asset.A_ASSET_STATUS_Activated))
		{
			setAssetActivationDate(date);
		}
		//

		// Disposal
		if(newStatus.equals(X_A_Asset.A_ASSET_STATUS_Disposed))
		{ // casat, vandut
			setAssetDisposalDate(date);
		}
		
		// Set new status
		setA_Asset_Status(newStatus);
	}	//	changeStatus
	
	// Temporary used variables:
	/**			*/
	private int m_UseLifeMonths_F = 0;
	public int getUseLifeMonths_F()											{	return m_UseLifeMonths_F;	}
	public void setUseLifeMonths_F(int UseLifeMonths_F)						{	m_UseLifeMonths_F = UseLifeMonths_F; }
	/**			*/
	private int m_A_Current_Period = 0;
	public int getA_Current_Period()										{	return m_A_Current_Period;	}
	public void setA_Current_Period(int A_Current_Period)					{	m_A_Current_Period = A_Current_Period; }
	/**			*/
	private Timestamp m_DateAcct = null;
	public Timestamp getDateAcct()											{	return m_DateAcct;	}
	public void setDateAcct(Timestamp DateAcct)								{	m_DateAcct = DateAcct; }
	/**			*/
	private int m_A_Depreciation_ID = 0;
	public int getA_Depreciation_ID()										{	return m_A_Depreciation_ID;	}
	public void setA_Depreciation_ID(int A_Depreciation_ID)					{	m_A_Depreciation_ID = A_Depreciation_ID; }
	/**			*/
	private int m_A_Depreciation_F_ID = 0;
	public int getA_Depreciation_F_ID()										{	return m_A_Depreciation_F_ID;	}
	public void setA_Depreciation_F_ID(int A_Depreciation_F_ID)				{	m_A_Depreciation_F_ID = A_Depreciation_F_ID; }
	/**			*/
	private BigDecimal m_A_Asset_Cost = Env.ZERO;
	private BigDecimal m_A_Accumulated_Depr = Env.ZERO;
	private BigDecimal m_A_Accumulated_Depr_F = Env.ZERO;
	public BigDecimal getA_Asset_Cost()										{	return m_A_Asset_Cost;	}
	public void setA_Asset_Cost(BigDecimal A_Asset_Cost)					{	m_A_Asset_Cost = A_Asset_Cost; }
	public BigDecimal getA_Accumulated_Depr()								{	return m_A_Accumulated_Depr;	}
	public void setA_Accumulated_Depr(BigDecimal A_Accumulated_Depr)		{	m_A_Accumulated_Depr = A_Accumulated_Depr; }
	public BigDecimal getA_Accumulated_Depr_F()								{	return m_A_Accumulated_Depr_F;	}
	public void setA_Accumulated_Depr_F(BigDecimal A_Accumulated_Depr_F)	{	m_A_Accumulated_Depr_F = A_Accumulated_Depr_F; }

	public MProductDownload[] getProductDownloads() {
		// TODO Auto-generated method stub
		return null;
	}

	public static MAsset getFromShipment(Properties ctx, int i, String trxName) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getProductR_MailText_ID() {
		// IDEMPIERE-197 Stabilize Fixed Assets
		// AssetDelivery is requiring this missing column
		// TODO: Adding this method to compile correctly and future research,
		// but the process AssetDelivery will fail with error "** Product Mail Text"
		return 0;
	}

	public boolean isDownloadable() {
		// IDEMPIERE-197 Stabilize Fixed Assets
		// AssetServlet is requiring this missing column
		// TODO: Adding this method to compile correctly and future research
		return false;
	}
}
