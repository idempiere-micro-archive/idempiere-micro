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

import org.compiere.impl.*;
import org.compiere.orm.MAttachment;
import org.compiere.orm.MAttachmentEntry;
import org.compiere.util.DisplayType;
import org.idempiere.common.util.CLogger;
import org.idempiere.common.util.Env;
import org.idempiere.common.util.Language;
import org.idempiere.common.util.MimeType;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;

/**
 *  Servlet Utilities
 *
 *  @author Jorg Janke
 *  @version  $Id: WebUtil.java,v 1.7 2006/09/24 12:11:54 comdivision Exp $
 */
public final class WebUtil
{
	/**	Static Logger	*/
	private static CLogger log	= CLogger.getCLogger (WebUtil.class);

	
	
	/**************************************************************************
	 *  Get Cookie Properties
	 *
	 *  @param request request
	 *  @return Properties
	 */
	public static Properties getCookieProprties(HttpServletRequest request)
	{
		//  Get Properties
		Cookie[] cookies = request.getCookies();
		if (cookies != null)
		{
			for (int i = 0; i < cookies.length; i++)
			{
				if (cookies[i].getName().equals(WebEnv.COOKIE_INFO))
					return propertiesDecode(cookies[i].getValue());
			}
		}
		return new Properties();
	}   //  getProperties

	
	/**
	 *  Get String Parameter.
	 *
	 *  @param request request
	 *  @param parameter parameter
	 *  @return string or null
	 */
	public static String getParameter (HttpServletRequest request, String parameter)
	{
		if (request == null || parameter == null)
			return null;
		String enc = request.getCharacterEncoding();
		try
		{
			if (enc == null)
			{
				request.setCharacterEncoding(WebEnv.ENCODING);
				enc = request.getCharacterEncoding();
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Set CharacterEncoding=" + WebEnv.ENCODING, e);
			enc = request.getCharacterEncoding();
		}
		String data = request.getParameter(parameter);
		if (data == null || data.length() == 0)
			return data;
		
		//	Convert
		if (enc != null && !WebEnv.ENCODING.equals(enc))
		{
			try
			{
				String dataEnc = new String(data.getBytes(enc), WebEnv.ENCODING);
				if (log.isLoggable(Level.FINER))log.log(Level.FINER, "Convert " + data + " (" + enc + ")-> " 
						+ dataEnc + " (" + WebEnv.ENCODING + ")");
				data = dataEnc;
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Convert " + data + " (" + enc + ")->" + WebEnv.ENCODING);
			}
		}
		
		//	Convert &#000; to character (JSTL input)
		String inStr = data;
		StringBuilder outStr = new StringBuilder();
		int i = inStr.indexOf("&#");
		while (i != -1)
		{
			outStr.append(inStr.substring(0, i));			// up to &#
			inStr = inStr.substring(i+2, inStr.length());	// from &#

			int j = inStr.indexOf(';');						// next ;
			if (j < 0)										// no second tag
			{
				inStr = "&#" + inStr;
				break;
			}

			String token = inStr.substring(0, j);
			try
			{
				int intToken = Integer.parseInt(token);
				outStr.append((char)intToken);				// replace context
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Token=" + token, e);
				outStr.append("&#").append(token).append(";");
			}
			inStr = inStr.substring(j+1, inStr.length());	// from ;
			i = inStr.indexOf("&#");
		}

		outStr.append(inStr);           					//	add remainder
		String retValue = outStr.toString();
		/**
		StringBuffer debug = new StringBuffer();
		char[] cc = data.toCharArray();
		for (int j = 0; j < cc.length; j++)
		{
			debug.append(cc[j]);
			int iii = (int)cc[j];
			debug.append("[").append(iii).append("]");
		}
		log.finest(parameter + "=" + data + " -> " + retValue + " == " + debug);
		**/
		if (log.isLoggable(Level.FINEST)) log.finest(parameter + "=" + data + " -> " + retValue);
		return retValue;
	}   //  getParameter

	/**
	 *  Get integer Parameter - 0 if not defined.
	 *
	 *  @param request request
	 *  @param parameter parameter
	 *  @return int result or 0
	 */
	public static int getParameterAsInt (HttpServletRequest request, String parameter)
	{
		if (request == null || parameter == null)
			return 0;
		String data = getParameter(request, parameter);
		if (data == null || data.length() == 0)
			return 0;
		try
		{
			return Integer.parseInt(data);
		}
		catch (Exception e)
		{
			log.warning (parameter + "=" + data + " - " + e);
		}
		return 0;
	}   //  getParameterAsInt

	/**
	 *  Get numeric Parameter - 0 if not defined
	 *
	 *  @param request request
	 *  @param parameter parameter
	 *  @return big decimal result or 0
	 */
	public static BigDecimal getParameterAsBD (HttpServletRequest request, String parameter)
	{
		if (request == null || parameter == null)
			return Env.ZERO;
		String data = getParameter(request, parameter);
		if (data == null || data.length() == 0)
			return Env.ZERO;
		try
		{
			return new BigDecimal (data);
		}
		catch (Exception e)
		{
		}
		try
		{
			DecimalFormat format = DisplayType.getNumberFormat(DisplayType.Number);
			Object oo = format.parseObject(data);
			if (oo instanceof BigDecimal)
				return (BigDecimal)oo;
			else if (oo instanceof Number)
				return BigDecimal.valueOf(((Number)oo).doubleValue());
			return new BigDecimal (oo.toString());
		}
		catch (Exception e)
		{
			if (log.isLoggable(Level.FINE)) log.fine(parameter + "=" + data + " - " + e);
		}
		return Env.ZERO;
	}   //  getParameterAsBD

	/**
	 *  Get date Parameter - null if not defined.
	 *	Date portion only
	 *  @param request request
	 *  @param parameter parameter
	 *  @return timestamp result or null
	 */
	public static Timestamp getParameterAsDate (HttpServletRequest request, 
		String parameter)
	{
		return getParameterAsDate (request, parameter, null);
	}	//	getParameterAsDate
	
	/**
	 *  Get date Parameter - null if not defined.
	 *	Date portion only
	 *  @param request request
	 *  @param parameter parameter
	 *  @param language optional language
	 *  @return timestamp result or null
	 */
	public static Timestamp getParameterAsDate (HttpServletRequest request, 
		String parameter, Language language)
	{
		if (request == null || parameter == null)
			return null;
		String data = getParameter(request, parameter);
		if (data == null || data.length() == 0)
			return null;
		
		//	Language Date Format
		if (language != null)
		{
			try
			{
				DateFormat format = DisplayType.getDateFormat(DisplayType.Date, language);
				java.util.Date date = format.parse(data);
				if (date != null)
					return new Timestamp (date.getTime());
			}
			catch (Exception e)
			{
			}
		}
		
		//	Default Simple Date Format
		try
		{
			SimpleDateFormat format = DisplayType.getDateFormat(DisplayType.Date);
			java.util.Date date = format.parse(data);
			if (date != null)
				return new Timestamp (date.getTime());
		}
		catch (Exception e)
		{
		}
		
		//	JDBC Format
		try 
		{
			return Timestamp.valueOf(data);
		}
		catch (Exception e) 
		{
		}
		
		log.warning(parameter + " - cannot parse: " + data);
		return null;
	}   //  getParameterAsDate

	/**
	 *  Get boolean Parameter.
	 *  @param request request
	 *  @param parameter parameter
	 *  @return true if found
	 */
	public static boolean getParameterAsBoolean (HttpServletRequest request, 
		String parameter)
	{
		return getParameterAsBoolean(request, parameter, null);
	}	//	getParameterAsBoolean
	
	/**
	 *  Get boolean Parameter.
	 *  @param request request
	 *  @param parameter parameter
	 *  @param expected optional expected value
	 *  @return true if found and if optional value matches
	 */
	public static boolean getParameterAsBoolean (HttpServletRequest request, 
		String parameter, String expected)
	{
		if (request == null || parameter == null)
			return false;
		String data = getParameter(request, parameter);
		if (data == null || data.length() == 0)
			return false;
		//	Ignore actual value
		if (expected == null)
			return true;
		//
		return expected.equalsIgnoreCase(data);
	}   //  getParameterAsBoolean
	
    /**
     * 	get Parameter or Null fi empty
     *	@param request request
     *	@param parameter parameter
     *	@return Request Value or null
     */
    public static String getParamOrNull (HttpServletRequest request, String parameter)
    {
        String value = WebUtil.getParameter(request, parameter);
        if(value == null) 
        	return value;
        if (value.length() == 0) 
        	return null;
        return value;
    }	//	getParamOrNull
    

    /**
     * 	reload
     *	@param logMessage
     *	@param jsp
     *	@param session
     *	@param request
     *	@param response
     *	@param thisContext
     *	@throws ServletException
     *	@throws IOException
     */
    public static void reload(String logMessage, String jsp, HttpSession session, HttpServletRequest request, HttpServletResponse response, ServletContext thisContext)
            throws ServletException, IOException
    {
        session.setAttribute(WebSessionCtx.HDR_MESSAGE, logMessage);
        log.warning(" - " + logMessage + " - update not confirmed");
        thisContext.getRequestDispatcher(jsp).forward(request, response);
    }
	


	/**
	 * 	Does Test exist
	 *	@param test string
	 *	@return true if String with data
	 */
	public static boolean exists (String test)
	{
		if (test == null)
			return false;
		return test.length() > 0;
	}	//	exists

	/**
	 * 	Does Parameter exist
	 * 	@param request request
	 *	@param parameter string
	 *	@return true if String with data
	 */
	public static boolean exists (HttpServletRequest request, String parameter)
	{
		if (request == null || parameter == null)
			return false;
		try
		{
			String enc = request.getCharacterEncoding();
			if (enc == null)
				request.setCharacterEncoding(WebEnv.ENCODING);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Set CharacterEncoding=" + WebEnv.ENCODING, e);
		}
		return exists (request.getParameter(parameter));
	}	//	exists


	/**
	 *	Is EMail address valid
	 * 	@param email mail address
	 * 	@return true if valid
	 */
	public static boolean isEmailValid (String email)
	{
		if (email == null || email.length () == 0)
			return false;
		try
		{
			InternetAddress ia = new InternetAddress (email, true);
			if (ia != null)
				return true;
		}
		catch (AddressException ex)
		{
			log.warning (email + " - "
				+ ex.getLocalizedMessage ());
		}
		return false;
	}	//	isEmailValid


	/**************************************************************************
	 *  Decode Properties into String (URL encoded)
	 *
	 *  @param pp properties
	 *  @return Encoded String
	 */
	public static String propertiesEncode (Properties pp)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try
		{
			pp.store(bos, "adempiere");   //  Header
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "store", e);
		}
		String result = new String (bos.toByteArray());
	//	System.out.println("String=" + result);
		try
		{
			result = URLEncoder.encode(result, WebEnv.ENCODING);
		}
		catch (UnsupportedEncodingException e)
		{
			log.log(Level.SEVERE, "encode" + WebEnv.ENCODING, e);
			String enc = System.getProperty("file.encoding");      //  Windows default is Cp1252
			try
			{
				result = URLEncoder.encode(result, enc);
				if (log.isLoggable(Level.INFO)) log.info("encode: " + enc);
			}
			catch (Exception ex)
			{
				log.log(Level.SEVERE, "encode", ex);
			}
		}
	//	System.out.println("String-Encoded=" + result);
		return result;
	}   //  propertiesEncode

	/**
	 *  Decode data String (URL encoded) into Properties
	 *
	 *  @param data data
	 *  @return Properties
	 */
	public static Properties propertiesDecode (String data)
	{
		String result = null;
	//	System.out.println("String=" + data);
		try
		{
			result = URLDecoder.decode(data, WebEnv.ENCODING);
		}
		catch (UnsupportedEncodingException e)
		{
			log.log(Level.SEVERE, "decode" + WebEnv.ENCODING, e);
			String enc = System.getProperty("file.encoding");      //  Windows default is Cp1252
			try
			{
				result = URLEncoder.encode(data, enc);
				log.log(Level.SEVERE, "decode: " + enc);
			}
			catch (Exception ex)
			{
				log.log(Level.SEVERE, "decode", ex);
			}
		}
	//	System.out.println("String-Decoded=" + result);

		ByteArrayInputStream bis = new ByteArrayInputStream(result.getBytes());
		Properties pp = new Properties();
		try
		{
			pp.load(bis);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "load", e);
		}
		return pp;
	}   //  propertiesDecode

	



	
	/**
	 * 	Stream Attachment Entry
	 *	@param response response
	 *	@param attachment attachment
	 *	@param attachmentIndex logical index
	 *	@return error message or null
	 */
	public static String streamAttachment (HttpServletResponse response,
                                           MAttachment attachment, int attachmentIndex)
	{
		if (attachment == null)
			return "No Attachment";
		
		int realIndex = -1;
		MAttachmentEntry[] entries = attachment.getEntries();
		for (int i = 0; i < entries.length; i++)
		{
			MAttachmentEntry entry = entries[i];
			if (entry.getIndex() == attachmentIndex)
			{
				realIndex = i;
				break;
			}
		}
		if (realIndex < 0)
		{
			if (log.isLoggable(Level.FINE)) log.fine("No Attachment Entry for Index=" 
				+ attachmentIndex + " - " + attachment);
			return "Attachment Entry not found";
		}
		
		MAttachmentEntry entry = entries[realIndex];
		if (entry.getData() == null)
		{
			if (log.isLoggable(Level.FINE)) log.fine("Empty Attachment Entry for Index=" 
				+ attachmentIndex + " - " + attachment);
			return "Attachment Entry empty";
		}
		
		//	Stream Attachment Entry
		try
		{
			int bufferSize = 2048; //	2k Buffer
			int fileLength = entry.getData().length;
			//
			response.setContentType(entry.getContentType());
			response.setBufferSize(bufferSize);
			response.setContentLength(fileLength);
			//
			if (log.isLoggable(Level.FINE)) log.fine(entry.toString());
			long time = System.currentTimeMillis();		//	timer start
			//
			ServletOutputStream out = response.getOutputStream ();
			out.write (entry.getData());
			out.flush();
			out.close();
			//
			time = System.currentTimeMillis() - time;
			double speed = (fileLength/(double)1024) / (time/(double)1000);
			if (log.isLoggable(Level.INFO)) log.info("Length=" 
				+ fileLength + " - " 
				+ time + " ms - " 
				+ speed + " kB/sec - " + entry.getContentType());
		}
		catch (IOException ex)
		{
			log.log(Level.SEVERE, ex.toString());
			return "Streaming error - " + ex;
		}
		return null;
	}	//	streamAttachment

	
	/**
	 * 	Stream File
	 *	@param response response
	 *	@param file file to stream
	 *	@return error message or null
	 */
	public static String streamFile (HttpServletResponse response, File file)
	{
		if (file == null)
			return "No File";
		if (!file.exists())
			return "File not found: " + file.getAbsolutePath();
		
		MimeType mimeType = MimeType.get(file.getAbsolutePath());
		//	Stream File
		try
		{
			int bufferSize = 2048; //	2k Buffer
			int fileLength = (int)file.length();
			//
			response.setContentType(mimeType.getMimeType());
			response.setBufferSize(bufferSize);
			response.setContentLength(fileLength);
			//
			if (log.isLoggable(Level.FINE)) log.fine(file.toString());
			long time = System.currentTimeMillis();		//	timer start
			//	Get Data
			FileInputStream in = new FileInputStream(file);
			ServletOutputStream out = response.getOutputStream ();
			int c = 0;
			while ((c = in.read()) != -1)
				out.write(c);
			//
			out.flush();
			out.close();
			in.close();
			//
			time = System.currentTimeMillis() - time;
			double speed = (fileLength/(double)1024) / (time/(double)1000);
			if (log.isLoggable(Level.INFO)) log.info("Length=" 
				+ fileLength + " - " 
				+ time + " ms - " 
				+ speed + " kB/sec - " + mimeType);
		}
		catch (IOException ex)
		{
			log.log(Level.SEVERE, ex.toString());
			return "Streaming error - " + ex;
		}
		return null;
	}	//	streamFile
	
	
	/**
	 * 	Remove Cookie with web user by setting user to _
	 * 	@param request request (for context path)
	 * 	@param response response to add cookie
	 */
	public static void deleteCookieWebUser (HttpServletRequest request, HttpServletResponse response, String COOKIE_NAME)
	{
		Cookie cookie = new Cookie(COOKIE_NAME, " ");
		cookie.setComment("adempiere Web User");
		cookie.setPath(request.getContextPath());
		cookie.setMaxAge(1);      //  second
		response.addCookie(cookie);
	}	//	deleteCookieWebUser
	
	/**************************************************************************
	 * 	Send EMail
	 *	@param request request
	 *	@param to web user
	 *	@param msgType see MMailMsg.MAILMSGTYPE_*
	 *	@param parameter object array with parameters
	 * 	@return mail EMail.SENT_OK or error message 
	 */
	public static String sendEMail (HttpServletRequest request, WebUser to,
		String msgType, Object[] parameter)
	{
		WebSessionCtx wsc = WebSessionCtx.get(request);
		MStore wStore = wsc.wstore;
		MMailMsg mailMsg = wStore.getMailMsg(msgType);
		//
		StringBuilder subject = new StringBuilder(mailMsg.getSubject());
		if (parameter.length > 0 && parameter[0] != null)
			subject.append(parameter[0]);
		//
		StringBuilder message = new StringBuilder();
		String hdr = wStore.getEMailFooter();
		if (hdr != null && hdr.length() > 0)
			message.append(hdr).append("\n");
		message.append(mailMsg.getMessage());
		if (parameter.length > 1 && parameter[1] != null)
			message.append(parameter[1]);
		if (mailMsg.getMessage2() != null)
		{
			message.append("\n")
				.append(mailMsg.getMessage2());
			if (parameter.length > 2 && parameter[2] != null)
				message.append(parameter[2]);
		}
		if (mailMsg.getMessage3() != null)
		{
			message.append("\n")
				.append(mailMsg.getMessage3());
			if (parameter.length > 3 && parameter[3] != null)
				message.append(parameter[3]);
		}
		message.append(MRequest.SEPARATOR)
			.append("http://").append(request.getServerName()).append(request.getContextPath())
			.append("/ - ").append(wStore.getName())
			.append("\n").append("Request from: ").append(getFrom(request))
			.append("\n");
		String ftr = wStore.getEMailFooter();
		if (ftr != null && ftr.length() > 0)
			message.append(ftr);
		
		//	Create Mail
		EMail email = wStore.createEMail(to.getEmail(),
			subject.toString(), message.toString());
		//	CC Order
		if (msgType.equals(MMailMsg.MAILMSGTYPE_OrderAcknowledgement))
		{
			String orderEMail = wStore.getWebOrderEMail();
			String storeEMail = wStore.getWStoreEMail();
			if (orderEMail != null && orderEMail.length() > 0
				&& !orderEMail.equals(storeEMail))	//	already Bcc
				email.addBcc(orderEMail);
		}

		//	Send
		String retValue = email.send();
		//	Log
		MUserMail um = new MUserMail(mailMsg, to.getAD_User_ID(), email);
		um.saveEx();
		//
		return retValue;
	}	//	sendEMail
	
	/**
	 * 	Get Remote From info
	 * 	@param request request
	 * 	@return remore info
	 */
	public static String getFrom (HttpServletRequest request)
	{
		String host = request.getRemoteHost();
		if (!host.equals(request.getRemoteAddr()))
			host += " (" + request.getRemoteAddr() + ")";
		return host;
	}	//	getFrom

	/**
	 * 	Add Cookie with web user
	 * 	@param request request (for context path)
	 * 	@param response response to add cookie
	 * 	@param webUser email address
	 */
	public static void addCookieWebUser (HttpServletRequest request, HttpServletResponse response, String webUser, String COOKIE_NAME)
	{
		Cookie cookie = new Cookie(COOKIE_NAME, webUser);
		cookie.setComment("adempiere Web User");
		cookie.setPath(request.getContextPath());
		cookie.setMaxAge(2592000);      //  30 days in seconds   60*60*24*30
		response.addCookie(cookie);
	}	//	setCookieWebUser

	/**
	 * 	Resend Validation Code
	 * 	@param request request
	 *	@param wu user
	 */
	public static void resendCode(HttpServletRequest request, WebUser wu)
	{
		String msg = sendEMail(request, wu, 
			MMailMsg.MAILMSGTYPE_UserVerification,
			new Object[]{
				request.getServerName(),
				wu.getName(),
				wu.getEMailVerifyCode()});
		if (EMail.SENT_OK.equals(msg))
			wu.setPasswordMessage ("EMail sent");
		else
			wu.setPasswordMessage ("Problem sending EMail: " + msg);
	}	//	resendCode
	

	/**
	 * 	Update Web User
	 * 	@param request request
	 * 	@param wu user
	 * 	@param updateEMailPwd if true, change email/password
	 * 	@return true if saved
	 */
	public static boolean updateFields (HttpServletRequest request, WebUser wu, boolean updateEMailPwd)
	{
		if (updateEMailPwd)
		{
			String s = WebUtil.getParameter (request, "PasswordNew");
			wu.setPasswordMessage (null);
			wu.setPassword (s);
			if (wu.getPasswordMessage () != null)
            {
                return false;
            }
			//
			s = WebUtil.getParameter (request, "EMail");
			if (!WebUtil.isEmailValid (s))
			{
				wu.setPasswordMessage ("EMail Invalid");
				return false;
			}
			wu.setEmail (s.trim());
		}
		//
		StringBuilder mandatory = new StringBuilder();
		String s = WebUtil.getParameter (request, "Name");
		if (s != null && s.length() != 0)
			wu.setName(s.trim());
		else
			mandatory.append(" - Name");
		s = WebUtil.getParameter (request, "Company");
		if (s != null && s.length() != 0)
			wu.setCompany(s);
		s = WebUtil.getParameter (request, "Title");
		if (s != null && s.length() != 0)
			wu.setTitle(s);
		//
		s = WebUtil.getParameter (request, "Address");
		if (s != null && s.length() != 0)
			wu.setAddress(s);
		else
			mandatory.append(" - Address");
		s = WebUtil.getParameter (request, "Address2");
		if (s != null && s.length() != 0)
			wu.setAddress2(s);
		//
		s = WebUtil.getParameter (request, "City");
		if (s != null && s.length() != 0)
			wu.setCity(s);
		else
			mandatory.append(" - City");
		s = WebUtil.getParameter (request, "Postal");
		if (s != null && s.length() != 0)
			wu.setPostal(s);
		else
			mandatory.append(" - Postal");
		//	Set Country before Region for validation
		s = WebUtil.getParameter (request, "C_Country_ID");
		if (s != null && s.length() != 0)
			wu.setC_Country_ID(s);
		s = WebUtil.getParameter (request, "C_Region_ID");
		if (s != null && s.length() != 0)
			wu.setC_Region_ID(s);
		s = WebUtil.getParameter (request, "RegionName");
		if (s != null && s.length() != 0)
			wu.setRegionName(s);
		//
		s = WebUtil.getParameter (request, "Phone");
		if (s != null && s.length() != 0)
			wu.setPhone(s);
		s = WebUtil.getParameter (request, "Phone2");
		if (s != null && s.length() != 0)
			wu.setPhone2(s);
		s = WebUtil.getParameter (request, "C_BP_Group_ID");
		if (s != null && s.length() != 0)
			wu.setC_BP_Group_ID (s);
		s = WebUtil.getParameter (request, "Fax");
		if (s != null && s.length() != 0)
			wu.setFax(s);
		//
		if (mandatory.length() > 0)
		{
			mandatory.insert(0, "Enter Mandatory");
			wu.setSaveErrorMessage(mandatory.toString());
			return false;
		}
		return wu.save();
	}	//	updateFields
	
	/**
	 * 
	 * @return Servername including host name: IP : instance name
	 */
	public static String getServerName(){
		StringBuilder strBuilder = new StringBuilder();
		
		try {
			strBuilder.append(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			log.log(Level.WARNING, "Local host or IP not found", e);
		}
		strBuilder.append(":").append(getHostIP());
		
			
		return strBuilder.toString();
	}
	
	public static String getHostIP() {
		String retVal = null;
		try {
			InetAddress localAddress= InetAddress.getLocalHost();
			if (!localAddress.isLinkLocalAddress() && !localAddress.isLoopbackAddress() && localAddress.isSiteLocalAddress())
				return localAddress.getHostAddress();
		} catch (UnknownHostException e) {
			log.log(Level.WARNING,
					"UnknownHostException while retrieving host ip");
		}
		
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()
							&& !inetAddress.isLinkLocalAddress()
							&& inetAddress.isSiteLocalAddress()) {
						retVal = inetAddress.getHostAddress().toString();
						break;
					}
				}
			}
		} catch (SocketException e) {
			log.log(Level.WARNING, "Socket Exeception while retrieving host ip");
		}

		if (retVal == null) {
			try {
				retVal = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				log.log(Level.WARNING,
						"UnknownHostException while retrieving host ip");
			}
		}
		return retVal;
	}

}   //  WebUtil
