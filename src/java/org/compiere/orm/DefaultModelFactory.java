/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.idempiere.org/license.html           *
 * Contributor(s): Carlos Ruiz - globalqss                                    *
 *****************************************************************************/
package org.compiere.orm;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

import org.idempiere.orm.PO;
import org.idempiere.common.util.CCache;
import org.idempiere.common.util.CLogger;
import org.idempiere.common.util.Env;
import org.idempiere.common.util.Util;

/**
 * Default model factory implementation base on legacy code in MTable.
 * @author Jorg Janke
 * @author hengsin
 */
public class DefaultModelFactory implements IModelFactory {

	private static CCache<String,Class<?>> s_classCache = new CCache<String,Class<?>>(null, "PO_Class", 20, false);
	private final static CLogger s_log = CLogger.getCLogger(DefaultModelFactory.class);

	/**	Packages for Model Classes	*/
	private static final String[]	s_packages = new String[] {

		"org.compiere.model",
		"org.compiere.impexp",
		"compiere.model",			//	globalqss allow compatibility with other plugins
		"adempiere.model",			//	Extensions
		"org.adempiere.model",

			"org.compiere.impl",	// order is important this must be BEFORE the bundles

			"org.compiere.bo",
			"org.compiere.conversionrate",
			"org.compiere.crm",
			"org.compiere.order",
			"org.compiere.orm",
			"org.compiere.process",
			"org.compiere.product",
			"org.compiere.tax",
			"org.compiere.wf",
			"org.compiere.validation"

	};

	/**	Special Classes				*/
	private static final String[]	s_special = new String[] {
		"AD_Element", "M_Element",
		"AD_Registration", "M_Registration",
		"AD_Tree", "MTree_Base",
		"R_Category", "MRequestCategory",
		"GL_Category", "MGLCategory",
		"K_Category", "MKCategory",
		"C_ValidCombination", "MAccount",
		"C_Phase", "MProjectTypePhase",
		"C_Task", "MProjectTypeTask"
	//	AD_Attribute_Value, AD_TreeNode
	};


    @Override
    public Class<?> getClass(String tableName) {
        return getClass(tableName, true);
    }

	/* (non-Javadoc)
	 * @see org.compiere.orm.IModelFactory#getClass(java.lang.String)
	 */
	public Class<?> getClass(String tableName, Boolean useCache) {
//		Not supported
		if (tableName == null || tableName.endsWith("_Trl"))
			return null;

		//check cache
        if (useCache) {
            Class<?> cache = s_classCache.get(tableName);
            if (cache != null) {
                //Object.class indicate no generated PO class for tableName
                if (cache.equals(Object.class))
                    return null;
                else
                    return cache;
            }
        }

		MTable table = MTable.get(Env.getCtx(), tableName);
		String entityType = table.getEntityType();

		//	Import Tables (Name conflict)
		//  Import Tables doesn't manage model M classes, just X_
		if (tableName.startsWith("I_"))
		{
			MEntityType et = MEntityType.get(Env.getCtx(), entityType);
			String etmodelpackage = et.getModelPackage();
			if (etmodelpackage == null || MEntityType.ENTITYTYPE_Dictionary.equals(entityType))
				etmodelpackage = "org.compiere.impl"; // fallback for dictionary or empty model package on entity type
			Class<?> clazz = getPOclass(etmodelpackage + ".X_" + tableName, tableName);
			if (clazz != null)
			{
				s_classCache.put(tableName, clazz);
				return clazz;
			}
			s_log.warning("No class for table: " + tableName);
			return null;
		}

		//	Special Naming
		for (int i = 0; i < s_special.length; i++)
		{
			if (s_special[i++].equals(tableName))
			{
				Class<?> clazz = getPOclass(s_special[i], tableName);
				if (clazz != null)
				{
					s_classCache.put(tableName, clazz);
					return clazz;
				}
				break;
			}
		}

		//begin [ 1784588 ] Use ModelPackage of EntityType to Find Model Class - vpj-cd
		if (!MEntityType.ENTITYTYPE_Dictionary.equals(entityType))
		{
			MEntityType et = MEntityType.get(Env.getCtx(), entityType);
			String etmodelpackage = et.getModelPackage();
			if (etmodelpackage != null)
			{
				Class<?> clazz = null;
				clazz = getPOclass(etmodelpackage + ".M" + Util.replace(tableName, "_", ""), tableName);
				if (clazz != null) {
					s_classCache.put(tableName, clazz);
					return clazz;
				}
				clazz = getPOclass(etmodelpackage + ".X_" + tableName, tableName);
				if (clazz != null) {
					s_classCache.put(tableName, clazz);
					return clazz;
				}
				s_log.warning("No class for table with it entity: " + tableName);
			}
		}
		//end [ 1784588 ]

		//	Strip table name prefix (e.g. AD_) Customizations are 3/4
		String className = tableName;
		int index = className.indexOf('_');
		if (index > 0)
		{
			if (index < 3)		//	AD_, A_
				 className = className.substring(index+1);
			/* DELETEME: this part is useless - teo_sarca, [ 1648850 ]
			else
			{
				String prefix = className.substring(0,index);
				if (prefix.equals("Fact"))		//	keep custom prefix
					className = className.substring(index+1);
			}
			*/
		}
		//	Remove underlines
		String classNameWOU = Util.replace(className, "_", "");

		//	Search packages
		for (int i = 0; i < s_packages.length; i++)
		{
			StringBuffer name = new StringBuffer(s_packages[i]).append(".M").append(classNameWOU);
			Class<?> clazz = getPOclass(name.toString(), tableName);
			if (clazz != null)
			{
				s_classCache.put(tableName, clazz);
				return clazz;
			}
			name = new StringBuffer(s_packages[i]).append(".X_").append(tableName); //X_C_ContactActivity
			clazz = getPOclass(name.toString(), tableName);
			if (clazz != null)
			{
				s_classCache.put(tableName, clazz);
				return clazz;
			}
		}

		//Object.class to indicate no PO class for tableName
		s_classCache.put(tableName, Object.class);
		return null;
	}

	/**
	 * Get PO class
	 * @param className fully qualified class name
	 * @param tableName Optional. If specified, the loaded class will be validated for that table name
	 * @return class or null
	 */
	private Class<?> getPOclass (String className, String tableName)
	{
		try
		{
			Class<?> clazz = Class.forName(className);
			// Validate if the class is for specified tableName
			if (tableName != null)
			{
				String classTableName = clazz.getField("Table_Name").get(null).toString();
				if (!tableName.equals(classTableName))
				{
					if (s_log.isLoggable(Level.FINEST)) s_log.finest("Invalid class for table: " + className+" (tableName="+tableName+", classTableName="+classTableName+")");
					return null;
				}
			}
			//	Make sure that it is a PO class
			if (org.idempiere.icommon.model.IPO.class.isAssignableFrom(clazz)){
				if (s_log.isLoggable(Level.FINE)) {
					s_log.fine("Use: " + className);
				}
				return clazz;
			} else {
			}
		}
		catch (Exception e)
		{
		}
		if (s_log.isLoggable(Level.FINEST)) {
			s_log.finest("Not found: " + className);
		}
		return null;
	}	//	getPOclass

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName) {
		Class<?> clazz = getClass(tableName);
		if (clazz == null)
		{
			s_log.warning("No class for table: " + tableName + " called with Record_ID");
			return null;
		}

		boolean errorLogged = false;
		try
		{
			Constructor<?> constructor = null;
			try
			{
				constructor = clazz.getDeclaredConstructor(new Class[]{Properties.class, int.class, String.class});
			}
			catch (Exception e)
			{
				String msg = e.getMessage();
				if (msg == null)
					msg = e.toString();
				s_log.warning("No transaction Constructor for " + clazz + " (" + msg + ")");
			}

			try {
				PO po = constructor != null ? (PO) constructor.newInstance(new Object[]{Env.getCtx(), new Integer(Record_ID), trxName}) : null;
				return po;
			} catch ( Exception ex ) {
				s_log.warning( "PO FAILED for table '" + tableName + "', Record_ID:" + Record_ID + " and clazz '" + clazz.getCanonicalName() + "'"  );
				throw ex;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();

			if (e.getCause() != null)
			{
				Throwable t = e.getCause();
				s_log.log(Level.SEVERE, "(id) - Table=" + tableName + ",Class=" + clazz, t);
				errorLogged = true;
				if (t instanceof Exception)
					s_log.saveError("Error", (Exception)e.getCause());
				else
					s_log.saveError("Error", "Table=" + tableName + ",Class=" + clazz);
			}
			else
			{
				s_log.log(Level.SEVERE, "(id) - Table=" + tableName + ",Class=" + clazz, e);
				errorLogged = true;
				s_log.saveError("Error", "Table=" + tableName + ",Class=" + clazz);
			}
		}
		if (!errorLogged)
			s_log.log(Level.SEVERE, "(id) - Not found - Table=" + tableName
				+ ", Record_ID=" + Record_ID);
		return null;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName) {
		return getPO(tableName, rs, trxName, null);
	}


	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName, String columnNamePrefix) {
		Class<?> clazz = getClass(tableName);
		if (clazz == null)
		{
			s_log.warning( "PO NO CLAZZ FOR TABLE '" + tableName + "' with ResultSet" );
            getClass(tableName, false);
			return null;
		}

		boolean errorLogged = false;
		try
		{
			if ( columnNamePrefix==null ) {
				Constructor<?> constructor = clazz.getDeclaredConstructor(new Class[]{Properties.class, ResultSet.class, String.class});
				PO po = (PO) constructor.newInstance(new Object[]{Env.getCtx(), rs, trxName});
				return po;
			} else {
				Constructor<?> constructor = clazz.getDeclaredConstructor(new Class[]{Properties.class, ResultSet.class, String.class, String.class});
				PO po = (PO) constructor.newInstance(new Object[]{Env.getCtx(), rs, trxName, columnNamePrefix});
				return po;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();

			s_log.log(Level.SEVERE, "(rs) - Table=" + tableName + ",Class=" + clazz, e);
			errorLogged = true;
			s_log.saveError("Error", "Table=" + tableName + ",Class=" + clazz);
		}
		if (!errorLogged)
			s_log.log(Level.SEVERE, "(rs) - Not found - Table=" + tableName);
		return null;
	}
}
