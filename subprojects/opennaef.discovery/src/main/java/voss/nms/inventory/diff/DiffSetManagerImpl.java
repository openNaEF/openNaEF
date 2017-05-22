package voss.nms.inventory.diff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.FileUtils;
import voss.nms.inventory.config.DiffConfiguration;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.diff.network.NetworkDiffThread;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class DiffSetManagerImpl extends UnicastRemoteObject implements DiffSetManager {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);

    private static DiffSetManagerImpl instance = null;

    public synchronized static DiffSetManagerImpl getInstance() throws IOException, InventoryException, ClassNotFoundException {
        if (instance == null) {
            instance = new DiffSetManagerImpl();
        }
        return instance;
    }

    private final Map<String, DiffCategory> categories = new HashMap<String, DiffCategory>();
    private final Map<DiffCategory, DiffSet> diffSets = new HashMap<DiffCategory, DiffSet>();
    private NetworkDiffThread networkDiffThread = null;
    private DiffConfiguration config = null;
    private DateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");

    public DiffSetManagerImpl() throws IOException, InventoryException, ClassNotFoundException {
        super();
        this.categories.put(DiffCategory.DISCOVERY.name(), DiffCategory.DISCOVERY);
        loadConfig();
        restore();
    }

    public synchronized void loadConfig() throws IOException, InventoryException {
        if (this.config == null) {
            this.config = DiffConfiguration.getInstance();
        }
        this.config.reloadConfiguration();
        log.info("config updated.");
    }

    private void restore() throws IOException, ClassNotFoundException {
        for (DiffCategory category : this.categories.values()) {
            String fileName = findNewestFile(category);
            if (fileName == null) {
                continue;
            }
            File file = new File(this.config.getDiffsetStoreDirectory(), fileName);
            log.info("restoring: " + file.getAbsolutePath());
            DiffSet set = loadDiffSet(file);
            this.diffSets.put(category, set);
            log.info("restored: " + file.getAbsolutePath());
        }
    }

    private String findNewestFile(final DiffCategory key) throws IOException {
        File dir = config.getDiffsetStoreDirectory();
        String[] names = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name != null && name.startsWith(key.name());
            }
        });
        if (names == null || names.length == 0) {
            return null;
        }
        Arrays.sort(names);
        return names[names.length - 1];
    }

    private DiffSet loadDiffSet(File file) throws IOException, ClassNotFoundException {
        if (file == null) {
            throw new IllegalArgumentException("file not found.");
        }
        DiffSet set = FileUtils.loadObject(file);
        return set;
    }

    public synchronized List<String> list() {
        List<String> result = new ArrayList<String>();
        for (String displayName : this.categories.keySet()) {
            result.add(displayName);
        }
        return result;
    }

    public synchronized DiffCategory getCategoryByDisplayName(String caption) {
        return this.categories.get(caption);
    }

    public synchronized void createNewDiff(DiffCategory cat) throws RemoteException {
        try {
            createNewDiffInner(cat);
        } catch (TaskException e) {
            throw new RemoteException(e.getCauseName(), e);
        }
    }

    public synchronized void createNewDiffInner(DiffCategory cat) throws TaskException {
        switch (cat) {
            case DISCOVERY:
                if (this.networkDiffThread != null) {
                    if (this.networkDiffThread.isRunning()) {
                        throw new TaskException("already running.", TaskException.Code.ALREADY_RUNNING);
                    } else {
                        this.networkDiffThread = null;
                    }
                }
                this.networkDiffThread = new NetworkDiffThread(this, cat);
                this.networkDiffThread.start();
                return;
        }
        throw new IllegalStateException("no such category:" + cat.name());
    }

    public synchronized void updateDiffSet(DiffCategory category, DiffSet diffset) throws IOException {
        log.info("updating diffset: " + diffset.getSourceSystem());
        this.diffSets.put(category, diffset);

        File storeDir = config.getDiffsetStoreDirectory();
        switch (category) {
            case DISCOVERY:
                saveDiffSet(storeDir, diffset);
                break;
        }
        abort(category);
        switch (category) {
            case DISCOVERY:
                this.networkDiffThread = null;
                break;
        }
    }

    private void saveDiffSet(File parentDir, DiffSet diffSet) throws IOException {
        String fileName = diffSet.getSourceSystem() + "-" + df.format(diffSet.getCreationDate()) + ".diffset";
        File diffSetFile = new File(parentDir, fileName);
        FileUtils.saveObject(diffSetFile, diffSet, true);
        log.info("stored diffset as " + diffSetFile.getAbsolutePath());
    }

    public synchronized void abort(DiffCategory cat) {
        switch (cat) {
            case DISCOVERY:
                if (this.networkDiffThread != null && this.networkDiffThread.isRunning()) {
                    this.networkDiffThread.interrupt();
                }
                break;
        }
    }

    public synchronized boolean isRunning(DiffCategory category) {
        if (category == null) {
            log.debug("*category==null");
            return false;
        }
        switch (category) {
            case DISCOVERY:
                if (this.networkDiffThread != null) {
                    log.debug("*network is " + (this.networkDiffThread.isRunning() ? "running" : "not nunning."));
                    return this.networkDiffThread.isRunning();
                }
                log.debug("*network is not nunning.");
                return false;
        }
        throw new IllegalArgumentException("unknown categoryName:" + category.name());
    }

    public synchronized String getStatus(DiffCategory category) {
        if (category == null) {
            return null;
        }
        switch (category) {
            case DISCOVERY:
                if (this.networkDiffThread != null) {
                    return this.networkDiffThread.isRunning() ? "running" : "idle.";
                } else {
                    return "idle.";
                }
        }
        return "unknown:" + category.name();
    }

    public synchronized DiffSet getDiffSet(DiffCategory category) {
        return this.diffSets.get(category);
    }

    public synchronized void apply(DiffUnit unit) {

    }

    public synchronized void discard(DiffUnit unit) {

    }
}