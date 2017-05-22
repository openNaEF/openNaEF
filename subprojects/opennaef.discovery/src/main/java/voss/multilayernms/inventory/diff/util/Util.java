package voss.multilayernms.inventory.diff.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.FileUtils;
import voss.multilayernms.inventory.config.MplsNmsDiffConfiguration;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.DiffSet;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Util {

    private static final Logger log = LoggerFactory.getLogger(Util.class);

    private static final Map<DiffCategory, String> lockMap = new HashMap<DiffCategory, String>();
    private static final DateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");

    private Util() {
    }

    private MyFilter createMyFilter(DiffCategory category) {
        return new MyFilter(category.name() + "-");
    }

    public static synchronized Serializable loadSerializableFile(File file) throws IOException, ClassNotFoundException {
        Serializable result = null;
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        result = (Serializable) ois.readObject();
        ois.close();
        return result;
    }

    public static boolean lock(DiffCategory category, String user) {
        log.debug("lock request category[" + category + "] user[" + user + "]");
        synchronized (lockMap) {
            if (lockMap.containsKey(category)) {
                log.debug("already locked.");
                return false;
            }
            log.debug("get lock.");
            lockMap.put(category, user);
            return true;
        }
    }

    public static boolean unlock(DiffCategory category, String user) {
        synchronized (lockMap) {
            if (user != null && user.equals(lockMap.get(category))) {
                String lastUser = lockMap.remove(category);
                log.debug("unlock category[" + category + "] user[" + lastUser + "]");
                return true;
            }
        }
        return false;
    }

    public static void unlockAndLockForce(DiffCategory category, String userName) {
        synchronized (lockMap) {
            String lastUser = lockMap.remove(category);
            log.debug("unlockFouce category[" + category + "] user[" + lastUser + "]");
            lockMap.put(category, userName);
            log.debug("lockFouce category[" + category + "] user[" + userName + "]");
        }
    }

    public static void unlockForce(DiffCategory category) {
        synchronized (lockMap) {
            String lastUser = lockMap.remove(category);
            log.debug("unlockFouce category[" + category + "] user[" + lastUser + "]");
        }
    }

    public static String getLockUserName(DiffCategory category) {
        synchronized (lockMap) {
            return lockMap.get(category);
        }
    }

    public static File getCurrentFile(DiffCategory category) {
        File dir = new File(ConfigUtil.getInstance().getDifferenceSetDir());
        File result = null;
        for (File f : dir.listFiles(new Util().createMyFilter(category))) {
            if (result == null || result.lastModified() < f.lastModified()) {
                result = f;
            }
        }
        return result;
    }

    private class MyFilter implements FilenameFilter {
        private final String prefix;

        public MyFilter(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public boolean accept(File dir, String name) {
            if (name != null && name.startsWith(prefix)) return true;
            return false;
        }
    }

    public static Exception getCauseException(Exception e) {
        while (e.getCause() != null) {
            e = (Exception) e.getCause();
        }
        return e;
    }

    public static void saveDiffSet(DiffSet diffSet) throws IOException {
        String fileName = diffSet.getSourceSystem() + "-" + df.format(diffSet.getCreationDate()) + ".diffset";
        File diffSetFile = new File(MplsNmsDiffConfiguration.getInstance().getDiffsetStoreDirectory(), fileName);
        FileUtils.saveObject(diffSetFile, diffSet, true);
        log.info("stored diffset as " + diffSetFile.getAbsolutePath());
    }

    public static DiffSet loadDiffSet(DiffCategory category) {
        try {
            File file = Util.getCurrentFile(category);
            if (file == null) return null;
            log.info("load diffset from " + file.getAbsolutePath());
            DiffSet set = FileUtils.loadObject(file);
            return set;
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    public static void saveObjectForDebug(Object obj, String prefix) {
        if (!ConfigUtil.getInstance().isDebugFlagOn()) return;
        String fileName = prefix + "-" + obj.getClass().getName() + "-" + df.format(new Date()) + ".dmp";
        File file = new File(ConfigUtil.getInstance().getDebugDumpDir(), fileName);
        try {
            FileUtils.saveObject(file, obj, true);
            log.info("stored dump as " + file.getAbsolutePath());
        } catch (IOException e) {
            log.error("", e);
        }
    }

}