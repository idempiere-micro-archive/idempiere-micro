package org.adempiere.base;

import org.adempiere.model.IAddressValidation;
import org.adempiere.osgi.OSGiScriptEngineManager;
import org.compiere.impexp.BankStatementLoaderInterface;
import org.compiere.impexp.BankStatementMatcherInterface;
import org.compiere.impl.*;
import org.compiere.order.IShipmentProcessor;
import org.compiere.order.IShipmentProcessorFactory;
import org.compiere.order.MShipperFacade;
import org.compiere.process.IProcessFactory;
import org.compiere.process.ProcessCall;
import org.compiere.util.PaymentExport;
import org.compiere.util.ReplenishInterface;
import org.compiere.validation.ModelValidator;
import org.idempiere.common.base.Service;
import org.idempiere.common.util.CLogger;
import org.osgi.framework.FrameworkUtil;

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
public class Core extends org.compiere.process.Core {

    private final static CLogger s_log = CLogger.getCLogger(Core.class);

    /**
     * @return list of active resource finder
     */
    public static IResourceFinder getResourceFinder() {
        return new IResourceFinder() {

            public URL getResource(String name) {
                List<IResourceFinder> f = Service.locator().list(IResourceFinder.class).getServices();
                for (IResourceFinder finder : f) {
                    URL url = finder.getResource(name);
                    if (url!=null)
                        return url;
                }
                return null;
            }
        };
    }

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
        return null;
    }

    /**
     *  Get payment processor instance
     * 	@param mbap payment processor model
     * 	@param mp payment model
     *  @return initialized PaymentProcessor or null
     */
    public static PaymentProcessor getPaymentProcessor(MBankAccountProcessor mbap, PaymentInterface mp) {
        if (s_log.isLoggable(Level.FINE)) s_log.fine("create for " + mbap);

        MPaymentProcessor mpp = new MPaymentProcessor(mbap.getCtx(), mbap.getC_PaymentProcessor_ID(), mbap.get_TrxName());
        String className = mpp.getPayProcessorClass();
        if (className == null || className.length() == 0) {
            s_log.log(Level.SEVERE, "No PaymentProcessor class name in " + mbap);
            return null;
        }
        //
        PaymentProcessor myProcessor = null;

        List<IPaymentProcessorFactory> factoryList = Service.locator().list(IPaymentProcessorFactory.class).getServices();
        if (factoryList != null) {
            for(IPaymentProcessorFactory factory : factoryList) {
                PaymentProcessor processor = factory.newPaymentProcessorInstance(className);
                if (processor != null) {
                    myProcessor = processor;
                    break;
                }
            }
        }

        if (myProcessor == null) {
            s_log.log(Level.SEVERE, "Not found in service/extension registry and classpath");
            return null;
        }

        //  Initialize
        myProcessor.initialize(mbap, mp);
        //
        return myProcessor;
    }

    /**
     * get BankStatementLoader instance
     *
     * @param className
     * @return instance of the BankStatementLoaderInterface or null
     */
    public static BankStatementLoaderInterface getBankStatementLoader(String className){
        if (className == null || className.length() == 0) {
            s_log.log(Level.SEVERE, "No BankStatementLoaderInterface class name");
            return null;
        }

        BankStatementLoaderInterface myBankStatementLoader = null;

        List<IBankStatementLoaderFactory> factoryList =
                Service.locator().list(IBankStatementLoaderFactory.class).getServices();
        if (factoryList != null) {
            for(IBankStatementLoaderFactory factory : factoryList) {
                BankStatementLoaderInterface loader = factory.newBankStatementLoaderInstance(className);
                if (loader != null) {
                    myBankStatementLoader = loader;
                    break;
                }
            }
        }

        if (myBankStatementLoader == null) {
            s_log.log(Level.CONFIG, className + " not found in service/extension registry and classpath");
            return null;
        }

        return myBankStatementLoader;
    }

    /**
     * get BankStatementMatcher instance
     *
     * @param className
     * @return instance of the BankStatementMatcherInterface or null
     */
    public static BankStatementMatcherInterface getBankStatementMatcher(String className){
        if (className == null || className.length() == 0) {
            s_log.log(Level.SEVERE, "No BankStatementMatcherInterface class name");
            return null;
        }

        BankStatementMatcherInterface myBankStatementMatcher = null;

        List<IBankStatementMatcherFactory> factoryList =
                Service.locator().list(IBankStatementMatcherFactory.class).getServices();
        if (factoryList != null) {
            for(IBankStatementMatcherFactory factory : factoryList) {
                BankStatementMatcherInterface matcher = factory.newBankStatementMatcherInstance(className);
                if (matcher != null) {
                    myBankStatementMatcher = matcher;
                    break;
                }
            }
        }

        if (myBankStatementMatcher == null) {
            s_log.log(Level.CONFIG, className + " not found in service/extension registry and classpath");
            return null;
        }

        return myBankStatementMatcher;
    }

    /**
     *
     * @param sf
     * @return shipment process instance or null if not found
     */
    public static IShipmentProcessor getShipmentProcessor(MShipperFacade sf)
    {
        if (s_log.isLoggable(Level.FINE)) s_log.fine("create for " + sf);

        String className = sf.getShippingProcessorClass();
        if (className == null || className.length() == 0)
        {
            s_log.log(Level.SEVERE, "Shipment processor class not define for shipper " + sf);
            return null;
        }

        List<IShipmentProcessorFactory> factoryList = Service.locator().list(IShipmentProcessorFactory.class).getServices();
        if (factoryList == null)
            return null;
        for (IShipmentProcessorFactory factory : factoryList)
        {
            IShipmentProcessor processor = factory.newShipmentProcessorInstance(className);
            if (processor != null)
                return processor;
        }

        return null;
    }

    /**
     * Get address validation instance
     * @param validation
     * @return address validation instance or null if not found
     */
    public static IAddressValidation getAddressValidation(MAddressValidation validation)
    {
        String className = validation.getAddressValidationClass();
        if (className == null || className.length() == 0)
        {
            s_log.log(Level.SEVERE, "Address validation class not defined: " + validation);
            return null;
        }

        List<IAddressValidationFactory> factoryList = Service.locator().list(IAddressValidationFactory.class).getServices();
        if (factoryList == null)
            return null;
        for (IAddressValidationFactory factory : factoryList)
        {
            IAddressValidation processor = factory.newAddressValidationInstance(className);
            if (processor != null)
                return processor;
        }

        return null;
    }

    /**
     * get Custom Replenish instance
     *
     * @param className
     * @return instance of the ReplenishInterface or null
     */
    public static ReplenishInterface getReplenish(String className){
        if (className == null || className.length() == 0) {
            s_log.log(Level.SEVERE, "No ReplenishInterface class name");
            return null;
        }

        ReplenishInterface myReplenishInstance = null;

        List<IReplenishFactory> factoryList =
                Service.locator().list(IReplenishFactory.class).getServices();
        if (factoryList != null) {
            for(IReplenishFactory factory : factoryList) {
                ReplenishInterface loader = factory.newReplenishInstance(className);
                if (loader != null) {
                    myReplenishInstance = loader;
                    break;
                }
            }
        }

        if (myReplenishInstance == null) {
            s_log.log(Level.CONFIG, className + " not found in service/extension registry and classpath");
            return null;
        }

        return myReplenishInstance;
    }


    /** Get script engine, checking classpath first, and then osgi plugins
     *
     * @param engineName
     * @return ScriptEngine found, or null
     */
    public static ScriptEngine getScriptEngine(String engineName)
    {
        OSGiScriptEngineManager osgiFactory = new OSGiScriptEngineManager( FrameworkUtil.getBundle(Core.class).getBundleContext());
        ScriptEngine engine = osgiFactory.getEngineByName(engineName);

        return engine;
    }

    /**
     * get PaymentExporter instance
     *
     * @param className
     * @return instance of the PaymentExporterInterface or null
     */
    public static PaymentExport getPaymentExporter (String className){
        if (className == null || className.length() == 0) {
            s_log.log(Level.SEVERE, "No PaymentExporter class name");
            return null;
        }

        PaymentExport myPaymentExporter = null;

        List<IPaymentExporterFactory> factoryList =
                Service.locator().list(IPaymentExporterFactory.class).getServices();
        if (factoryList != null) {
            for(IPaymentExporterFactory factory : factoryList) {
                PaymentExport exporter = factory.newPaymentExporterInstance(className);
                if (exporter != null) {
                    myPaymentExporter = exporter;
                    break;
                }
            }
        }

        if (myPaymentExporter == null) {
            s_log.log(Level.CONFIG, className + " not found in service/extension registry and classpath");
            return null;
        }

        return myPaymentExporter;
    }


}
