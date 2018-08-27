package org.idempiere.app

import org.compiere.orm.MClient
import org.compiere.orm.MSystem
import org.compiere.validation.ModelValidationEngine
import org.idempiere.common.db.CConnection
import org.idempiere.common.util.CLogMgt
import org.idempiere.common.util.CLogger
import org.idempiere.common.util.DB
import org.idempiere.common.util.Ini
import org.idempiere.common.util.Env
import org.idempiere.common.util.SecureInterface
import org.idempiere.common.util.SecureEngine
import java.util.concurrent.ScheduledThreadPoolExecutor

open class Micro {
    fun getThreadPoolExecutor(): ScheduledThreadPoolExecutor {
        return threadPoolExecutor!!
    }

    protected var log: CLogger? = null
    private var threadPoolExecutor: ScheduledThreadPoolExecutor? = null

    private fun createThreadPool() {
        val defaultMax = Runtime.getRuntime().availableProcessors() * 20
        val properties = Ini.getIni().getProperties()
        val s = properties.getProperty("MaxThreadPoolSize")
        val maxSize =
            if (s != null) {
                try {
                    Integer.parseInt(s)
                } catch (e: Exception) {
                    0
                }
            } else { 0 }

        val max =
            if (maxSize <= 0) {
                defaultMax
            } else { maxSize }

        // start thread pool
        threadPoolExecutor = ScheduledThreadPoolExecutor(max)
    }

    fun startup() {
        if (log != null) return
        val ini = Ini.getIni()
        ini.isClient = false
        CLogMgt.initialize(false)
        log = CLogger.getCLogger(Micro::class.java)

        val properties = ini.properties
        for (key in properties.keys) {
            if (key is String) {
                var s = key
                if (s.endsWith("." + ini.P_TRACELEVEL)) {
                    val level = properties.getProperty(s)
                    s = s.substring(0, s.length - ("." + ini.P_TRACELEVEL).length)
                    CLogMgt.setLevel(s, level)
                }
            }
        }
        DB.setDBTarget(CConnection.get(null))
        createThreadPool()

        if (!DB.isConnected()) {
            this.log!!.severe("No Database")
        }

        if (!DB.isBuildOK(Env.getCtx())) {
            log = null
            return
        }

        val system = MSystem.get(Env.getCtx()) ?: return

        // 	Initialize main cached Singletons
        ModelValidationEngine.get()
        try {
            var className: String? = system.getEncryptionKey()
            if (className == null || className.length == 0) {
                className = System.getProperty(SecureInterface.ADEMPIERE_SECURE)
                if (className != null && className.length > 0 &&
                        className != SecureInterface.ADEMPIERE_SECURE_DEFAULT) {
                    SecureEngine.init(className) // 	test it
                    system.setEncryptionKey(className)
                    system.saveEx()
                }
            }
            SecureEngine.init(className)

            MClient.getAll(Env.getCtx())
        } catch (e: Exception) {
            this.log!!.warning("Environment problems: " + e.toString())
        }

        // 	Start Workflow Document Manager (in other package) for PO
        var className: String? = null
        try {
            className = "org.compiere.wf.DocWorkflowManager"
            Class.forName(className)
        } catch (e: Exception) {
            this.log!!.warning("Not started: " + className + " - " + e.message)
        }

        DB.updateMail()
    }
}
