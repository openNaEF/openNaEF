package voss.multilayernms.inventory.diff.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.config.MplsNmsDiffConfiguration;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.TaskException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DiffSetManager {
    public static final String FILENAME = "diffset-result.properties";
    private static final String RESULT_ENTRY = ":result";
    private static final String DATE_ENTRY = ":date";
    private static final String USER_NAME = "System";

    private static DiffSetManager instance = null;
    private static final Logger log = LoggerFactory.getLogger(DiffSetManager.class);

    public static synchronized DiffSetManager getInstance() {
        if (instance == null) instance = new DiffSetManager();
        return instance;
    }

    private final SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private Map<DiffCategory, Date> latestDate = new HashMap<DiffCategory, Date>();
    private Map<DiffCategory, String> latestResult = new HashMap<DiffCategory, String>();
    private NetworkDiffThread nwThread = null;

    private DiffSetManager() {
        loadLatestResult();
    }

    public synchronized void start(DiffCategory category) throws TaskException {
        switch (category) {
            case DISCOVERY:
                if (nwThread != null) {
                    if (nwThread.isRunning()) {
                        throw new TaskException("already running.", TaskException.Code.ALREADY_RUNNING);
                    } else {
                        nwThread = null;
                    }
                }
                nwThread = new NetworkDiffThread(category, USER_NAME);
                nwThread.start();
                return;
        }
        throw new IllegalStateException("no such category:" + category.name());
    }

    public synchronized void abort(DiffCategory category) {
        switch (category) {
            case DISCOVERY:
                if (nwThread != null && nwThread.isRunning()) {
                    log.debug("Aborted[" + category + "]");
                    updateLatestResult(category, "Aborted");
                    nwThread.interrupt();
                }
                return;
        }
        throw new IllegalStateException("no such category:" + category.name());
    }

    public void setSuccessResult(DiffCategory category, Date date) {
        updateLatestResult(category, "Success");
    }

    public void setErrorResult(DiffCategory category, String result) {
        log.debug("setErrorResult[" + category + "]");
        updateLatestResult(category, result);
    }

    public synchronized boolean isRunning(DiffCategory category) {
        switch (category) {
            case DISCOVERY:
                if (nwThread != null) {
                    return nwThread.isRunning();
                }
                return false;
        }
        return false;
    }

    private void updateLatestResult(DiffCategory category, String result) {
        latestResult.put(category, result);
        latestDate.put(category, new Date());
        saveLatestResult();
    }

    private synchronized void loadLatestResult() {
        try {
            MplsNmsDiffConfiguration config = MplsNmsDiffConfiguration.getInstance();
            File dir = config.getDiffsetStoreDirectory();
            File propFile = new File(dir, FILENAME);
            if (!propFile.exists()) {
                propFile.createNewFile();
            }
            Properties prop = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(propFile);
                prop.load(fis);
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
            for (DiffCategory category : DiffCategory.values()) {
                String resultEntryName = category.name() + RESULT_ENTRY;
                String dateEntryName = category.name() + DATE_ENTRY;
                String resultEntry = prop.getProperty(resultEntryName);
                String dateEntryValue = prop.getProperty(dateEntryName);
                Date dateEntry = null;
                try {
                    if (dateEntryValue != null) {
                        dateEntry = df.parse(dateEntryValue);
                    }
                } catch (Exception e) {
                    log.warn("cannot parse: " + dateEntryName + "->" + dateEntryValue);
                }
                this.latestResult.put(category, resultEntry);
                this.latestDate.put(category, dateEntry);
            }
        } catch (Exception e) {
            log.error("failed to save diff-service result.", e);
        }
    }

    private synchronized void saveLatestResult() {
        try {
            MplsNmsDiffConfiguration config = MplsNmsDiffConfiguration.getInstance();
            File dir = config.getDiffsetStoreDirectory();
            File propFile = new File(dir, FILENAME);
            Properties prop = new Properties();
            for (Map.Entry<DiffCategory, String> entry : this.latestResult.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                String key = entry.getKey().name() + RESULT_ENTRY;
                prop.setProperty(key, entry.getValue());
            }
            for (Map.Entry<DiffCategory, Date> entry : this.latestDate.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                String key = entry.getKey().name() + DATE_ENTRY;
                prop.setProperty(key, df.format(entry.getValue()));
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(propFile);
                prop.store(fos, "diffset-result at " + df.format(new Date()));
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
        } catch (Exception e) {
            log.error("failed to save diff-service result.", e);
        }
    }

    public String getLatestResult(DiffCategory category) {
        return latestResult.get(category);
    }

    public Date getLatestDate(DiffCategory category) {
        return latestDate.get(category);
    }

}