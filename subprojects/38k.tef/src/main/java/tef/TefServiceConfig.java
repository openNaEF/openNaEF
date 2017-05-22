package tef;

import lib38k.net.httpd.HttpServer;
import lib38k.xml.Xml;
import tef.ui.http.TefHttpServer;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.List;

public class TefServiceConfig {

    static class MasterServerConfig {

        final String distributorUrl;

        MasterServerConfig(String distributorUrl) {
            this.distributorUrl = distributorUrl;
        }
    }

    public static class JournalDistributorConfig {

        public final Set<InetAddress> authorizedDistributeeAddresses;

        JournalDistributorConfig(Set<InetAddress> authorizedDistributeeAddresses) {
            this.authorizedDistributeeAddresses
                    = Collections.unmodifiableSet(authorizedDistributeeAddresses);
        }
    }

    public static class ShellConfig {

        public final int port;
        public final String pluginFileName;

        ShellConfig(int port, String pluginFileName) {
            this.port = port;
            this.pluginFileName = pluginFileName;
        }
    }

    public static class BusinessLogicConfig {

        public final String logicJarFileName;

        BusinessLogicConfig(String logicJarFileName) {
            this.logicJarFileName = logicJarFileName;
        }
    }

    static class StackTraceCatalogConfig {

        final List<String> omissionLineRegExps;

        StackTraceCatalogConfig(List<String> omissionLineRegExps) {
            this.omissionLineRegExps = Collections.unmodifiableList(omissionLineRegExps);
        }
    }

    private static final String ROOT_ELEMENT_NAME = "tef-service-config";

    private Xml.Elem root_;

    private String serviceName_ = null;
    private TefService.RunningMode runningMode_;
    private int rmiRegistryPort_;
    private String transactionCoordinatorServiceUrl_ = null;
    private MasterServerConfig masterServerConfig_ = null;
    private JournalDistributorConfig journalDistributorConfig_ = null;
    public ShellConfig shellConfig = null;
    public HttpServer.Config httpServerConfig = null;
    public BusinessLogicConfig businessLogicConfig = null;
    public StackTraceCatalogConfig stackTraceCatalogConfig = null;

    TefServiceConfig() {
    }

    void loadTefServiceConfig(TefService tefService) {
        File configFile
                = new File(TefService.instance().getConfigsDirectory(), "TefServiceConfig.xml");
        if (!configFile.exists()) {
            throw new ConfigurationException("file not found: " + configFile.getAbsolutePath());
        }
        root_ = new Xml(configFile).getRoot();
        if (!root_.getName().equals(ROOT_ELEMENT_NAME)) {
            throw new ConfigurationException("root element must be " + ROOT_ELEMENT_NAME + ".");
        }

        serviceName_ = root_.getNonNullAttr("service-name");

        String runningModeStr = root_.getNonNullAttr("running-mode");
        try {
            runningMode_ = TefUtils.resolveEnum(TefService.RunningMode.class, runningModeStr);
        } catch (IllegalArgumentException iae) {
            throw new ConfigurationException("unknown running-mode: " + runningModeStr);
        }

        rmiRegistryPort_ = root_.getIntAttr("rmi-registry-port");

        Xml.Elem transactionCoordinatorServiceElement
                = root_.getSubElem("transaction-coordinator-service");
        if (transactionCoordinatorServiceElement != null) {
            transactionCoordinatorServiceUrl_
                    = transactionCoordinatorServiceElement.getAttr("url");
        }

        Xml.Elem masterServerElement = root_.getSubElem("master-server");
        if (masterServerElement != null) {
            if (getRunningMode() == TefService.RunningMode.MASTER) {
                throw new ConfigurationException
                        ("configuration failure: running-mode 'master' "
                                + "with master-server element.");
            }

            String distributorUrl
                    = getNonNullAttribute
                    (masterServerElement, "distributor-url", "running-mode is mirror");

            masterServerConfig_ = new MasterServerConfig(distributorUrl);
        } else {
            if (getRunningMode() == TefService.RunningMode.MIRROR) {
                throw new ConfigurationException
                        ("configuration failure: runnin-mode 'mirror' "
                                + "without master-server element.");
            }
        }

        Xml.Elem journalDistributorConfigElement = root_.getSubElem("journal-distributor");
        if (journalDistributorConfigElement != null) {
            Set<InetAddress> clientAddresses = new HashSet<InetAddress>();
            for (Xml.Elem distributeeAddressesElement
                    : journalDistributorConfigElement.getSubElems("distributee")) {
                String addressStr = distributeeAddressesElement.getAttr("address");
                try {
                    clientAddresses.add(InetAddress.getByName(addressStr));
                } catch (UnknownHostException uhe) {
                    logMessage(tefService, "journal distributee address ignored: " + addressStr);
                }
            }

            journalDistributorConfig_ = new JournalDistributorConfig(clientAddresses);
        }

        Xml.Elem shellConfigElement = root_.getSubElem("shell");
        if (shellConfigElement != null) {
            shellConfig
                    = new ShellConfig
                    (shellConfigElement.getNonNullIntAttr("port"),
                            shellConfigElement.getAttr("plugin_file_name"));
        }

        Xml.Elem httpdConfigElement = root_.getSubElem("http");
        if (httpdConfigElement != null) {
            int port = httpdConfigElement.getNonNullIntAttr("port");
            String defaultAuthenticationRealm
                    = httpdConfigElement.getAttr("default_authentication_realm");
            File pluginConfigFile
                    = new File
                    (TefService.instance().getConfigsDirectory(),
                            TefHttpServer.PLUGIN_CONFIG_FILE_NAME);
            String pluginFilePath = httpdConfigElement.getAttr("plugin_file_name");
            File pluginFile
                    = TefFileUtils.isAbsolutePath(pluginFilePath)
                    ? new File(pluginFilePath)
                    : new File(TefService.instance().getWorkingDirectory(), pluginFilePath);

            httpServerConfig
                    = new HttpServer.Config
                    (port,
                            defaultAuthenticationRealm,
                            pluginConfigFile,
                            pluginFile,
                            TefHttpServer.EXTRA_RESPONSE_STATUS_HEADER_FIELD_NAME);
        }

        Xml.Elem logicConfigElement = root_.getSubElem("logic");
        if (logicConfigElement != null) {
            businessLogicConfig
                    = new BusinessLogicConfig(logicConfigElement.getAttr("logic_file_name"));
        }

        Xml.Elem stackTraceCatalogConfigElement = root_.getSubElem("stacktrace-catalog");
        if (stackTraceCatalogConfigElement != null) {
            List<String> omissionLineRegExps = new ArrayList<String>();
            for (Xml.Elem omissionElement
                    : stackTraceCatalogConfigElement.getSubElems("omission-line")) {
                omissionLineRegExps.add(omissionElement.getNonNullAttr("regexp"));
            }

            stackTraceCatalogConfig = new StackTraceCatalogConfig(omissionLineRegExps);
        }
    }

    private String getNonNullAttribute
            (Xml.Elem e, String attributeName, String extraConditionalMessage) {
        String attribute = e.getAttr(attributeName);
        if (attribute == null || attribute.equals("")) {
            throw new ConfigurationException
                    (e.getName() + "." + attributeName + " is mandatory"
                            + (extraConditionalMessage == null
                            ? ""
                            : " when " + extraConditionalMessage)
                            + ".");
        }
        return attribute;
    }

    private void logMessage(TefService tefService, String message) {
        tefService.logMessage("[tef-service-config loading] " + message);
    }

    String getServiceName() {
        return serviceName_;
    }

    TefService.RunningMode getRunningMode() {
        return runningMode_;
    }

    int getRmiRegistryPort() {
        return rmiRegistryPort_;
    }

    String getTransactionCoordinatorServiceUrl() {
        return transactionCoordinatorServiceUrl_;
    }

    MasterServerConfig getMasterServerConfig() {
        return masterServerConfig_;
    }

    JournalDistributorConfig getJournalDistributorConfig() {
        return journalDistributorConfig_;
    }

    public Xml.Elem getRoot() {
        return root_;
    }
}
