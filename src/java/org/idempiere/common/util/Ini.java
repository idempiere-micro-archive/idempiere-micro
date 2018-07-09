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
 * or via info@idempiere.org or http://www.idempiere.org/license.html           *
 *****************************************************************************/
package org.idempiere.common.util;

import kotlin.Unit;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;

//import org.idempiere.model.ModelValidationEngine; DAP TODO

/**
 *	Load & Save INI Settings from property file
 *	Initiated in Adempiere.startup
 *	Settings activated in ALogin.getIni
 *
 *  @author     Jorg Janke
 *  @version    $Id$
 *
 * @author Teo Sarca, www.arhipac.ro
 * 			<li>FR [ 1658127 ] Select charset encoding on import
 * 			<li>FR [ 2406123 ] Ini.getIni().saveProperties- fails if target directory does not exist
 */
public class Ini extends software.hsharp.core.util.Ini implements Serializable 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8936090051638559660L;

	/**	Logger			*/
	private  CLogger log = CLogger.getCLogger(Ini.class);

	public static String getBasePropertyFileName() {
		return "idempiere";
	}

	/** Apps User ID		*/
	public  final String	P_UID = 			"ApplicationUserID";
	private  final String	DEFAULT_UID = 		"GardenAdmin";
	/** Apps Password		*/
	public  final String	P_PWD = 			"ApplicationPassword";
	private  final String	DEFAULT_PWD = 		"GardenAdmin";
	/** Store Password		*/
	public  final String	P_STORE_PWD = 		"StorePassword";
	private  final boolean DEFAULT_STORE_PWD = true;
	/** Trace Level			*/
	public  final String	P_TRACELEVEL = 		"TraceLevel";
	private  final String DEFAULT_TRACELEVEL = "WARNING";
	/** Trace to File		*/
	public  final String	P_TRACEFILE = 		"TraceFile";
	private  final boolean DEFAULT_TRACEFILE = false;
	/** Language			*/
	public  final String 	P_LANGUAGE = 		"Language";
	private  final String DEFAULT_LANGUAGE = Language.getName(System.getProperty("user.language") + "_" + System.getProperty("user.country"));
	/** Ini File Name		*/
	public  final String 	P_INI = 			"FileNameINI";
	private  final String DEFAULT_INI = 		"";
	/** Connection Details	*/
	public  final String	P_CONNECTION =		"Connection";
	private  final String	DEFAULT_CONNECTION = "";
	/** Data Source			*/
	public  final String  P_CONTEXT = 		"DataSource";
	private  final String	DEFAULT_CONTEXT	= 	"java:idempiereDB";
	/** Look & Feel			*/
	public  final String	P_UI_LOOK =			"UILookFeel";

    private  final String	DEFAULT_UI_LOOK =	"Adempiere";
	/** UI Theme			*/

	private  final String	DEFAULT_UI_THEME =	"Adempiere Theme";
	/** UI Theme			*/
	public  final String	P_UI_THEME =		"UITheme";

	/** Flat Color UI
	public  final String	P_UI_FLAT =			"UIFlat";
	private  final boolean DEFAULT_UI_FLAT =	false;
	*/

	/** Auto Save			*/
	public  final String  P_A_COMMIT =		"AutoCommit";
	private  final boolean DEFAULT_A_COMMIT =	true;
	/** Auto Login			*/
	public  final String	P_A_LOGIN =			"AutoLogin";
	private  final boolean DEFAULT_A_LOGIN =	false;
	/** Auto New Record		*/
	public  final String	P_A_NEW =			"AutoNew";
	private  final boolean DEFAULT_A_NEW =	false;
	/** Dictionary Maintenance	*/
	public  final String  P_ADEMPIERESYS =		"AdempiereSys";	//	Save system records
	private  final boolean DEFAULT_ADEMPIERESYS = false;
	/** Log Migration Script	*/
	public  final String  P_LOGMIGRATIONSCRIPT =		"LogMigrationScript";	//	Log migration script
	private  final boolean DEFAULT_LOGMIGRATIONSCRIPT = false;
	/** Show Acct Tabs			*/
	public  final String  P_SHOW_ACCT =		"ShowAcct";
	private  final boolean DEFAULT_SHOW_ACCT = true;
	/** Show Translation Tabs	*/
	public  final String  P_SHOW_TRL =		"ShowTrl";
	private  final boolean DEFAULT_SHOW_TRL =	false;
	/** Cache Windows			*/
	public  final String  P_CACHE_WINDOW =	"CacheWindow";
	private  final boolean DEFAULT_CACHE_WINDOW = true;
	/** Temp Directory			*/
	public  final String  P_TEMP_DIR =    	"TempDir";
	private  final String  DEFAULT_TEMP_DIR =	"";
	/** Role					*/
	public  final String  P_ROLE =			"Role";
	private  final String  DEFAULT_ROLE =		"";
	/** Client Name				*/
	public  final String	P_CLIENT =			"Client";
	private  final String	DEFAULT_CLIENT =	"";
	/** Org Name				*/
	public  final String	P_ORG =				"Organization";
	private  final String	DEFAULT_ORG =		"";
	/** Printer Name			*/
	public  final String  P_PRINTER =			"Printer";
	private  final String  DEFAULT_PRINTER =	"";
	/** Warehouse Name			*/
	public  final String  P_WAREHOUSE =		"Warehouse";
	private  final String  DEFAULT_WAREHOUSE = "";
	/** Current Date			*/
	public  final String  P_TODAY =       	"CDate";
	private  final Timestamp DEFAULT_TODAY =	new Timestamp(System.currentTimeMillis());
	/** Print Preview			*/
	public  final String  P_PRINTPREVIEW = 	"PrintPreview";
	private  final boolean DEFAULT_PRINTPREVIEW =	false;
	/** Validate connection on startup */
	public  final String P_VALIDATE_CONNECTION_ON_STARTUP = "ValidateConnectionOnStartup";
	private  final boolean DEFAULT_VALIDATE_CONNECTION_ON_STARTUP = false;

	/** Single instance per window id **/
	public  final String P_SINGLE_INSTANCE_PER_WINDOW = "SingleInstancePerWindow";
	public  final boolean DEFAULT_SINGLE_INSTANCE_PER_WINDOW = false;

	/** Open new windows as maximized **/
	public  final String P_OPEN_WINDOW_MAXIMIZED = "OpenWindowMaximized";
	public  final boolean DEFAULT_OPEN_WINDOW_MAXIMIZED = false;
	//
	private  final String P_WARNING =	    	"Warning";
	private  final String DEFAULT_WARNING =	"Do_not_change_any_of_the_data_as_they_will_have_undocumented_side_effects.";
	private  final String P_WARNING_de =		"WarningD";
	private  final String DEFAULT_WARNING_de ="Einstellungen_nicht_aendern,_da_diese_undokumentierte_Nebenwirkungen_haben.";

	/** Charset */
	public  final String P_CHARSET = "Charset";
	/** Charser Default Value */
	private  final String DEFAULT_CHARSET = "UTF-8";

	/** Load tab fields meta data using background thread **/
	public  final String P_LOAD_TAB_META_DATA_BG = "LoadTabMetaDataBackground";

	public  final String DEFAULT_LOAD_TAB_META_DATA_BG = "N";

	/** Ini Properties		*/
	private  final String[]   PROPERTIES = new String[] {
		P_UID, P_PWD, P_TRACELEVEL, P_TRACEFILE,
		P_LANGUAGE, P_INI,
		P_CONNECTION, P_STORE_PWD,
		P_UI_LOOK, P_UI_THEME, /* P_UI_FLAT,*/
		P_A_COMMIT, P_A_LOGIN, P_A_NEW,
		P_ADEMPIERESYS, P_LOGMIGRATIONSCRIPT, P_SHOW_ACCT, P_SHOW_TRL,
		P_CACHE_WINDOW,
		P_CONTEXT, P_TEMP_DIR,
		P_ROLE, P_CLIENT, P_ORG, P_PRINTER, P_WAREHOUSE, P_TODAY,
		P_PRINTPREVIEW,
		P_VALIDATE_CONNECTION_ON_STARTUP,
		P_SINGLE_INSTANCE_PER_WINDOW,
		P_OPEN_WINDOW_MAXIMIZED,
		P_WARNING, P_WARNING_de,
		P_CHARSET, P_LOAD_TAB_META_DATA_BG
	};
	/** Ini Property Values	*/
	private  final String[]   VALUES = new String[] {
		DEFAULT_UID, DEFAULT_PWD, DEFAULT_TRACELEVEL, DEFAULT_TRACEFILE?"Y":"N",
		DEFAULT_LANGUAGE, DEFAULT_INI,
		DEFAULT_CONNECTION, DEFAULT_STORE_PWD?"Y":"N",
		DEFAULT_UI_LOOK, DEFAULT_UI_THEME, /* DEFAULT_UI_FLAT?"Y":"N", */
		DEFAULT_A_COMMIT?"Y":"N", DEFAULT_A_LOGIN?"Y":"N", DEFAULT_A_NEW?"Y":"N",
		DEFAULT_ADEMPIERESYS?"Y":"N", DEFAULT_LOGMIGRATIONSCRIPT?"Y":"N", DEFAULT_SHOW_ACCT?"Y":"N", DEFAULT_SHOW_TRL?"Y":"N",
		DEFAULT_CACHE_WINDOW?"Y":"N",
		DEFAULT_CONTEXT, DEFAULT_TEMP_DIR,
		DEFAULT_ROLE, DEFAULT_CLIENT, DEFAULT_ORG, DEFAULT_PRINTER, DEFAULT_WAREHOUSE, DEFAULT_TODAY.toString(),
		DEFAULT_PRINTPREVIEW?"Y":"N",
		DEFAULT_VALIDATE_CONNECTION_ON_STARTUP?"Y":"N",
		DEFAULT_SINGLE_INSTANCE_PER_WINDOW?"Y":"N",
		DEFAULT_OPEN_WINDOW_MAXIMIZED?"Y":"N",
		DEFAULT_WARNING, DEFAULT_WARNING_de,
		DEFAULT_CHARSET, DEFAULT_LOAD_TAB_META_DATA_BG
	};

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

	@Override
	protected void initEmptyFile() {
        checkProperties();
        save();
    }

	private  void checkProperties() {
		//	Check/set properties	defaults
		for (int i = 0; i < PROPERTIES.length; i++)
		{
			if (VALUES[i] != null && VALUES[i].length() > 0)
				checkProperty(PROPERTIES[i], VALUES[i]);
		}

		//
		String tempDir = System.getProperty("java.io.tmpdir");
		if (tempDir == null || tempDir.length() == 1)
			tempDir = getAdempiereHome();
		if (tempDir == null)
			tempDir = "";
		checkProperty(P_TEMP_DIR, tempDir);
	}

	/**
	 *	Load property and set to default, if not existing
	 *
	 * 	@param key   Key
	 * 	@param defaultValue   Default Value
	 * 	@return Property
	 */
	private  String checkProperty (String key, String defaultValue)
	{
		String result = null;
		if (key.equals(P_WARNING) || key.equals(P_WARNING_de))
			result = defaultValue;
		else if (!isClient())
			result = getProperties().getProperty (key, SecureInterface.CLEARVALUE_START + defaultValue + SecureInterface.CLEARVALUE_END);
		else
			result = getProperties().getProperty (key, SecureEngine.encrypt(defaultValue, 0));
        getProperties().setProperty (key, result);
		return result;
	}	//	checkProperty

	/**
	 *	Return File Name of INI file
	 *  <pre>
	 *  Examples:
	 *	    C:\WinNT\Profiles\jjanke\idempiere.properties
	 *      D:\idempiere\idempiere.properties
	 *      idempiere.properties
	 *  </pre>
	 *  Can be overwritten by -DPropertyFile=myFile allowing multiple
	 *  configurations / property files.
	 *  @param tryUserHome get user home first
	 *  @return file name
	 */
	public  String getFileName (boolean tryUserHome)
	{
		if (System.getProperty("PropertyFile") != null)
			return System.getProperty("PropertyFile");
		//
		String base = null;
		if (tryUserHome && s_client)
			base = System.getProperty("user.home");
		//  Server
		if (!s_client || base == null || base.length() == 0)
		{
			String home = getAdempiereHome();
			if (home != null)
				base = home;
		}
		if (base != null && !base.endsWith(File.separator))
			base += File.separator;
		if (base == null)
			base = "";
		//
		return base + getBasePropertyFileName();
	}	//	getFileName


	/**************************************************************************
	 *	Set Property
	 *  @param key   Key
	 *  @param value Value
	 */
	public  void setProperty (String key, String value)
	{
	//	log.finer(key + "=" + value);
		if (key.equals(P_WARNING) || key.equals(P_WARNING_de))
            getProperties().setProperty(key, value);
		else if (!isClient())
            getProperties().setProperty(key, SecureInterface.CLEARVALUE_START + value + SecureInterface.CLEARVALUE_END);
		else
		{
			if (value == null)
                getProperties().setProperty(key, "");
			else
			{
				String eValue = SecureEngine.encrypt(value, 0);
				if (eValue == null)
                    getProperties().setProperty(key, "");
				else
                    getProperties().setProperty(key, eValue);
			}
		}
	}	//	setProperty

	/**
	 *	Set Property
	 *  @param key   Key
	 *  @param value Value
	 */
	public  void setProperty (String key, boolean value)
	{
		setProperty (key, value ? "Y" : "N");
	}   //  setProperty

	/**
	 *	Set Property
	 *  @param key   Key
	 *  @param value Value
	 */
	public  void setProperty (String key, int value)
	{
		setProperty (key, String.valueOf(value));
	}   //  setProperty

	/**
	 *	Get Property
	 *  @param key  Key
	 *  @return     Value
	 */
	public  String getProperty (String key)
	{
		if (key == null)
			return "";
		String retStr = getProperties().getProperty(key, "");
		if (retStr == null || retStr.length() == 0)
			return "";
		//
		String value = SecureEngine.decrypt(retStr, 0);
	//	log.finer(key + "=" + value);
		if (value == null)
			return "";
		return value;
	}	//	getProperty

	/**
	 *	Get Property as Boolean
	 *  @param key  Key
	 *  @return     Value
	 */
	public  boolean isPropertyBool (String key)
	{
		return getProperty (key).equals("Y");
	}	//	getProperty

	/**
	 * 	Cache Windows
	 *	@return true if windows are cached
	 */
	public  boolean isCacheWindow()
	{
		return getProperty (P_CACHE_WINDOW).equals("Y");
	}	//	isCacheWindow

	/**************************************************************************
	 *  Get Properties
	 *
	 * @return Ini properties
	 */
	public  Properties getProperties()
	{
		return getProp();
	}   //  getProperties

	/**
	 *  toString
	 *  @return String representation
	 */
	public  String getAsString()
	{
		StringBuilder buf = new StringBuilder ("Ini[");
		Enumeration<?> e = getProperties().keys();
		while (e.hasMoreElements())
		{
			String key = (String)e.nextElement();
			buf.append(key).append("=");
			buf.append(getProperty(key)).append("; ");
		}
		buf.append("]");
		return buf.toString();
	}   //  toString


	/*************************************************************************/

	/** System environment prefix                                       */
	public  final String  ENV_PREFIX = "env.";
	/** System Property Value of IDEMPIERE_HOME                          */
	public  final String  IDEMPIERE_HOME = "IDEMPIERE_HOME";

	/** IsClient Internal marker            */
	private  boolean      s_client = false;
	/** IsClient Internal marker            */
	private  boolean      s_loaded = false;
	/** Show license dialog for first time **/
	private  boolean		s_license_dialog = true;

	/**
	 *  Are we in Client Mode ?
	 *  @return true if client
	 */
	public  boolean isClient()
	{
		return s_client;
	}   //  isClient

	/**
	 *  Set Client Mode
	 *  @param client client
	 */
	public  void setClient (boolean client)
	{
		s_client = client;
	}   //  setClient

	/**
	 * Set show license dialog for new setup
	 * @param b
	 */
	public  void setShowLicenseDialog(boolean b)
	{
		s_license_dialog = b;
	}

	/**
	 * Is show license dialog for new setup
	 * @return boolean
	 */
	public  boolean isShowLicenseDialog()
	{
		return s_license_dialog;
	}

	/**
	 *  Are the properties loaded?
	 *  @return true if properties loaded.
	 */
	public  boolean isLoaded()
	{
		return s_loaded;
	}   //  isLoaded

	/**
	 *  Get default (Home) directory
	 *  @return Home directory
	 */
	public  String getAdempiereHome()
	{
		//  Try Environment
		String retValue = getAdempiereHomeImpl();
		if (retValue == null)
			retValue = File.separator + "idempiere";
		return retValue;
	}   //  getHome

	/**
	 *  Get Idempiere Home from Environment
	 *  @return idempiereHome or null
	 */
	public  String getAdempiereHomeImpl()
	{
		String env = System.getProperty (ENV_PREFIX + IDEMPIERE_HOME);
		if (env == null || env.trim().length() == 0)
			env = System.getProperty (IDEMPIERE_HOME);
		if (env == null || env.trim().length() == 0)
		{
			//client - user home, server - current working directory
			String current = isClient() ? System.getProperty("user.home")
					: System.getProperty("user.dir");
			if (current != null && current.trim().length() > 0)
			{
				//check directory exists and writable
				File file = new File(current);
				if (file.exists() && file.canWrite())
				{
					env = current;
				}
			}
		}
		if (env == null || env.trim().length() == 0 )	//	Fallback
			env = File.separator + "idempiere";
		return env;
	}   //  getAdempiereHome

	/**
	 *  Set Idempiere Home
	 *  @param idempiereHome IDEMPIERE_HOME
	 */
	public  void setAdempiereHome (String idempiereHome)
	{
		if (idempiereHome != null && idempiereHome.length() > 0)
			System.setProperty (IDEMPIERE_HOME, idempiereHome);
	}   //  setAdempiereHome

	/**
	 * 	Find Idempiere Home
	 *	@return idempiere home or null
	 */
	public  String findAdempiereHome()
	{
		return getAdempiereHome();
	}	//	findAdempiereHome

	/**************************************************************************
	 * 	Get Window Dimension
	 *	@param AD_Window_ID window no
	 *	@return dimension or null
	 */
	public  Dimension getWindowDimension(int AD_Window_ID)
	{
		String key = "WindowDim" + AD_Window_ID;
		String value = (String)getProperties().get(key);
		if (value == null || value.length() == 0)
			return null;
		int index = value.indexOf('|');
		if (index == -1)
			return null;
		try
		{
			String w = value.substring(0, index);
			String h = value.substring(index+1);
			return new Dimension(Integer.parseInt(w),Integer.parseInt(h));
		}
		catch (Exception e)
		{
		}
		return null;
	}	//	getWindowDimension

	/**
	 * 	Set Window Dimension
	 *	@param AD_Window_ID window
	 *	@param windowDimension dimension - null to remove
	 */
	public  void setWindowDimension(int AD_Window_ID, Dimension windowDimension)
	{
		String key = "WindowDim" + AD_Window_ID;
		if (windowDimension != null)
		{
			String value = windowDimension.width + "|" + windowDimension.height;
			getProperties().put(key, value);
		}
		else
            getProperties().remove(key);
	}	//	setWindowDimension

	/**
	 * 	Get Window Location
	 *	@param AD_Window_ID window id
	 *	@return location or null
	 */
	public  Point getWindowLocation(int AD_Window_ID)
	{
		String key = "WindowLoc" + AD_Window_ID;
		String value = (String)getProperties().get(key);
		if (value == null || value.length() == 0)
			return null;
		int index = value.indexOf('|');
		if (index == -1)
			return null;
		try
		{
			String x = value.substring(0, index);
			String y = value.substring(index+1);
			return new Point(Integer.parseInt(x),Integer.parseInt(y));
		}
		catch (Exception e)
		{
		}
		return null;
	}	//	getWindowLocation

	/**
	 * 	Set Window Location
	 *	@param AD_Window_ID window
	 *	@param windowLocation location - null to remove
	 */
	public  void setWindowLocation(int AD_Window_ID, Point windowLocation)
	{
		String key = "WindowLoc" + AD_Window_ID;
		if (windowLocation != null)
		{
			String value = windowLocation.x + "|" + windowLocation.y;
            getProperties().put(key, value);
		}
		else
            getProperties().remove(key);
	}	//	setWindowLocation

	/**
	 * 	Get Divider Location
	 *	@return location
	 */
	public  int getDividerLocation()
	{
		String key = "Divider";
		String value = (String)getProperties().get(key);
		if (value == null || value.length() == 0)
			return 0;
		try
		{
			return Integer.parseInt(value);
		}
		catch (Exception e)
		{
		}
		return 0;
	}	//	getDividerLocation

	/**
	 * 	Set Divider Location
	 *	@param dividerLocation location
	 */
	public  void setDividerLocation(int dividerLocation)
	{
		String key = "Divider";
		String value = String.valueOf(dividerLocation);
        getProperties().put(key, value);
	}	//	setDividerLocation

	/**
	 * Get Available Encoding Charsets
	 * @return array of available encoding charsets
	 * @since 3.1.4
	 */
	public  Charset[] getAvailableCharsets() {
		Collection<Charset> col = Charset.availableCharsets().values();
		Charset[] arr = new Charset[col.size()];
		col.toArray(arr);
		return arr;
	}

	/**
	 * Get current charset
	 * @return current charset
	 * @since 3.1.4
	 */
	public  Charset getCharset() {
		String charsetName = getProperty(P_CHARSET);
		if (charsetName == null || charsetName.length() == 0)
			return Charset.defaultCharset();
		try {
			return Charset.forName(charsetName);
		} catch (Exception e) {
		}
		return Charset.defaultCharset();
	}

	public  String getPropertyFileName()
	{
		return getFileName();
	}

  public static void main(final String[] args) throws Exception{
		System.out.println(Ini.getIni().getAdempiereHome());
		Ini.getIni().setClient (false);
	}

  private static Ini instance;
    
  public Ini(){
      super(getBasePropertyFileName());
      instance = this;
  }

  public static Ini getIni(){
		if (instance==null) {
		    instance = (Ini) software.hsharp.core.util.Ini.Companion.load(
                getBasePropertyFileName(),
				(String fileName) -> { return new Ini(); }
            );
        }
        return instance;
  }
}	//	Ini
