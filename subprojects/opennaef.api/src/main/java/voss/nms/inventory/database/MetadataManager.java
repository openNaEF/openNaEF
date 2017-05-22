package voss.nms.inventory.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.config.InventoryConfiguration;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MetadataManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MetadataManager.class);
    public static final String NODE_METADATA_DIR = "node";
    public static final String CHASSIS_METADATA_DIR = "chassis";
    public static final String MODULE_METADATA_DIR = "module";
    private static MetadataManager instance = null;

    public static MetadataManager getInstance() throws IOException, InventoryException {
        if (instance == null) {
            instance = new MetadataManager();
        }
        return instance;
    }

    private File metadataRootDir = null;

    private MetadataManager() throws IOException, InventoryException {
        InventoryConfiguration config = InventoryConfiguration.getInstance();
        String metadataDirName = config.getMetadataDir();
        File root = new File(metadataDirName);
        if (!root.exists()) {
            throw new IOException("no metadata root dir." + root.getAbsolutePath());
        }
        log.info("metadata dir is " + root.getAbsolutePath());
        this.metadataRootDir = root;
    }

    public List<String> getVendorList() {
        List<String> result = new ArrayList<String>();
        for (String subDirName : this.metadataRootDir.list()) {
            File subDir = new File(this.metadataRootDir, subDirName);
            if (!subDir.isDirectory()) {
                continue;
            }
            if (subDirName.startsWith("#")) {
                continue;
            }
            result.add(subDirName);
        }
        return result;
    }

    private File getVendorDir(String vendor) throws IOException {
        if (vendor == null) {
            return null;
        }
        File file = new File(this.metadataRootDir, vendor);
        if (!file.exists()) {
            log.warn("node metadata directory is not found: " + vendor);
            return null;
        }
        return file;
    }

    private File getNodeDir(String vendor) throws IOException {
        if (vendor == null) {
            return null;
        }
        File vendorDir = getVendorDir(vendor);
        File file = new File(vendorDir, NODE_METADATA_DIR);
        if (!file.exists()) {
            log.warn("node metadata directory is not found: " + vendor);
            return null;
        }
        return file;
    }

    private File getChassisDir(String vendor) throws IOException {
        if (vendor == null) {
            return null;
        }
        File vendorDir = getVendorDir(vendor);
        File file = new File(vendorDir, CHASSIS_METADATA_DIR);
        if (!file.exists()) {
            log.warn("chassis metadata directory is not found: " + vendor);
            return null;
        }
        return file;
    }

    private File getModuleDir(String vendor) throws IOException {
        if (vendor == null) {
            return null;
        }
        File vendorDir = getVendorDir(vendor);
        File file = new File(vendorDir, MODULE_METADATA_DIR);
        if (!file.exists()) {
            log.warn("module metadata directory is not found: " + vendor);
            return null;
        }
        return file;
    }

    public List<String> getNodeTypeList(String vendor) throws IOException {
        if (vendor == null) {
            return new ArrayList<String>();
        }
        List<String> result = new ArrayList<String>();
        File nodeDir = getNodeDir(vendor);
        if (nodeDir == null) {
            return result;
        }
        for (String member : nodeDir.list()) {
            File file = new File(nodeDir, member);
            if (!file.isFile()) {
                continue;
            }
            member = member.replaceAll("\\.[Tt][Xx][Tt]", "");
            result.add(member);
        }
        return result;
    }

    public List<String> getChassisTypeList(String vendor) throws IOException {
        List<String> result = new ArrayList<String>();
        result.add(null);
        if (vendor == null) {
            return result;
        }
        File nodeDir = getChassisDir(vendor);
        if (nodeDir == null || !nodeDir.exists()) {
            return result;
        }
        for (String member : nodeDir.list()) {
            File file = new File(nodeDir, member);
            if (!file.isFile()) {
                continue;
            }
            member = member.replaceAll("\\.[Tt][Xx][Tt]", "");
            result.add(member);
        }
        return result;
    }

    public List<String> getModuleTypeList(String vendor) throws IOException {
        List<String> result = new ArrayList<String>();
        result.add(null);
        if (vendor == null) {
            return result;
        }
        File nodeDir = getModuleDir(vendor);
        if (nodeDir == null || !nodeDir.exists()) {
            return result;
        }
        for (String member : nodeDir.list()) {
            File file = new File(nodeDir, member);
            if (!file.isFile()) {
                continue;
            }
            member = member.replaceAll("\\.[Tt][Xx][Tt]", "");
            result.add(member);
        }
        return result;
    }

    public List<String> getNodeMetadata(String vendor, String nodeType)
            throws IOException {
        return loadMetadata(vendor, NODE_METADATA_DIR, nodeType);
    }

    public List<String> getChassisMetadata(String vendor, String chassisType)
            throws IOException {
        return loadMetadata(vendor, CHASSIS_METADATA_DIR, chassisType);
    }

    public List<String> getModuleMetadata(String vendor, String moduleType)
            throws IOException {
        return loadMetadata(vendor, MODULE_METADATA_DIR, moduleType);
    }

    private List<String> loadMetadata(String vendor, String sub, String target)
            throws IOException {
        File vendorDir = getVendorDir(vendor);
        File subDir = new File(vendorDir, sub);
        if (!subDir.exists()) {
            throw new IOException("dir not found: " + subDir.getAbsolutePath());
        } else if (!subDir.isDirectory()) {
            throw new IOException("dir is not directory: " + subDir.getAbsolutePath());
        }
        File file = new File(subDir, target + ".txt");
        if (!file.exists()) {
            throw new IOException("target file not found: " + file.getAbsolutePath());
        }
        BufferedReader br = null;
        List<String> contents = new ArrayList<String>();
        try {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, Charset.forName("MS932"));
            br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                contents.add(line);
            }
            return contents;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

}