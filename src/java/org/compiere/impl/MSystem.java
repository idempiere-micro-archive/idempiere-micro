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
 *****************************************************************************/
package org.compiere.impl;

import org.compiere.db.LDAP;
import org.compiere.orm.MSysConfig;
import org.compiere.orm.Query;
import org.compiere.util.DisplayType;
import org.compiere.webutil.TimeUtil;
import org.idempiere.common.db.CConnection;
import org.idempiere.common.db.Database;
import org.idempiere.common.exceptions.AdempiereException;
import org.idempiere.common.exceptions.DBException;
import org.idempiere.common.util.CLogMgt;
import org.idempiere.common.util.DB;
import org.idempiere.common.util.Env;
import org.idempiere.common.util.Ini;
import org.idempiere.common.util.CCache;

import java.lang.management.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;


/**
 * 	System Record (just one)
 *
 *  @author Jorg Janke
 *  @version $Id: MSystem.java,v 1.3 2006/10/09 00:22:28 jjanke Exp $
 *
 *  @author Teo Sarca, www.arhipac.ro
 *  		<li>FR [ 2214883 ] Remove SQL code and Replace for Query
 */
public class MSystem extends org.compiere.orm.MSystem
{
	/**
	 *
	 */
	private static final long serialVersionUID = 8639311032004561198L;

	/**
	 * 	Is LDAP Authentification defined
	 *	@return true if ldap defined
	 */
	public boolean isLDAP()
	{
		String host = getLDAPHost();
		if (host == null || host.length() == 0)
			return false;
		String domain = getLDAPDomain();
		return domain != null
			&& domain.length() > 0;
	}	//	isLDAP

	/**
	 * 	LDAP Authentification. Assumes that LDAP is defined.
	 *	@param userName user name
	 *	@param password password
	 *	@return true if ldap authenticated
	 */
	public boolean isLDAP (String userName, String password)
	{
		return LDAP.validate(getLDAPHost(), getLDAPDomain(), userName, password);
	}	//	isLDAP

	/**
	 * 	Get DB Address
	 *	@return address
	 */
	public String getDBAddress ()
	{
		String s = super.getDBAddress ();
		if (s == null || s.length() == 0)
			s = CConnection.get().getConnectionURL();
		return s;
	}	//	getDBAddress

	/**
	 * 	Before Save
	 *	@param newRecord new
	 *	@return true/false
	 */
	protected boolean beforeSave (boolean newRecord)
	{
		//	Mandatory Values
		if (get_Value(COLUMNNAME_IsAutoErrorReport) == null)
			setIsAutoErrorReport (true);
		//
		boolean userChange = Ini.getIni().isClient() &&
			(is_ValueChanged(COLUMNNAME_Name)
			|| is_ValueChanged(COLUMNNAME_UserName)
			|| is_ValueChanged(COLUMNNAME_Password)
			|| is_ValueChanged(COLUMNNAME_LDAPHost)
			|| is_ValueChanged(COLUMNNAME_LDAPDomain)
			|| is_ValueChanged(COLUMNNAME_CustomPrefix)
			);
		if (userChange)
		{
			String name = getName();
			if (name.equals("?") || name.length() < 2)
			{
				throw new AdempiereException("Define a unique System name (e.g. Company name) not " + name);
			}
			if (getUserName().equals("?") || getUserName().length() < 2)
			{
				throw new AdempiereException("Use the same EMail address as in the Adempiere Web Store");
			}
			if (getPassword().equals("?") || getPassword().length() < 2)
			{
				throw new AdempiereException("Use the same Password as in the Adempiere Web Store");
			}
		}
		//
		setInfo();
		return true;
	}	//	beforeSave

	/**
	 * 	Save Record (ID=0)
	 * 	@return true if saved
	 */
	public boolean save()
	{
		if (!beforeSave(false))
			return false;
		return saveUpdate();
	}	//	save

	/**
	 * 	String Representation
	 *	@return info
	 */
	public String toString()
	{
		return "MSystem[" + getName()
			+ ",User=" + getUserName()
			+ ",ReleaseNo=" + getReleaseNo()
			+ "]";
	}	//	toString


	/**************************************************************************
	 * 	Check validity
	 *	@return true if valid
	 */
	public boolean isValid()
	{
		if (getName() == null || getName().length() < 2)
		{
			log.log(Level.WARNING, "Name not valid: " + getName());
			return false;
		}
		if (getPassword() == null || getPassword().length() < 2)
		{
			log.log(Level.WARNING, "Password not valid: " + getPassword());
			return false;
		}
		if (getInfo() == null || getInfo().length() < 2)
		{
			log.log(Level.WARNING, "Need to run Migration once");
			return false;
		}
		return true;
	}	//	isValid

	/**
	 * 	Is there a PDF License
	 *	@return true if there is a PDF License
	 */
	public boolean isPDFLicense()
	{
		String key = getSummary();
		return key != null && key.length() > 25;
	}	//	isPDFLicense


	/**
	 * 	Print info
	 */
	public void info()
	{
		if (!CLogMgt.isLevelFine())
			return;
		//	OS
	//	OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
	//	log.fine(os.getName() + " " + os.getVersion() + " " + os.getArch()
	//		+ " Processors=" + os.getAvailableProcessors());
		//	Runtime
		@SuppressWarnings("unused")
		RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
		// log.fine(rt.getName() + " (" + rt.getVmVersion() + ") Up=" + TimeUtil.formatElapsed(rt.getUptime()));
		//	Memory
		if (CLogMgt.isLevelFiner())
		{
			List<MemoryPoolMXBean> list = ManagementFactory.getMemoryPoolMXBeans();
			Iterator<MemoryPoolMXBean> it = list.iterator();
			while (it.hasNext())
			{
				@SuppressWarnings("unused")
				MemoryPoolMXBean pool = (MemoryPoolMXBean)it.next();
				/*
				log.finer(pool.getName() + " " + pool.getType()
					+ ": " + new CMemoryUsage(pool.getUsage()));
				*/
			}
		}
		else
		{
			@SuppressWarnings("unused")
			MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
			// log.fine("VM: " + new CMemoryUsage(memory.getNonHeapMemoryUsage()));
			// log.fine("Heap: " + new CMemoryUsage(memory.getHeapMemoryUsage()));
		}
		//	Thread
		@SuppressWarnings("unused")
		ThreadMXBean th = ManagementFactory.getThreadMXBean();
		/*
		log.fine("Threads=" + th.getThreadCount()
			+ ", Peak=" + th.getPeakThreadCount()
			+ ", Demons=" + th.getDaemonThreadCount()
			+ ", Total=" + th.getTotalStartedThreadCount()
		);
		*/
	}	//	info

	/*
	 * Allow remember me feature
	 * ZK_LOGIN_ALLOW_REMEMBER_ME and SWING_ALLOW_REMEMBER_ME parameter allow the next values
	 *   U - Allow remember the username (default for zk)
	 *   P - Allow remember the username and password (default for swing)
	 *   N - None
	 *
	 *	@return boolean representing if remember me feature is allowed
	 */
	private static final String SYSTEM_ALLOW_REMEMBER_USER = "U";
	private static final String SYSTEM_ALLOW_REMEMBER_PASSWORD = "P";

	public static boolean isZKRememberUserAllowed() {
		String ca = MSysConfig.getValue(MSysConfig.ZK_LOGIN_ALLOW_REMEMBER_ME, SYSTEM_ALLOW_REMEMBER_USER);
		return (ca.equalsIgnoreCase(SYSTEM_ALLOW_REMEMBER_USER) || ca.equalsIgnoreCase(SYSTEM_ALLOW_REMEMBER_PASSWORD));
	}

	public static boolean isZKRememberPasswordAllowed() {
		String ca = MSysConfig.getValue(MSysConfig.ZK_LOGIN_ALLOW_REMEMBER_ME, SYSTEM_ALLOW_REMEMBER_USER);
		return (ca.equalsIgnoreCase(SYSTEM_ALLOW_REMEMBER_PASSWORD) && !MSysConfig.getBooleanValue(MSysConfig.USER_PASSWORD_HASH, false));
	}

	public static boolean isSwingRememberUserAllowed() {
		String ca = MSysConfig.getValue(MSysConfig.SWING_LOGIN_ALLOW_REMEMBER_ME, SYSTEM_ALLOW_REMEMBER_PASSWORD);
		return (ca.equalsIgnoreCase(SYSTEM_ALLOW_REMEMBER_USER) || ca.equalsIgnoreCase(SYSTEM_ALLOW_REMEMBER_PASSWORD));
	}

	public static boolean isSwingRememberPasswordAllowed() {
		String ca = MSysConfig.getValue(MSysConfig.SWING_LOGIN_ALLOW_REMEMBER_ME, SYSTEM_ALLOW_REMEMBER_PASSWORD);
		return (ca.equalsIgnoreCase(SYSTEM_ALLOW_REMEMBER_PASSWORD) && !MSysConfig.getBooleanValue(MSysConfig.USER_PASSWORD_HASH, false));
	}

	/** System - cached					*/
	private static CCache<Integer, MSystem> s_system = new CCache<Integer, MSystem>(Table_Name, 1, -1, true);

	public synchronized static MSystem get (Properties ctx)
	{
		if (s_system.get(0) != null)
			return s_system.get(0);
		//
		MSystem system = new Query(ctx, Table_Name, null, null)
				.setOrderBy(COLUMNNAME_AD_System_ID)
				.firstOnly();
		if (system == null)
			return null;
		//
		if (!Ini.getIni().isClient() && system.setInfo())
		{
			system.saveEx();
		}
		s_system.put(0, system);
		return system;
	}	//	get

	public MSystem (Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

}	//	MSystem
