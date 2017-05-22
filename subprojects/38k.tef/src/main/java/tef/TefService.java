package tef;

import lib38k.io.IoUtils;
import lib38k.logger.FileLogger;
import lib38k.logger.Logger;
import lib38k.net.httpd.HttpServer;
import tef.logic.BusinessLogic;
import tef.logic.BusinessLogicPool;
import tef.ui.http.TefHttpServer;
import tef.ui.shell.ShellPluginsConfig;
import tef.ui.shell.ShellServer;

import java.io.File;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.util.*;
import java.util.List;

/**
 * <p>TefService は TEF に関するサービスを提供します。
 */
public class TefService {

    public static final String TEF_NAME = "Universal Time-Engineering Framework";
    static final String MODULE_NAME = "tef";

    static enum RestoringMode {

        RESTORING_BY_JOURNAL,
        RESTORING_BY_BULK
    }

    public enum RunningMode {

        MASTER,
        MIRROR,
        FROZEN_SNAPSHOT
    }

    static class TefServiceLogs extends Logs {

        private final File journalsDirectory_;
        private final File stacktraceCatalogsDirectory_;
        private final File threadDumpDirectory_;

        TefServiceLogs(long time, File workingDirectory, String logsDirectoryName) {
            super(time, workingDirectory, logsDirectoryName);

            journalsDirectory_
                    = IoUtils.initializeDirectory(getLogsDirectory(), "transactions");
            stacktraceCatalogsDirectory_
                    = IoUtils.initializeDirectory(getLogsDirectory(), "stacktrace-catalogs");
            threadDumpDirectory_
                    = IoUtils.initializeDirectory(getLogsDirectory(), "thread-dump");
        }

        File getJournalsDirectory() {
            return journalsDirectory_;
        }

        File getStacktraceCatalogsDirectory() {
            return stacktraceCatalogsDirectory_;
        }

        File getThreadDumpDirectory() {
            return threadDumpDirectory_;
        }
    }

    private static TefService singleton__;
    private boolean isInitializing_ = false;
    private RunningMode runningMode_;
    private RestoringMode restoringMode_ = null;
    private boolean isTefServiceAvailable_ = false;

    private JournalDistributor journalDistributor_;
    private JournalReceiver journalReceiver_;

    private Thread transactionRestoringThread_ = null;

    private String serviceName_ = null;
    private final String buildVersion_;

    private final TefServiceConfig config_;

    private final File workingDirectory_;
    private final File configsDirectory_;

    private final TefServiceLogs logs_;
    private final TransactionExecLogger transactionExecLogger_;
    private final lib38k.logger.Logger genericLogger_;

    private final MvoRegistry mvoRegistry_;
    private final Indexes indexes_;

    private final long tefServiceInstanceId_;

    private final MvoMeta mvoMeta_;

    private final ReadTransactionRecycler readTransactionRecycler_;

    private final PostCommitProcessService postCommitProcessService_;

    private final TransactionDigestComputer transactionDigestComputer_;

    private final SystemProperties systemProperties_ = new SystemProperties(this);

    private volatile java.rmi.registry.Registry rmiRegistry_;

    private final Map<String, StackTraceCatalog> stacktraceCatalogs_
            = new HashMap<String, StackTraceCatalog>();

    final StackTraceCatalog beginWriteTransactionStacktraceCatalog;
    final StackTraceCatalog closeWriteTransactionStacktraceCatalog;
    final StackTraceCatalog beginReadTransactionStacktraceCatalog;
    final StackTraceCatalog closeReadTransactionStacktraceCatalog;
    final StackTraceCatalog beginDistributedTransactionStacktraceCatalog;

    private BusinessLogicPool businessLogicPool_ = null;

    private final ExtraObjectCoder.Instances extraObjectCoders_ = new ExtraObjectCoder.Instances();

    private final Map<String, String> moduleVersions_ = new LinkedHashMap<String, String>();

    private HttpServer httpd_;

    public TefService() {
        synchronized (TefService.class) {
            if (singleton__ != null) {
                throw new IllegalStateException();
            }

            singleton__ = this;

            String workingDirectoryStr = System.getProperty("tef-working-directory");
            workingDirectory_ = workingDirectoryStr == null
                    ? new File(".")
                    : new File(workingDirectoryStr);
            checkDirectoryExistence(workingDirectory_, "working directory");

            configsDirectory_ = new File(getWorkingDirectory(), "configs");
            checkDirectoryExistence(configsDirectory_, "configs");
        }

        tefServiceInstanceId_ = System.currentTimeMillis();
        logs_ = new TefServiceLogs(tefServiceInstanceId_, getWorkingDirectory(), "logs");
        buildVersion_ = readBuildVersion(MODULE_NAME);
        config_ = new TefServiceConfig();
        config_.loadTefServiceConfig(this);
        serviceName_ = config_.getServiceName();
        runningMode_ = config_.getRunningMode();
        transactionExecLogger_
                = new TransactionExecLogger(this, createLogger("transaction-execution"));
        genericLogger_ = createLogger("generic");

        mvoRegistry_ = new MvoRegistry(this);
        indexes_ = new Indexes();

        mvoMeta_ = new MvoMeta();

        readTransactionRecycler_ = new ReadTransactionRecycler(this);
        postCommitProcessService_ = new PostCommitProcessService(this);
        transactionDigestComputer_ = new TransactionDigestComputer();

        beginWriteTransactionStacktraceCatalog = newStacktraceCatalog("w+");
        closeWriteTransactionStacktraceCatalog = newStacktraceCatalog("w-");
        beginReadTransactionStacktraceCatalog = newStacktraceCatalog("r+");
        closeReadTransactionStacktraceCatalog = newStacktraceCatalog("r-");
        beginDistributedTransactionStacktraceCatalog = newStacktraceCatalog("d+");

        logMessage
                ("tef service, build:" + buildVersion_
                        + ", service-name:" + serviceName_
                        + ", running-mode:" + runningMode_.name().toLowerCase());
    }

    protected synchronized String readBuildVersion(String moduleName) {
        try {
            String versionFileName = "/version." + moduleName;

            String version
                    = new String
                    (IoUtils.getStreamData(getClass().getResourceAsStream(versionFileName)));

            moduleVersions_.put(moduleName, version);

            return version;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * <p>TEF サービスを開始します。
     */
    public final void start() {
        synchronized (this) {
            if (isTefServiceAvailable_) {
                throw new IllegalStateException("the tef service is already running.");
            }
            if (isInitializing_) {
                throw new IllegalStateException("the tef service is already initializing.");
            }

            isInitializing_ = true;
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                TefService.this.logMessage("shutting down...");
            }
        });

        serviceInitializing();

        businessLogicPool_ = null;

        try {
            JournalArchivingService journalArchiver = new JournalArchivingService(this);

            setTransactionRestoringThread();
            if (MvoBulkDump.isMvoBulkDumpExisting()) {
                restoringMode_ = RestoringMode.RESTORING_BY_BULK;
                MvoBulkDump.restore();
            } else {
                restoringMode_ = RestoringMode.RESTORING_BY_JOURNAL;
                JournalReader.restoreJournals(this);
            }
            restoringMode_ = null;
            resetTransactionRestoringThread();

            rmiRegistry_ = LocateRegistry.createRegistry(config_.getRmiRegistryPort());

            TefServiceConfig.JournalDistributorConfig journalDistributorConfig
                    = config_.getJournalDistributorConfig();
            if (journalDistributorConfig != null) {
                journalDistributor_ = new JournalDistributor
                        (this,
                                journalDistributorConfig.authorizedDistributeeAddresses);
            }

            TefServiceConfig.ShellConfig shellConfig = config_.shellConfig;
            if (shellConfig != null) {
                ShellServer shellService = createShellService();
                shellService.start();

                if (shellConfig.pluginFileName != null) {
                    ShellPluginsConfig.getInstance().updateConfig();
                }
            }

            HttpServer.Config httpdConfig = config_.httpServerConfig;
            if (httpdConfig != null) {
                httpd_ = createHttpService(httpdConfig, createLogger("http"));
                httpd_.start();

                if (httpdConfig.pluginFile != null) {
                    httpd_.getPluginsConfig().updateConfig();
                }
            }

            TefServiceConfig.BusinessLogicConfig businessLogicConfig
                    = config_.businessLogicConfig;
            if (businessLogicConfig != null) {
                businessLogicPool_ = createBusinessLogicPool();
            }

            if (getRunningMode() == TefService.RunningMode.MIRROR) {
                journalReceiver_ = new JournalReceiver(this);
            }

            SystemPropertyInterlock.configure(this);

            if (journalReceiver_ != null && journalReceiver_.isMirroringEnabled()) {
                journalReceiver_.awaitUntilSynchronizeWithMaster();
            }

            journalArchiver.start();
        } catch (TefInitializationFailedException tife) {
            synchronized (this) {
                isTefServiceAvailable_ = false;
            }
            throw tife;
        } catch (Exception e) {
            synchronized (this) {
                isTefServiceAvailable_ = false;
            }
            throw new TefInitializationFailedException(e);
        }

        synchronized (this) {
            isTefServiceAvailable_ = true;
            isInitializing_ = false;

            logMessage("tef-service enabled.");
        }

        if (businessLogicPool_ != null) {
            businessLogicPool_.init();
        }

        if (config_.getTransactionCoordinatorServiceUrl() != null) {
            try {
                TransactionManager.Facade.initializeLocalSide
                        (config_.getTransactionCoordinatorServiceUrl());
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        serviceStarted();
    }

    protected void serviceInitializing() {
    }

    protected void serviceStarted() {
    }

    public static synchronized TefService instance() {
        return singleton__;
    }

    public synchronized boolean isInitializing() {
        return isInitializing_;
    }

    synchronized final RestoringMode getRestoringMode() {
        return restoringMode_;
    }

    public synchronized final boolean isTefServiceAvailable() {
        return isTefServiceAvailable_;
    }

    synchronized final void disableTefService() {
        isTefServiceAvailable_ = false;
        logError("tef-service disabled.", new Throwable());
    }

    protected ShellServer createShellService() {
        return new ShellServer();
    }

    protected HttpServer createHttpService(HttpServer.Config config, Logger logger) {
        return new TefHttpServer(config, logger);
    }

    protected BusinessLogicPool createBusinessLogicPool() {
        return new BusinessLogicPool(this, new Class[0]);
    }

    /**
     * この TefService の作業ディレクトリを返します。
     */
    public File getWorkingDirectory() {
        return workingDirectory_;
    }

    TefServiceLogs getLogs() {
        return logs_;
    }

    /**
     * ログが保存されるディレクトリを返します。
     */
    public File getLogsDirectory() {
        return logs_.getLogsDirectory();
    }

    public FileLogger createLogger(String logDirName) {
        return logs_.createLogger(logDirName);
    }

    /**
     * 設定ファイルのディレクトリを返します。
     */
    public File getConfigsDirectory() {
        return configsDirectory_;
    }

    public RunningMode getRunningMode() {
        return runningMode_;
    }

    JournalDistributor getJournalDistributor() {
        return journalDistributor_;
    }

    JournalReceiver getJournalReceiver() {
        return journalReceiver_;
    }

    public String getBuildVersion() {
        return buildVersion_;
    }

    Thread getTransactionRestoringThread() {
        return transactionRestoringThread_;
    }

    void setTransactionRestoringThread() {
        if (transactionRestoringThread_ != null) {
            throw new IllegalStateException();
        }

        transactionRestoringThread_ = Thread.currentThread();
    }

    void resetTransactionRestoringThread() {
        if (transactionRestoringThread_ == null) {
            throw new IllegalStateException();
        }

        transactionRestoringThread_ = null;
    }

    public String getServiceName() {
        return serviceName_;
    }

    private static void checkDirectoryExistence(File directory, String description) {
        if (!directory.exists()) {
            throw new TefInitializationFailedException
                    ("directory not exists (" + description + "): "
                            + directory.getAbsolutePath());
        }
        if (!directory.isDirectory()) {
            throw new TefInitializationFailedException
                    ("not a directory(" + description + "): " + directory.getAbsolutePath());
        }
    }

    public Logger getLogger() {
        return genericLogger_;
    }

    public void logMessage(String message) {
        genericLogger_.log("[info]" + message);
    }

    public void logMessage(String message, List<String> extraMessages) {
        genericLogger_.log("[info]" + message, extraMessages);
    }

    public void logError(String message, Throwable t) {
        genericLogger_.logError("[error]" + message, t);
    }

    TransactionExecLogger getTransactionExecLogger() {
        return transactionExecLogger_;
    }

    public TefServiceConfig getTefServiceConfig() {
        return config_;
    }

    /**
     * MvoRegistry を返します。
     */
    public MvoRegistry getMvoRegistry() {
        return mvoRegistry_;
    }

    Indexes getIndexes() {
        return indexes_;
    }

    void checkTransactionRestoringThread() {
        if (Thread.currentThread() != getTransactionRestoringThread()) {
            throw new IllegalStateException();
        }
    }

    MvoMeta getMvoMeta() {
        return mvoMeta_;
    }

    public Integer getFieldId(MVO.MvoField mvofield) {
        return mvoMeta_.isNewField(mvofield)
                ? null
                : mvoMeta_.getFieldId(mvofield);
    }

    ReadTransactionRecycler getReadTransactionRecycler() {
        return readTransactionRecycler_;
    }

    PostCommitProcessService getPostCommitProcessService() {
        return postCommitProcessService_;
    }

    TransactionDigestComputer getTransactionDigestComputer() {
        return transactionDigestComputer_;
    }

    public synchronized SystemProperties getSystemProperties() {
        return systemProperties_;
    }

    public java.rmi.registry.Registry getRmiRegistry() {
        return rmiRegistry_;
    }

    synchronized StackTraceCatalog newStacktraceCatalog(String name) {
        if (stacktraceCatalogs_.get(name) != null) {
            throw new IllegalArgumentException(name);
        }

        StackTraceCatalog catalog = new StackTraceCatalog(name);
        stacktraceCatalogs_.put(name, catalog);
        return catalog;
    }

    synchronized StackTraceCatalog getStacktraceCatalog(String name) {
        return stacktraceCatalogs_.get(name);
    }

    public BusinessLogicPool getBusinessLogicPool() {
        return businessLogicPool_;
    }

    public <T extends BusinessLogic> T logic(Class<T> logicClass) {
        return getBusinessLogicPool().getLogic(logicClass);
    }

    public void addTransactionCommitListener
            (String listenerName, TransactionCommitListener listener) {
        getPostCommitProcessService().addTransactionCommitListener(listenerName, listener);
    }

    public void removeTransactionCommitListener(TransactionCommitListener listener) {
        getPostCommitProcessService().removeTransactionCommitListener(listener);
    }

    public void removeTransactionCommitListener(String listenerName) {
        getPostCommitProcessService().removeTransactionCommitListener(listenerName);
    }

    ExtraObjectCoder.Instances getExtraObjectCoders() {
        return extraObjectCoders_;
    }

    public void addExtraObjectCoder(ExtraObjectCoder<?> eoc) {
        extraObjectCoders_.addExtraObjectCoder(eoc);
    }

    public List<String> getModuleNames() {
        return new ArrayList<String>(moduleVersions_.keySet());
    }

    public String getModuleVersion(String moduleName) {
        return moduleVersions_.get(moduleName);
    }

    public GlobalTransactionId getGlobalTxId(TransactionId.W localTxId, boolean isStrict) {
        return TransactionManager.Facade.getGlobalTxId(localTxId, isStrict);
    }

    public TransactionId.W getLocalTxId(GlobalTransactionId globalTxId) {
        return TransactionManager.Facade.getLocalTxId(globalTxId);
    }

    public HttpServer httpd() {
        return httpd_;
    }
}
