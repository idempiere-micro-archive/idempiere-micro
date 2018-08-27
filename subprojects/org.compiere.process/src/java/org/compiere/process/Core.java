/******************************************************************************
 * This file is part of Adempiere ERP Bazaar                                  *
 * http://www.adempiere.org                                                   *
 *                                                                            *
 * Copyright (C) Jorg Viola			                                          *
 * Copyright (C) Contributors												  *
 *                                                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *                                                                            *
 * Contributors:                                                              *
 * - Heng Sin Low                                                             *
 *****************************************************************************/
package org.compiere.process;

import org.compiere.util.PaymentExport;
import org.compiere.util.ReplenishInterface;
import org.idempiere.common.base.Service;
import org.idempiere.common.util.CLogger;

import javax.script.ScriptEngine;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;

/**
 * This is a facade class for the Service Locator.
 * It provides simple access to all core services.
 *
 * @author viola
 * @author hengsin
 * @author Silvano Trinchero, www.freepath.it
 *  		<li>IDEMPIERE-3243 added getScriptEngine to manage both registered engines and engines provided by osgi bundles 
 */
public class Core {

	private final static CLogger s_log = CLogger.getCLogger(Core.class);

		/**
	 *
	 * @param processId Java class name or equinox extension id
	 * @return ProcessCall instance or null if processId not found
	 */
	public static ProcessCall getProcess(String processId) {
		List<IProcessFactory> factories = Service.locator().list(IProcessFactory.class).getServices();
		if (factories != null && !factories.isEmpty()) {
			for(IProcessFactory factory : factories) {
				ProcessCall process = factory.newProcessInstance(processId);
				if (process != null)
					return process;
			}
		}
		return new DefaultProcessFactory().newProcessInstance(processId);
	}

}
