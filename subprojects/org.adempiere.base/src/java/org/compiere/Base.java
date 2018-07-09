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

/**
 *  Base Library Test Classes mainly for Optimize it
 *
 *  @author Jorg Janke
 *  @version $Id: Base.java,v 1.5 2006/09/21 20:44:54 jjanke Exp $
 */
class Base 
{

	/**
	 *  Get Used Memory in bytes
	 *  @return memory used
	 */
	private static long getMemoryUsed()
	{
		long free = Runtime.getRuntime().freeMemory();
		long total = Runtime.getRuntime().totalMemory();
		long used = total - free;
		//
		System.out.println("Memory used in kB = Total("
			+ String.valueOf(total/1024) + ")-Free("
			+ String.valueOf(free/1024) + ") = " + String.valueOf(used/1024));
		System.out.println("Active Threads=" + Thread.activeCount());
		return used;
	}   //  getMemoryUsed

	/**
	 *  List Threads
	 */
	private static void listThreads()
	{
		Thread[] list = new Thread[Thread.activeCount()];
	//	int no = Thread.currentThread().enumerate(list);
		for (int i = 0; i < list.length; i++)
		{
			if (list[i] != null)
				System.out.println("Thread " + i + " - " + list[i].toString());
		}
	}   //  listThreads

	/**
	 *  Start
	 *  @param args ignored
	 
	public static void main(String[] args)
	{
		
	}   //  main*/
}   //  Base
