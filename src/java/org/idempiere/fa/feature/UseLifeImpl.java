/**
 *
 */
package org.idempiere.fa.feature;

import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

//import MAssetClass; //commented by @win
import org.compiere.impl.PO;
import org.compiere.impl.SetGetModel;
import org.compiere.orm.SetGetUtil;
import org.idempiere.common.util.CLogger;
import org.compiere.webutil.TimeUtil;

 
 
 /**
  * Asset properties - classification of assets, service period, life use.
  *	@author Teo Sarca, SC ARHIPAC SERVICE SRL
  *	@version $Id$
  */
public class UseLifeImpl
	implements UseLife
{
	private final static String FIELD_UseLifeYears = "UseLifeYears";
	private final static String FIELD_UseLifeMonths = "UseLifeMonths";
	private final static String FIELD_FiscalPostfix = "_F";
	
	private SetGetModel m_obj = null;
	private CLogger log = CLogger.getCLogger(getClass());
	private boolean fiscal = false;
	
	/**
	 */
	public static UseLifeImpl get(SetGetModel obj) {
		return new UseLifeImpl(obj, false);
	}
	
	/**
	 */
	public static UseLifeImpl get(SetGetModel obj, boolean fiscal) {
		return new UseLifeImpl(obj, fiscal);
	}
	
	/**
	 */
	public UseLifeImpl(SetGetModel obj, boolean fiscal) {
		m_obj = obj;
		this.fiscal = fiscal;
	}
	
	/**
	 */
	public Properties getCtx() {
		return m_obj.getCtx();
	}
	
	public int get_Table_ID() {
		return m_obj.get_Table_ID();
	}

	public String get_TableName() {
		return m_obj.get_TableName();
	}

	/**
	 */
	private final static String getFieldName(String fieldName, boolean fiscal) {
		String field = fieldName;
		if (fiscal) {
			field += FIELD_FiscalPostfix;
		}
		return field;
	}
	
	/**
	 */
	public boolean isFiscal() {
		return fiscal;
	}
	
	/**
	 */
	public boolean set_AttrValue(String name, Object value) {
		return m_obj.set_AttrValue(name, value);
	}
	
	/**
	 */
	public Object get_AttrValue(String name) {
		return m_obj.get_AttrValue(name);
	}
	
	/**
	 */
	public boolean is_AttrValueChanged(String name) {
		return m_obj.is_AttrValueChanged(name);
	}
	
	/**
	 *	@return transaction name for decorated object
	 */
	public String get_TrxName() {
		return m_obj.get_TrxName();
	}

	/**	Set UseLifeMonths and UseLifeYears
	 *	@param	value	use life months
	 */
	public void setUseLifeMonths(int value) {
		if (log.isLoggable(Level.FINE)) log.fine("Entering: value=" + value + ", " + this);
		m_obj.set_AttrValue(getFieldName(FIELD_UseLifeMonths, fiscal), Integer.valueOf(value));
		m_obj.set_AttrValue(getFieldName(FIELD_UseLifeYears, fiscal), Integer.valueOf(value/12));
		if (log.isLoggable(Level.FINE)) log.fine("Leaving: value=" + value + ", " + this);
	}
	
	/**
	 *	@return use life months
	 */
	public int getUseLifeMonths() {
		Object obj = m_obj.get_AttrValue(getFieldName(FIELD_UseLifeMonths, fiscal));
		if (obj != null && obj instanceof Number) {
			return ((Number)obj).intValue();
		}
		return 0;
	}
	
	/**	Set UseLifeYears and UseLifeMonths
	 *	@param value		use life years
	 */
	public void setUseLifeYears(int value) {
		if (log.isLoggable(Level.FINE)) log.fine("Entering: value=" + value + ", " + this);
		m_obj.set_AttrValue(getFieldName(FIELD_UseLifeYears, fiscal), Integer.valueOf(value));
		m_obj.set_AttrValue(getFieldName(FIELD_UseLifeMonths, fiscal), Integer.valueOf(value*12));
		if (log.isLoggable(Level.FINE)) log.fine("Leaving: value=" + value + ", " + this);
	}
	
	/**
	 *	@return use life years
	 */
	public int getUseLifeYears() {
		Object obj = m_obj.get_AttrValue(getFieldName(FIELD_UseLifeYears, fiscal));
		if (obj != null && obj instanceof Number) {
			return ((Number)obj).intValue();
		}
		return 0;
	}
	
	/**
	 * Adjust use life years
	 * @param deltaUseLifeYears
	 * @param reset
	 */
	public void adjustUseLifeYears(int deltaUseLifeYears, boolean reset)
	{
		int uselife = (reset ? 0 : getUseLifeYears());
		int new_uselife = uselife + deltaUseLifeYears;
		setUseLifeYears(new_uselife);
		if (log.isLoggable(Level.FINE)) log.fine("UseLifeYears=" + uselife + ", delta=" + deltaUseLifeYears + " => new UseLifeYears=" + new_uselife + " (isFiscal=" + isFiscal() + ")");
	}
	
	/**
	 *	@return Asset Service Date (PIF)
	 */
	public Timestamp getAssetServiceDate() {
		if (m_obj instanceof UseLife) {
			return ((UseLife)m_obj).getAssetServiceDate();
		} else {
			Object obj = m_obj.get_AttrValue("AssetServiceDate");
			if (obj != null && obj instanceof Timestamp) {
				return (Timestamp)obj;
			}
		}
		return null;
	}
	
	/**
	 *	@return asset class ID
	 */
	/* commented out by @win
	public int getA_Asset_Class_ID()
	{
		if (m_obj instanceof UseLife)
		{
			return ((UseLife)m_obj).getA_Asset_Class_ID();
		}
		else
		{
			Object obj = m_obj.get_AttrValue("A_Asset_Class_ID");
			if (obj != null && obj instanceof Number)
			{
				return ((Number)obj).intValue();
			}
		}
		return 0;
	}
	*/ // end comment by @win
	
	/**
	 * Copy UseLifeMonths, UseLifeMonths_F, UseLifeYears, UseLifeYears_F fields from "from" to "to"
	 * @param	to	destination model
	 * @param	from source model
	 */
	public static void copyValues(PO to, PO from) {
		SetGetUtil.copyValues(to, from, new String[]{"UseLifeMonths", "UseLifeYears", "UseLifeMonths_F", "UseLifeYears_F"}, null);
	}
	
	/**	Validates and corrects errors in model  */
	public boolean validate() {
		return validate(true);
	}
	
	/**	Validates and corrects errors in model */
	public boolean validate(boolean saveError) {
		if (log.isLoggable(Level.FINE)) log.fine("Entering: " + this);
		
		int useLifeYears = 0;
		int useLifeMonths = 0;
		useLifeYears = getUseLifeYears();
		useLifeMonths = getUseLifeMonths();
		
		if (useLifeMonths == 0) {
			useLifeMonths = useLifeYears * 12;
		}
		if (useLifeMonths % 12 != 0) {
			if(saveError) log.saveError("Error", "@Invalid@ @UseLifeMonths@=" + useLifeMonths + "(@Diff@=" + (useLifeMonths % 12) + ")" );
			return false;
		}
		if (useLifeYears == 0) {
			useLifeYears = (int)(useLifeMonths / 12);
		}
		/* commented out by @win
		int A_Asset_Class_ID = getA_Asset_Class_ID();
		if (A_Asset_Class_ID > 0 && (useLifeMonths == 0 || useLifeYears == 0)) {
			if(saveError) log.saveError("Error", "@Invalid@ @UseLifeMonths@=" + useLifeMonths + ", @UseLifeYears@=" + useLifeYears);
			return false;
		}
		*/ //commented out by @win
		
		setUseLifeMonths(useLifeMonths);
		setUseLifeYears(useLifeYears);
		
		/* commented by @win
		MAssetClass assetClass = MAssetClass.get(getCtx(), A_Asset_Class_ID);
		if (assetClass != null && !assetClass.validate(this)) {
			if (log.isLoggable(Level.FINE)) log.fine("Leaving [RETURN FALSE]");
			return false;
		}
		*/ //end comment by @win
		
		if (log.isLoggable(Level.FINE)) log.fine("Leaving [RETURN TRUE]");
		return true;
	}
	
	/**	String representation (intern)
	 */
	public String toString()
	{
		return 
			"UseLifeImpl[UseLife=" + getUseLifeYears() + "|" + getUseLifeMonths()
				+ ", isFiscal=" + isFiscal()
				+ ", AssetServiceDate=" + getAssetServiceDate()
				//+ ", A_Asset_Class=" + getA_Asset_Class_ID() //commented by @win
				+ ", m_obj=" + m_obj
				+ "]"
		;
	}
	
	/**	Calculate date accounting for = assetServiceDate + A_Current_Period
	 *	@param assetServiceDate	data PIF
	 *	@param A_Current_Period	 (displacement)
	 *	@return assetServiceDate + A_Current_Period
	 */
	public static Timestamp getDateAcct(Timestamp assetServiceDate, int A_Current_Period) {
		if (assetServiceDate == null)
			return null;
		return TimeUtil.addMonths(assetServiceDate, A_Current_Period);
	}
 }