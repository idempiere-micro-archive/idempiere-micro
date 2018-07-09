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
package org.compiere;

import org.adempiere.base.Core;
import org.compiere.impl.MClient;
import org.compiere.lookups.MLookup;
import org.compiere.orm.MSysConfig;
import org.compiere.impl.MSystem;
import org.compiere.validation.ModelValidationEngine;
import org.compiere.webutil.Login;
import org.idempiere.common.db.CConnection;
import org.idempiere.common.util.*;
import org.osgi.service.component.annotations.Component;
import software.hsharp.core.services.ISystemImpl;

import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;

/**
 *  Adempiere Control Class
 *
 *  @author Jorg Janke
 *  @version $Id: Adempiere.java,v 1.8 2006/08/11 02:58:14 jjanke Exp $
 *
 */
@Component
public final class Adempiere implements ISystemImpl
{
	/** Timestamp                   */
	 public final String	ID				= "$Id: Adempiere.java,v 1.8 2006/08/11 02:58:14 jjanke Exp $";
	/** Main Version String         */
	 public String	MAIN_VERSION	= "Release 5.1";
	/** Detail Version as date      Used for Client/Server		*/
	 public String	DATE_VERSION	= "2017-10-31";
	/** Database Version as date    Compared with AD_System		*/
	 public String	DB_VERSION		= "2017-10-31"; // TODO DAP take from DB

	/** Product Name            */
	 public final String	NAME 			= "iDempiere\u00AE"; // TODO DAP take from DB ?
	/** URL of Product          */
	 public final String	URL				= "www.idempiere.org";
	/** 16*16 Product Image. **/
	 private final String	s_File16x16		= "images/iD16.gif";
	/** 32*32 Product Image.   	*/
	 private final String	s_file32x32		= "images/iD32.gif";
	/** 100*30 Product Image.  	*/
	 private final String	s_file100x30	= "images/iD10030.png";
	/** 48*15 Product Image.   	*/
	 private final String	s_file48x15		= "images/iDempiere.png";
	 private final String	s_file48x15HR	= "images/iDempiereHR.png";
	/** Support Email           */
	 private String		s_supportEmail	= "";

	/** Subtitle                */
	 public final String	SUB_TITLE		= "Smart Suite ERP, CRM and SCM";
	 public final String	ADEMPIERE_R		= "iDempiere\u00AE";
	 public final String	COPYRIGHT		= "\u00A9 1999-2016 iDempiere\u00AE";

	 private String		s_ImplementationVersion = null;
	 private String		s_ImplementationVendor = null;

	 private final String ONLINE_HELP_URL = "http://wiki.idempiere.org";

	/**	Logging								*/
	private  CLogger		log = null;

	/** Thread pool **/
	private  ScheduledThreadPoolExecutor threadPoolExecutor = null;

/* DAP
	 {
		ClassLoader loader = Adempiere.class.getClassLoader();
		InputStream inputStream = loader.getResourceAsStream("org/adempiere/version.properties");
		if (inputStream != null)
		{
			Properties properties = new Properties();
			try {
				properties.load(inputStream);
				if (properties.containsKey("MAIN_VERSION"))
					MAIN_VERSION = properties.getProperty("MAIN_VERSION");
				if (properties.containsKey("DATE_VERSION"))
					DATE_VERSION = properties.getProperty("DATE_VERSION");
				if (properties.containsKey("DB_VERSION"))
					DB_VERSION = properties.getProperty("DB_VERSION");
				if (properties.containsKey("IMPLEMENTATION_VERSION"))
					s_ImplementationVersion = properties.getProperty("IMPLEMENTATION_VERSION");
				if (properties.containsKey("IMPLEMENTATION_VENDOR"))
					s_ImplementationVendor = properties.getProperty("IMPLEMENTATION_VENDOR");
			} catch (IOException e) {
			}
		}
	}*/

	/**
	 *  Get Product Name
	 *  @return Application Name
	 */
	public  String getName()
	{
		return NAME;
	}   //  getName

	/**
	 *  Get Product Version
	 *  @return Application Version
	 */
	public  String getVersion()
	{
		String version = MSysConfig.getValue(MSysConfig.APPLICATION_MAIN_VERSION, null);
		if(version != null)
			return version;

		return "Unknown";
	}   //  getVersion

	public  boolean isVersionShown(){
		return MSysConfig.getBooleanValue(MSysConfig.APPLICATION_MAIN_VERSION_SHOWN, true);
	}

	public  boolean isDBVersionShown(){
		boolean defaultVal = MSystem.get(Env.getCtx()).getSystemStatus().equalsIgnoreCase("P") ? false : true;
		return MSysConfig.getBooleanValue(MSysConfig.APPLICATION_DATABASE_VERSION_SHOWN, defaultVal);
	}

	public  boolean isVendorShown(){
		return MSysConfig.getBooleanValue(MSysConfig.APPLICATION_IMPLEMENTATION_VENDOR_SHOWN, true);
	}

	public  boolean isJVMShown(){
		boolean defaultVal = MSystem.get(Env.getCtx()).getSystemStatus().equalsIgnoreCase("P") ? false : true;
		return MSysConfig.getBooleanValue(MSysConfig.APPLICATION_JVM_VERSION_SHOWN, defaultVal);
	}

	public  boolean isOSShown(){
		boolean defaultVal = MSystem.get(Env.getCtx()).getSystemStatus().equalsIgnoreCase("P") ? false : true;
		return MSysConfig.getBooleanValue(MSysConfig.APPLICATION_OS_INFO_SHOWN, defaultVal);
	}

	public  boolean isHostShown()
	{
		boolean defaultVal = MSystem.get(Env.getCtx()).getSystemStatus().equalsIgnoreCase("P") ? false : true;
		return MSysConfig.getBooleanValue(MSysConfig.APPLICATION_HOST_SHOWN, defaultVal);
	}

	public  String getDatabaseVersion()
	{
//		return DB.getSQLValueString(null, "select lastmigrationscriptapplied from ad_system");
		return MSysConfig.getValue(MSysConfig.APPLICATION_DATABASE_VERSION,
				DB.getSQLValueString(null, "select lastmigrationscriptapplied from ad_system"));
	}

	/**
	 *	Short Summary (Windows)
	 *  @return summary
	 */
	public  String getSum()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(NAME).append(" ").append(MAIN_VERSION).append(SUB_TITLE);
		return sb.toString();
	}	//	getSum

	/**
	 *	Summary (Windows).
	 * 	iDempiere(tm) Release 1.0c_2013-06-27 -Smart Suite ERP, CRM and SCM- Copyright (c) 1999-2013 iDempiere; Implementation: 2.5.1a 20040417-0243 - (C) 1999-2005 Jorg Janke, iDempiere Inc. USA
	 *  @return Summary in Windows character set
	 */
	public  String getSummary()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(NAME).append(" ")
			.append(MAIN_VERSION).append("_").append(DATE_VERSION)
			.append(" -").append(SUB_TITLE)
			.append("- ").append(COPYRIGHT)
			.append("; Implementation: ").append(getImplementationVersion())
			.append(" - ").append(getImplementationVendor());
		return sb.toString();
	}	//	getSummary

	/**
	 * 	Set Package Info
	 */
	private  void setPackageInfo()
	{
		if (s_ImplementationVendor != null)
			return;

		Package adempierePackage = Package.getPackage("org.compiere");
		s_ImplementationVendor = adempierePackage.getImplementationVendor();
		s_ImplementationVersion = adempierePackage.getImplementationVersion();
		if (s_ImplementationVendor == null)
		{
			s_ImplementationVendor = "Supported by iDempiere community";
			s_ImplementationVersion = "iDempiere";
		}
	}	//	setPackageInfo

	/**
	 * 	Get Jar Implementation Version
	 * 	@return Implementation-Version
	 */
	public  String getImplementationVersion()
	{
		if (s_ImplementationVersion == null)
			setPackageInfo();
		return s_ImplementationVersion;
	}	//	getImplementationVersion

	/**
	 * 	Get Jar Implementation Vendor
	 * 	@return Implementation-Vendor
	 */
	public  String getImplementationVendor()
	{
		if(DB.isConnected()){
			String vendor = MSysConfig.getValue(MSysConfig.APPLICATION_IMPLEMENTATION_VENDOR, null);
			if(vendor != null)
				return vendor;
		}
		if (s_ImplementationVendor == null)
			setPackageInfo();
		return s_ImplementationVendor;
	}	//	getImplementationVendor

	/**
	 *  Get Checksum
	 *  @return checksum
	 */
	public  int getCheckSum()
	{
		return getSum().hashCode();
	}   //  getCheckSum

	/**
	 *	Summary in ASCII
	 *  @return Summary in ASCII
	 */
	public  String getSummaryAscii()
	{
		String retValue = getSummary();
		//  Registered Trademark
		retValue = Util.replace(retValue, "\u00AE", "(r)");
		//  Trademark
		retValue = Util.replace(retValue, "\u2122", "(tm)");
		//  Copyright
		retValue = Util.replace(retValue, "\u00A9", "(c)");
		//  Cr
		retValue = Util.replace(retValue, Env.NL, " ");
		retValue = Util.replace(retValue, "\n", " ");
		return retValue;
	}	//	getSummaryAscii

	/**
	 * 	Get Java VM Info
	 *	@return VM info
	 */
	public  String getJavaInfo()
	{
		return System.getProperty("java.vm.name")
			+ " " + System.getProperty("java.vm.version");
	}	//	getJavaInfo

	/**
	 * 	Get Operating System Info
	 *	@return OS info
	 */
	public  String getOSInfo()
	{
		return System.getProperty("os.name") + " "
			+ System.getProperty("os.version") + " "
			+ System.getProperty("sun.os.patch.level");
	}	//	getJavaInfo

	/**
	 *  Get full URL
	 *  @return URL
	 */
	public  String getURL()
	{
		return "http://" + URL;
	}   //  getURL

	/**
	 * @return URL
	 */
	public  String getOnlineHelpURL()
	{
		return ONLINE_HELP_URL;
	}

	/**
	 *  Get Sub Title
	 *  @return Subtitle
	 */
	public  String getSubtitle()
	{
		return SUB_TITLE;
	}   //  getSubitle


	/**
	 *  Get Support Email
	 *  @return Support mail address
	 */
	public  String getSupportEMail()
	{
		return s_supportEmail;
	}   //  getSupportEMail

	/**
	 *  Set Support Email
	 *  @param email Support mail address
	 */
	public  void setSupportEMail(String email)
	{
		s_supportEmail = email;
	}   //  setSupportEMail

	/**
	 * 	Get JNLP CodeBase
	 *	@return code base or null
	 */
	public  URL getCodeBase()
	{
		return null;
	}	//	getCodeBase

	/**
	 * @return True if client is started using web start
	 */
	public  boolean isWebStartClient()
	{
		return getCodeBase() != null;
	}

	/**
	 * 	Get JNLP CodeBase Host
	 *	@return code base or null
	 */
	public  String getCodeBaseHost()
	{
		URL url = getCodeBase();
		if (url == null)
			return null;
		return url.getHost();
	}	//	getCodeBase

	public  synchronized boolean isStarted()
	{
		return (log != null);
	}

    @Override
    public void startup() {
        startup(false);
    }


    /*************************************************************************
	 *  Startup Client/Server.
	 *  - Print greeting,
	 *  - Check Java version and
	 *  - load ini parameters
	 *  If it is a client, load/set PLAF and exit if error.
	 *  If Client, you need to call startupEnvironment explicitly!
	 * 	For testing call method startupEnvironment
	 *	@param isClient true for client
	 *  @return successful startup
	 */
	public  synchronized boolean startup (boolean isClient)
	{
		//	Already started
		if (log != null)
			return true;

		//	Check Version
		if (isClient && !Login.isJavaOK(isClient))
			System.exit(1);

		Ini.getIni().setClient(isClient);		//	init logging in Ini

		if (! isClient)  // Calling this on client is dropping the link with eclipse console
			CLogMgt.initialize(isClient);
		//	Init Log
		log = CLogger.getCLogger(Adempiere.class);
		//	Greeting
		if (log.isLoggable(Level.INFO)) log.info(getSummaryAscii());
	//	log.info(getAdempiereHome() + " - " + getJavaInfo() + " - " + getOSInfo());

		//  Load System environment
	//	EnvLoader.load(Ini.getIni().ENV_PREFIX);

		//  System properties
		Ini.getIni().getProperties();

		//	Set up Log
		/*
		CLogMgt.setLevel(Ini.getIni().getProperty(Ini.getIni().P_TRACELEVEL));
		if (isClient && Ini.getIni().isPropertyBool(Ini.getIni().P_TRACEFILE))
			CLogMgt.addHandler(new CLogFile(Ini.getIni().findAdempiereHome(), true, isClient));
		*/

		//setup specific log level
		Properties properties = Ini.getIni().getProperties();
		for(Object key : properties.keySet())
		{
			if (key instanceof String)
			{
				String s = (String)key;
				if (s.endsWith("."+Ini.getIni().P_TRACELEVEL))
				{
					String level = properties.getProperty(s);
					s = s.substring(0, s.length() - ("."+Ini.getIni().P_TRACELEVEL).length());
					CLogMgt.setLevel(s, level);
				}
			}
		}

		//	Set UI
		if (isClient)
		{
			if (CLogMgt.isLevelAll())
				log.log(Level.FINEST, System.getProperties().toString());
		}

		//  Set Default Database Connection from Ini
		DB.setDBTarget(CConnection.get(getCodeBaseHost()));
		createThreadPool();

		if (isClient)		//	don't test connection
			return false;	//	need to call

		return startupEnvironment(isClient);
	}   //  startup

	private  void createThreadPool() {
		int max = Runtime.getRuntime().availableProcessors() * 20;
		int defaultMax = max;
		Properties properties = Ini.getIni().getProperties();
		String maxSize = properties.getProperty("MaxThreadPoolSize");
		if (maxSize != null) {
			try {
				max = Integer.parseInt(maxSize);
			} catch (Exception e) {}
		}
		if (max <= 0) {
			max = defaultMax;
		}

		// start thread pool
		threadPoolExecutor = new ScheduledThreadPoolExecutor(max);

		//Trx.startTrxMonitor(); TODO DAP the other way around
	}

	/**
	 * 	Startup Adempiere Environment.
	 * 	Automatically called for Server connections
	 * 	For testing call this method.
	 *	@param isClient true if client connection
	 *  @return successful startup
	 */
	public  boolean startupEnvironment (boolean isClient)
	{
		startup(isClient);		//	returns if already initiated
		if (!DB.isConnected())
		{
			log.severe ("No Database");
			return false;
		}

		//	Check Build
		if (!DB.isBuildOK(Env.getCtx()))
		{
			if (isClient)
				System.exit(1);
			log = null;
			return false;
		}

		MSystem system = MSystem.get(Env.getCtx());	//	Initializes Base Context too
		if (system == null)
			return false;

		//	Initialize main cached Singletons
		ModelValidationEngine.get();
		try
		{
			String className = system.getEncryptionKey();
			if (className == null || className.length() == 0)
			{
				className = System.getProperty(SecureInterface.ADEMPIERE_SECURE);
				if (className != null && className.length() > 0
					&& !className.equals(SecureInterface.ADEMPIERE_SECURE_DEFAULT))
				{
					SecureEngine.init(className);	//	test it
					system.setEncryptionKey(className);
					system.saveEx();
				}
			}
			SecureEngine.init(className);

			//
			if (isClient)
				MClient.get(Env.getCtx(),0);			//	Login Client loaded later
			else
				MClient.getAll(Env.getCtx());
		}
		catch (Exception e)
		{
			log.warning("Environment problems: " + e.toString());
		}

		//	Start Workflow Document Manager (in other package) for PO
		String className = null;
		try
		{
			className = "org.compiere.wf.DocWorkflowManager";
			Class.forName(className);
		}
		catch (Exception e)
		{
			log.warning("Not started: " + className + " - " + e.getMessage());
		}

		if (!isClient)
			DB.updateMail();

		return true;
	}	//	startupEnvironment

	public  URL getResource(String name) {
		return Core.getResourceFinder().getResource(name);
	}

	public  synchronized void stop() {
		if (threadPoolExecutor != null) {
			threadPoolExecutor.shutdown();
		}
		log = null;
	}

	public  ScheduledThreadPoolExecutor getThreadPoolExecutor() {
		return threadPoolExecutor;
	}

    public static void main(final String[] args) throws Exception{
		Adempiere.getI().startup(false);
	}

    private static Adempiere instance;

    public Adempiere(){
		instance = this;

		try {
			MLookup.setThreadPoolExecutor(getThreadPoolExecutor());
		} catch ( AdempiereSystemError ex ) {
			ex.printStackTrace();
		}
	}

    public static Adempiere getI(){
        if(instance == null){
            new Adempiere();
        }
        return instance;
    }

}	//	Adempiere
