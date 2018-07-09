/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2012 Trek Global                                             *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/

package org.compiere.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.compiere.model.IAttachmentStore;
import org.compiere.model.I_AD_Attachment;
import org.compiere.model.I_AD_AttachmentEntry;
import org.compiere.model.I_AD_StorageProvider;
import org.compiere.orm.MAttachment;
import org.compiere.orm.MAttachmentEntry;
import org.idempiere.common.util.CLogger;

public class AttachmentDBSystem implements IAttachmentStore
{
	
	/** Indicator for zip data  */
	public static final String 	ZIP = "zip";
	private final CLogger log = CLogger.getCLogger(getClass());


	@Override
	public boolean loadLOBData(I_AD_Attachment attach, I_AD_StorageProvider prov) {
//		Reset
		attach.prepareItems();
			//
			byte[] data = attach.getBinaryData();
			if (data == null)
				return true;
			if (log.isLoggable(Level.FINE)) log.fine("ZipSize=" + data.length);
			if (data.length == 0)
				return true;

			//	Old Format - single file
			if (!ZIP.equals(attach.getTitle()))
			{
				attach.getItems().add (new MAttachmentEntry(attach.getTitle(), data, 1));
				return true;
			}

			try
			{
				ByteArrayInputStream in = new ByteArrayInputStream(data);
				ZipInputStream zip = new ZipInputStream (in);
				ZipEntry entry = zip.getNextEntry();
				while (entry != null)
				{
					String name = entry.getName();
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					byte[] buffer = new byte[2048];
					int length = zip.read(buffer);
					while (length != -1)
					{
						out.write(buffer, 0, length);
						length = zip.read(buffer);
					}
					//
					byte[] dataEntry = out.toByteArray();
					if (log.isLoggable(Level.FINE)) log.fine(name 
						+ " - size=" + dataEntry.length + " - zip="
						+ entry.getCompressedSize() + "(" + entry.getSize() + ") "
						+ (entry.getCompressedSize()*100/entry.getSize())+ "%");
					//
					attach.getItems().add (new MAttachmentEntry (name, dataEntry, attach.getItems().size()+1));
					entry = zip.getNextEntry();
				}
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "loadLOBData", e);
				attach.cleanItems();
				return false;
			}
			return true;
	}

	@Override
	public boolean save(I_AD_Attachment attach, I_AD_StorageProvider prov) {
		if (attach.getItems() == null || attach.getItems().size() == 0)
		{
			attach.setBinaryData(null);
			return true;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		ZipOutputStream zip = new ZipOutputStream(out);
		zip.setMethod(ZipOutputStream.DEFLATED);
		zip.setLevel(Deflater.BEST_COMPRESSION);
		zip.setComment("iDempiere");
		//
		try
		{
			for (int i = 0; i < attach.getItems().size(); i++)
			{
				I_AD_AttachmentEntry item = attach.getItems().get(i);
				ZipEntry entry = new ZipEntry(item.getName());
				entry.setTime(System.currentTimeMillis());
				entry.setMethod(ZipEntry.DEFLATED);
				zip.putNextEntry(entry);
				byte[] data = item.getData();
				zip.write (data, 0, data.length);
				zip.closeEntry();
				if (log.isLoggable(Level.FINE)) log.fine(entry.getName() + " - "
					+ entry.getCompressedSize() + " (" + entry.getSize() + ") "
					+ (entry.getCompressedSize()*100/entry.getSize())+ "%");
			}
		//	zip.finish();
			zip.close();
			byte[] zipData = out.toByteArray();
			if (log.isLoggable(Level.FINE)) log.fine("Length=" +  zipData.length);
			attach.setBinaryData(zipData);
			attach.setTitle(MAttachment.ZIP);
			return true;
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "saveLOBData", e);
		}
		attach.setBinaryData(null);
		return false;
	}

	@Override
	public boolean delete(I_AD_Attachment attach, I_AD_StorageProvider prov) {
		// nothing todo - deleting the db record deletes the items
		return true;
	}

	@Override
	public boolean deleteEntry(I_AD_Attachment attach, I_AD_StorageProvider provider, int index) {
		attach.getItems().remove(index);
		return true;
	}
	
}
