package voss.discovery.iolib.simulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.model.NodeInfo;

import java.io.File;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class OldSimulationArchive implements SimulationArchive {
    private final static Logger log = LoggerFactory.getLogger(OldSimulationArchive.class);
    private final File archiveFile;
    private List<String> targetList = new ArrayList<String>();
    private List<String> entryNames = new ArrayList<String>();

    public OldSimulationArchive(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file is null.");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("file not exist: [" + file.getAbsolutePath() + "]");
        }
        this.archiveFile = file;
        ZipFile zip = new ZipFile(file);

        Enumeration<?> entries = zip.entries();
        for (; entries.hasMoreElements(); ) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            log.debug("found entry: " + entry.getName());
            this.entryNames.add(entry.getName());
        }

        for (String entryName : entryNames) {
            if (entryName.matches("[0-9.]+\\.pdu")) {
                String targetId = entryName.replace(".pdu", "");
                this.targetList.add(targetId);
                log.debug("* " + targetId);
            }
        }
    }

    public OldSimulationArchive(String recordingSessionName) throws IOException {
        throw new RuntimeException("not supported.");
    }

    public ZipOutputStream open() throws IOException {
        throw new RuntimeException("not supported.");
    }

    public boolean isOpened() {
        return false;
    }

    public void close() throws IOException {
    }

    public SimulationEntry getSimulationEntry(NodeInfo nodeinfo) throws IOException {
        String target = getEffectiveTarget(nodeinfo.listIpAddress());
        if (target == null) {
            return null;
        }
        return new OldSimulationEntryImpl(target, this);
    }

    public SimulationEntry addSimulationEntry(ZipOutputStream zos, String target) throws IOException {
        throw new RuntimeException("not supported.");
    }

    private String getEffectiveTarget(List<InetAddress> inetAddresses) {
        for (InetAddress inetAddress : inetAddresses) {
            log.debug("getEffectiveTarget(): trying inetAddress:" + inetAddress.getHostAddress());
            String target = getTargetBy(inetAddress);
            if (exists(target)) {
                return target;
            }
        }
        return null;
    }

    public String getTargetBy(InetAddress inetAddress) {
        String target = inetAddress.getHostAddress();
        if (inetAddress instanceof Inet6Address) {
            target = target.replace(':', '-');
        }
        return target;
    }

    public boolean exists(String target) {
        return targetList.contains(target);
    }

    public File getSimulationArchiveFile() {
        return this.archiveFile;
    }

}