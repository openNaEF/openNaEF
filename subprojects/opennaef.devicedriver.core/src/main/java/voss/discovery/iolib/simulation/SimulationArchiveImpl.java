package voss.discovery.iolib.simulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.model.NodeInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class SimulationArchiveImpl implements SimulationArchive {
    private final static Logger log = LoggerFactory.getLogger(SimulationArchiveImpl.class);
    private boolean recordable = false;
    private ZipOutputStream zos = null;
    private final File archiveFile;
    private List<String> targetList = new ArrayList<String>();
    private List<String> entryNames = new ArrayList<String>();

    public SimulationArchiveImpl(File file) throws IOException {
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
            if (entryName.matches("[A-Za-z0-9._]+/")) {
                String targetId = entryName.replace("/", "");
                this.targetList.add(targetId);
                log.debug(targetId);
            }
        }
    }

    public SimulationArchiveImpl(String recordingSessionName) throws IOException {
        if (recordingSessionName == null) {
            throw new IllegalArgumentException("recordingSessionName is null.");
        }
        this.recordable = true;
        String filename = recordingSessionName + ".zip";
        File file = new File(filename);
        try {
            if (file.exists()) {
                throw new IllegalArgumentException("file " + filename + " exists.");
            }
        } catch (SecurityException se) {
            throw new IOException(se);
        }
        log.info("simulation archive[" + file.getAbsolutePath() + "] created.");
        this.archiveFile = file;
    }

    public SimulationArchiveImpl(File dir, String recordingSessionName) throws IOException {
        if (dir == null) {
            throw new IllegalArgumentException("dir is null.");
        } else if (recordingSessionName == null) {
            throw new IllegalArgumentException("recordingSessionName is null.");
        }
        if (!dir.exists()) {
            boolean r = dir.mkdirs();
            if (!r) {
                throw new IOException("failed to mkdir: " + dir.getAbsolutePath());
            }
        }
        this.recordable = true;
        String filename = recordingSessionName + ".zip";
        File file = new File(dir, filename);
        try {
            if (file.exists()) {
                throw new IllegalArgumentException("file " + filename + " exists.");
            }
        } catch (SecurityException se) {
            throw new IOException(se);
        }
        log.info("simulation archive[" + file.getAbsolutePath() + "] created.");
        this.archiveFile = file;
    }

    public ZipOutputStream open() throws IOException {
        if (this.recordable != true) {
            throw new IOException("cannot open - already closed.");
        }
        zos = new ZipOutputStream(new FileOutputStream(this.archiveFile));
        ZipEntry startingEntry = new ZipEntry("META-INFO.txt");
        zos.putNextEntry(startingEntry);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String comment = "Created at:" + sdf.format(new Date()) + " (" + System.currentTimeMillis() + ")";
        zos.write(comment.getBytes());
        zos.closeEntry();
        zos.flush();
        log.debug("simulation archive[" + this.archiveFile.getAbsolutePath() + "] opened.");
        return zos;
    }

    public boolean isOpened() {
        return this.recordable;
    }

    public void close() throws IOException {
        try {
            this.zos.flush();
            if (this.recordable == true) {
                this.zos.close();
            }
        } finally {
            this.recordable = false;
        }
        log.debug("simulation archive[" + this.archiveFile.getAbsolutePath() + "] closed.");
    }

    public SimulationEntry getSimulationEntry(NodeInfo nodeinfo) throws IOException {
        assert nodeinfo != null;
        String target = getEffectiveTarget(nodeinfo.listIpAddress());
        if (target == null) {
            return null;
        }
        return new SimulationEntryImpl(target, this);
    }

    public SimulationEntry addSimulationEntry(ZipOutputStream zos, String target) throws IOException {
        if (target == null || entryNames.contains(target)) {
            throw new IllegalArgumentException();
        }
        SimulationEntry entry = new SimulationEntryImpl(target, zos);
        entry.createTargetSimulatorEntry();
        targetList.add(target);
        return entry;
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