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
package org.compiere.webutil;

import org.compiere.Adempiere;
import org.compiere.impl.MClient;
import org.compiere.impl.MSystem;
import org.idempiere.common.util.CLogMgt;
import org.idempiere.common.util.CLogger;
import org.idempiere.common.util.Ini;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;

/**
 *  Web Environment and debugging
 *
 *  @author Jorg Janke
 *  @version $Id: WebEnv.java,v 1.2 2006/07/30 00:51:05 jjanke Exp $
 */
public class WebEnv
{
	/** Add HTML Debug Info                     */
	public static boolean DEBUG                 = true;
	/**	Logging									*/
	private static CLogger log = CLogger.getCLogger(WebEnv.class);

	/**
	 *  Base Directory links <b>http://localhost:8080/adempiere</b>
	 *  to the physical <i>%adempiere_HOME%/jetty/webroot/adempiere</i> directory
	 */
	public static final String   	DIR_BASE    = "/adempiere";      //  /adempiere
	/** Image Sub-Directory under BASE          */
	private static final String     DIR_IMAGE   = "images";         //  /adempiere/images
	/** Stylesheet Name                         */
	private static final String     STYLE_STD   = "/css/standard.css";   //  /adempiere/standard.css
	/** Small Logo. */
	private static final String     LOGO        = "LogoSmall.gif";  //  /adempiere/LogoSmall.gif
	/** Store Sub-Directory under BASE          */
	private static final String     DIR_STORE   = "store";          //  /adempiere/store

	/**  Frame name for Commands - WCmd	*/
	public static final String      TARGET_CMD  = "WCmd";
	/**  Frame name for Menu - WMenu	*/
	public static final String      TARGET_MENU = "WMenu";
	/**  Frame name for Apps Window - WWindow	*/
	public static final String      TARGET_WINDOW = "WWindow";
	/**  Frame name for Apps PopUp - WPopUp		*/
	public static final String      TARGET_POPUP = "WPopUp";

	/** Character Set (iso-8859-1 - utf-8) 		*/
	public static final String      CHARSET = "UTF-8";     //  Default: UNKNOWN
	/** Encoding (ISO-8859-1 - UTF-8) 		*/
	public static final String      ENCODING = "UTF-8";
	/** Cookie Name                             */
	public static final String      COOKIE_INFO = "adempiereInfo";

	/** Timeout - 15 Minutes                    */
	public static final int         TIMEOUT     = 15*60;


	/** Initialization OK?                      */
	private static boolean          s_initOK    = false;
	/** Not Braking Space						*/
	public static String			NBSP = "&nbsp;";

	/**
	 *  Init Web Environment.
	 *  To be called from every Servlet in the init method
	 *  or any other Web resource to make sure that the
	 *  environment is properly set.
	 *  @param config config
	 *  @return false if initialization problems
	 */
	public static boolean initWeb (ServletConfig config)
	{
		if (s_initOK)
		{
			if (log.isLoggable(Level.INFO)) log.info(config.getServletName());
			return true;
		}

		Enumeration<String> en = config.getInitParameterNames();
		StringBuffer info = new StringBuffer("Servlet Init Parameter: ")
			.append(config.getServletName());
		while (en.hasMoreElements())
		{
			String name = en.nextElement();
			String value = config.getInitParameter(name);
			System.setProperty(name, value);
			info.append("\n").append(name).append("=").append(value);
		}

		boolean retValue = initWeb (config.getServletContext());

		//	Logging now initiated
		if (log.isLoggable(Level.INFO)) log.info(info.toString());
		return retValue;
	}   //  initWeb

	/**
	 * 	Init Web.
	 * 	Only call directly for Filters, etc.
	 *	@param context servlet context
	 *  @return false if initialization problems
	 */
	public static boolean initWeb (ServletContext context)
	{
		if (s_initOK)
		{
			if (log.isLoggable(Level.INFO)) log.info(context.getServletContextName());
			return true;
		}

		//  Load Environment Variables (serverApps/src/web/WEB-INF/web.xml)
		Enumeration<String> en = context.getInitParameterNames();
		StringBuffer info = new StringBuffer("Servlet Context Init Parameters: ")
			.append(context.getServletContextName());
		while (en.hasMoreElements())
		{
			String name = en.nextElement();
			String value = context.getInitParameter(name);
			System.setProperty(name, value);
			info.append("\n").append(name).append("=").append(value);
		}

		String propertyFile = Ini.getIni().getFileName(false);
		File file = new File(propertyFile);
		if (!file.exists())
		{
			throw new java.lang.IllegalStateException("idempiere.properties is not setup. PropertyFile="+propertyFile);
		}
		try
		{
			s_initOK = Adempiere.getI().startup(false);
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, "startup", ex);
		}
		if (!s_initOK)
			return false;

		//	Logging now initiated
		if (log.isLoggable(Level.INFO)) log.info(info.toString());
		//
		Properties ctx = new Properties();
		MClient client = MClient.get(ctx, 0);
		MSystem system = MSystem.get(ctx);
		client.sendEMail(client.getRequestEMail(),
			"Server started: " + system.getName() + " (" + WebUtil.getServerName() + ")",
			"ServerInfo: " + context.getServerInfo(), null);

		return s_initOK;
	}	//	initWeb


	/**************************************************************************
	 *  Get Base Directory entry.
	 *  <br>
	 *  /adempiere/
	 *  @param entry file entry or path
	 *  @return url to entry in base directory
	 */
	public static String getBaseDirectory (String entry)
	{
		StringBuilder sb = new StringBuilder (DIR_BASE);
		if (!entry.startsWith("/"))
			sb.append("/");
		sb.append(entry);
		return sb.toString();
	}   //  getBaseDirectory

	/**
	 *  Get Image Directory entry.
	 *  <br>
	 *  /adempiere/images
	 *  @param entry file entry or path
	 *  @return url to entry in image directory
	 */
	public static String getImageDirectory(String entry)
	{
		StringBuilder sb = new StringBuilder (DIR_BASE);
		sb.append("/").append(DIR_IMAGE);
		if (!entry.startsWith("/"))
			sb.append("/");
		sb.append(entry);
		return sb.toString();
	}   //  getImageDirectory

	/**
	 *  Get Store Directory entry.
	 *  <br>
	 *  /adempiere/store
	 *  @param entry file entry or path
	 *  @return url to entry in store directory
	 */
	public static String getStoreDirectory(String entry)
	{
		StringBuilder sb = new StringBuilder (DIR_BASE);
		sb.append("/").append(DIR_STORE);
		if (!entry.startsWith("/"))
			sb.append("/");
		sb.append(entry);
		return sb.toString();
	}   //  getStoreDirectory


	/**
	 * 	Get Cell Content
	 *	@param content optional content
	 *	@return string content or non breaking space
	 */
	public static String getCellContent (Object content)
	{
		if (content == null)
			return NBSP;
		String str = content.toString();
		if (str.length() == 0)
			return NBSP;
		return str;
	}	//	getCellContent

	/**
	 * 	Get Cell Content
	 *	@param content optional content
	 *	@return string content
	 */
	public static String getCellContent (int content)
	{
		return String.valueOf(content);
	}	//	getCellContent

	/**************************************************************************
	 * 	Dump Servlet Config
	 * 	@param config config
	 */
	public static void dump (ServletConfig config)
	{
		if (log.isLoggable(Level.CONFIG))log.config("ServletConfig " + config.getServletName());
		if (log.isLoggable(Level.CONFIG))log.config("- Context=" + config.getServletContext());
		if (!CLogMgt.isLevelFiner())
			return;
		boolean first = true;
		Enumeration<String> e = config.getInitParameterNames();
		while (e.hasMoreElements())
		{
			if (first)
				log.finer("InitParameter:");
			first = false;
			String key = e.nextElement();
			Object value = config.getInitParameter(key);
			if (log.isLoggable(Level.FINER)) log.finer("- " + key + " = " + value);
		}
	}	//	dump (ServletConfig)

	/**
	 * 	Dump Session
	 * 	@param ctx servlet context
	 */
	public static void dump (ServletContext ctx)
	{
		if (log.isLoggable(Level.CONFIG)) log.config("ServletContext " + ctx.getServletContextName());
		if (log.isLoggable(Level.CONFIG)) log.config("- ServerInfo=" + ctx.getServerInfo());
		if (!CLogMgt.isLevelFiner())
			return;
		boolean first = true;
		Enumeration<String> e = ctx.getInitParameterNames();
		while (e.hasMoreElements())
		{
			if (first)
				log.finer("InitParameter:");
			first = false;
			String key = e.nextElement();
			Object value = ctx.getInitParameter(key);
			if (log.isLoggable(Level.FINER)) log.finer("- " + key + " = " + value);
		}
		first = true;
		e = ctx.getAttributeNames();
		while (e.hasMoreElements())
		{
			if (first)
				log.finer("Attributes:");
			first = false;
			String key = e.nextElement();
			Object value = ctx.getAttribute(key);
			if (log.isLoggable(Level.FINER)) log.finer("- " + key + " = " + value);
		}
	}	//	dump

	/**
	 * 	Dump Session
	 * 	@param session session
	 */
	public static void dump (HttpSession session)
	{
		if (log.isLoggable(Level.CONFIG))log.config("Session " + session.getId());
		if (log.isLoggable(Level.CONFIG))log.config("- Created=" + new Timestamp(session.getCreationTime()));
		if (!CLogMgt.isLevelFiner())
			return;
		boolean first = true;
		Enumeration<String> e = session.getAttributeNames();
		while (e.hasMoreElements())
		{
			if (first)
				log.finer("Attributes:");
			first = false;
			String key = e.nextElement();
			Object value = session.getAttribute(key);
			if (log.isLoggable(Level.FINER)) log.finer("- " + key + " = " + value);
		}
	}	//	dump (session)

	/**
	 * 	Dump Request
	 * 	@param request request
	 */
	public static void dump (HttpServletRequest request)
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Request " + request.getProtocol() + " " + request.getMethod());
		if (!log.isLoggable(Level.FINER))
			return;
		log.finer("- Server="  + request.getServerName() + ", Port=" + request.getServerPort());
		log.finer("- ContextPath=" + request.getContextPath()
			+ ", ServletPath=" + request.getServletPath()
			+ ", Query=" + request.getQueryString());
		log.finer("- From " + request.getRemoteHost() + "/" + request.getRemoteAddr()
			//	+ ":" + request.getRemotePort()
				+ " - User=" + request.getRemoteUser());
		log.finer("- URI=" + request.getRequestURI() + ", URL=" + request.getRequestURL());
		log.finer("- AuthType=" + request.getAuthType());
		log.finer("- Secure=" + request.isSecure());
		log.finer("- PathInfo=" + request.getPathInfo() + " - " + request.getPathTranslated());
		log.finer("- UserPrincipal=" + request.getUserPrincipal());
		//
		boolean first = true;
		Enumeration<?> e = request.getHeaderNames();
		/** Header Names */
		while (e.hasMoreElements())
		{
			if (first)
				log.finer("- Header:");
			first = false;
			String key = (String)e.nextElement();
			Object value = request.getHeader(key);
			log.finer("  - " + key + " = " + value);
		}
		/** **/
		first = true;
		/** Parameter	*/
		try
		{
			String enc = request.getCharacterEncoding();
			if (enc == null)
				request.setCharacterEncoding(WebEnv.ENCODING);
		}
		catch (Exception ee)
		{
			log.log(Level.SEVERE, "Set CharacterEncoding=" + WebEnv.ENCODING, ee);
		}
		e = request.getParameterNames();
		while (e.hasMoreElements())
		{
			if (first)
				log.finer("- Parameter:");
			first = false;
			String key = (String)e.nextElement();
			String value = WebUtil.getParameter (request, key);
			log.finer("  - " + key + " = " + value);
		}
		first = true;
		/** Attributes	*/
		e = request.getAttributeNames();
		while (e.hasMoreElements())
		{
			if (first)
				log.finer("- Attributes:");
			first = false;
			String key = (String)e.nextElement();
			Object value = request.getAttribute(key);
			log.finer("  - " + key + " = " + value);
		}
		/** Cookies	*/
		Cookie[] ccc = request.getCookies();
		if (ccc != null)
		{
			for (int i = 0; i < ccc.length; i++)
			{
				if (i == 0)
					log.finer("- Cookies:");
				log.finer ("  - " + ccc[i].getName ()
					+ ", Domain=" + ccc[i].getDomain ()
					+ ", Path=" + ccc[i].getPath ()
					+ ", MaxAge=" + ccc[i].getMaxAge ());
			}
		}
		log.finer("- Encoding=" + request.getCharacterEncoding());
		log.finer("- Locale=" + request.getLocale());
		first = true;
		e = request.getLocales();
		while (e.hasMoreElements())
		{
			if (first)
				log.finer("- Locales:");
			first = false;
			log.finer("  - " + e.nextElement());
		}
		log.finer("- Class=" + request.getClass().getName());
	}	//	dump (Request)


}   //  WEnv
