package voss.discovery.iolib.simulation;

import net.snmp.SnmpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.model.Device;
import voss.model.NodeInfo;
import voss.model.Protocol;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class SimulationEntryImpl implements SimulationEntry {
    private static final String INFO_FILE = "INFO";
    private static final String MIB_FILE = "mib.pdu";
    private static final String MIB_FOLDER = "mib/";
    private static final String CONSOLE_RESULT_FOLDER = "console/";
    private static final String CONSOLE_RESULT_SUFFIX = ".txt";
    private static final String NETCONF_RESULT_FOLDER = "netconf/";
    private static final String NETCONF_RESULT_SUFFIX = ".txt";
    private static final String CONFIG_FOLDER = "config/";
    private static final String CONFIG_FILE = "config.txt";
    private static final String DEVICE_FOLDER = "bin/";
    private static final String DEVICE_FILE = "device.bin";

    private final static Logger log = LoggerFactory.getLogger(SimulationEntryImpl.class);
    private final String targetId;
    private final String prefix;
    private final SimulationArchive archive;
    private final ZipFile archiveZip;

    private ZipOutputStream zos = null;

    public SimulationEntryImpl(String targetId, SimulationArchive archive) throws IOException {
        if (targetId == null) {
            throw new IllegalArgumentException();
        }
        if (archive == null) {
            throw new IllegalArgumentException();
        }
        this.targetId = targetId;
        this.prefix = targetId + "/";
        this.archive = archive;
        this.archiveZip = new ZipFile(archive.getSimulationArchiveFile());
        this.zos = null;
    }

    public SimulationEntryImpl(String targetId, ZipOutputStream zos) throws IOException {
        if (targetId == null) {
            throw new IllegalArgumentException();
        }
        if (zos == null) {
            throw new IllegalArgumentException();
        }
        this.targetId = targetId;
        this.prefix = targetId + "/";
        this.archive = null;
        this.archiveZip = null;
        this.zos = zos;
    }

    @Override
    public String getTargetId() {
        return this.targetId;
    }

    @Override
    public boolean exists() throws IOException, ZipException {
        ZipFile zip = new ZipFile(this.archive.getSimulationArchiveFile());
        ZipEntry entry = zip.getEntry(targetId + "/");
        if (entry != null) {
            return true;
        }
        return false;
    }

    @Override
    public void createTargetSimulatorEntry() throws IOException {
        String prefix = targetId + "/";
        ZipEntry startingEntry = new ZipEntry(prefix);
        zos.putNextEntry(startingEntry);
        zos.closeEntry();

        ZipEntry infoEntry = new ZipEntry(prefix + INFO_FILE);
        zos.putNextEntry(infoEntry);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String comment = "Created:" + sdf.format(new Date()) + " (" + System.currentTimeMillis() + ")";
        zos.write(comment.getBytes());
        zos.closeEntry();

        ZipEntry config = new ZipEntry(prefix + CONFIG_FOLDER);
        zos.putNextEntry(config);
        zos.closeEntry();

        ZipEntry console = new ZipEntry(prefix + CONSOLE_RESULT_FOLDER);
        zos.putNextEntry(console);
        zos.closeEntry();

        ZipEntry netconf = new ZipEntry(prefix + NETCONF_RESULT_FOLDER);
        zos.putNextEntry(netconf);
        zos.closeEntry();

        ZipEntry mib = new ZipEntry(prefix + MIB_FOLDER);
        zos.putNextEntry(mib);
        zos.closeEntry();
        zos.flush();
    }

    @Override
    public InputStream getMibZipInputStream() throws IOException {
        String name = getMibZipEntryName();
        ZipEntry entry = archiveZip.getEntry(name);
        if (entry != null) {
            return this.archiveZip.getInputStream(entry);
        }
        throw new IllegalStateException("no entry found in zip:" + name);
    }

    @Override
    public String getMibZipEntryName() {
        String name = prefix + MIB_FOLDER + MIB_FILE;
        log.debug("getMibZipEntry(): name=" + name);
        return name;
    }

    @Override
    public InputStream getConsoleCommandResponse(String command) throws IOException {
        String name = getConsoleResultEntryName(command);
        ZipEntry entry = archiveZip.getEntry(name);
        if (entry == null) {
            log.warn("no entry found in zip:" + name);
            ByteArrayInputStream bais = new ByteArrayInputStream("".getBytes());
            return bais;
        }
        return this.archiveZip.getInputStream(entry);
    }

    @Override
    public InputStream getNetConfCommandResponse(String command) throws IOException {
        String name = getNetConfResultEntryName(command);
        ZipEntry entry = archiveZip.getEntry(name);
        if (entry == null) {
            log.warn("no entry found in zip:" + name);
            ByteArrayInputStream bais = new ByteArrayInputStream("".getBytes());
            return bais;
        }
        return this.archiveZip.getInputStream(entry);
    }

    @Override
    public InputStream getConfig() throws IOException {
        String name = prefix + CONFIG_FOLDER + CONFIG_FILE;
        ZipEntry entry = archiveZip.getEntry(name);
        if (entry == null) {
            throw new IllegalStateException("no entry found in zip:" + name);
        }
        return this.archiveZip.getInputStream(entry);
    }

    @Override
    public Device getDevice() throws IOException {
        String name = prefix + DEVICE_FOLDER + DEVICE_FILE;
        ZipEntry entry = archiveZip.getEntry(name);
        if (entry == null) {
            return null;
        }
        InputStream is = this.archiveZip.getInputStream(entry);
        ObjectInputStream ois = new ObjectInputStream(is);
        try {
            Object o = ois.readObject();
            if (Device.class.isInstance(o)) {
                return Device.class.cast(o);
            } else {
                throw new IllegalStateException("not device: " + o.getClass().getName());
            }
        } catch (Exception e) {
            throw new IOException("failed to restore.", e);
        } finally {
            ois.close();
        }
    }

    private final static String COMMAND_REPLACE_RULE = "[ \t/:|]+";

    private String getCanonicalCommandName(String command) {
        String temp = command.replaceAll(COMMAND_REPLACE_RULE, "_");
        return temp;
    }

    private String getConsoleResultEntryName(String command) {
        String canonicalCommandName = getCanonicalCommandName(command);
        String name = prefix + CONSOLE_RESULT_FOLDER + canonicalCommandName + CONSOLE_RESULT_SUFFIX;
        return name;
    }

    private String getNetConfResultEntryName(String command) {
        String canonicalCommandName = getCanonicalCommandName(command);
        String name = prefix + NETCONF_RESULT_FOLDER + canonicalCommandName + NETCONF_RESULT_SUFFIX;
        return name;
    }

    @Override
    public boolean writable() {
        return zos != null;
    }

    @Override
    public void close() {
        this.zos = null;
    }

    @Override
    public void addConsoleResult(ConsoleCommand command, String response) throws IOException {
        assert writable();
        String cmd = command.getCommand();
        String name = getConsoleResultEntryName(command.getCommand());
        if (!cmd.equals(name)) {
            cmd = cmd + "(save as '" + name + "')";
        }
        ZipEntry zipEntry = new ZipEntry(name);
        zos.putNextEntry(zipEntry);
        zos.write(response.getBytes());
        zos.closeEntry();
        zos.flush();
        log.debug("added entry: " + cmd);
    }

    @Override
    public void addNetConfResult(String command, String response) throws IOException {
        assert writable();
        String name = getNetConfResultEntryName(command);
        ZipEntry zipEntry = new ZipEntry(name);
        zos.putNextEntry(zipEntry);
        zos.write(response.getBytes());
        zos.closeEntry();
        zos.flush();
        log.debug("added entry: " + command);
    }

    @Override
    public void addDevice(Device device) throws IOException {
        if (device == null) {
            return;
        }
        assert writable();
        String name = prefix + DEVICE_FOLDER + DEVICE_FILE;
        ZipEntry zipEntry = new ZipEntry(name);
        zos.putNextEntry(zipEntry);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(device);
        oos.close();
        zos.write(bos.toByteArray());
        zos.closeEntry();
        zos.flush();
        log.debug("added entry: " + device.getDeviceName());
    }

    @Override
    public void addMibDump(NodeInfo nodeinfo, String startingOid) throws IOException {
        assert writable();
        int timeout = nodeinfo.getSnmpTimeoutSec() * 1000;
        if (timeout < 0) {
            timeout = 0;
        }
        int version;
        if (nodeinfo.getEffectiveSnmpProtocol() == null) {
            throw new IllegalStateException("No effective snmp-protocol.");
        } else if (nodeinfo.getEffectiveSnmpProtocol().getProtocol().equals(Protocol.SNMP_V1)) {
            version = 1;
        } else {
            version = 2;
        }
        String filename = targetId + ".pdu";
        int walkInterval = 0;
        try {
            int walkInterval_ = Integer.parseInt(System.getProperty("snmp-walk-interval", "0"));
            walkInterval = walkInterval_;
        } catch (NumberFormatException nfe) {
        }

        log.debug("addMibDump: params are:");
        log.debug("- target: " + targetId);
        log.debug("- snmp-version: " + version);
        log.debug("- timeout: " + timeout);
        log.debug("- walk-interval: " + walkInterval);
        FileInputStream fis = null;
        File file = null;
        try {
            file = new File(filename);
            log.debug("addMibDump: begin mib-dump.");
            if (version == 1) {
                String[] argsV1 = {"walk-dump", targetId, nodeinfo.getCommunityStringRO(),
                        startingOid, "-check", "-o=" + filename, "-timeout=" + timeout,
                        "-version=v1", "-walk-interval=" + walkInterval};
                SnmpClient.main(argsV1);
            } else {
                String[] argsV2 = {"walk-dump", targetId, nodeinfo.getCommunityStringRO(),
                        startingOid, "-check", "-o=" + filename, "-timeout=" + timeout,
                        "-walk-interval=" + walkInterval};
                SnmpClient.main(argsV2);
            }
            log.debug("addMibDump: end mib-dump.");

            if (!file.exists()) {
                throw new IOException("PDU dump creation failed. " + file.getCanonicalPath());
            }

            ZipEntry mibdumpEntry = new ZipEntry(getMibZipEntryName());
            zos.putNextEntry(mibdumpEntry);
            fis = new FileInputStream(file);
            int c;
            while ((c = fis.read()) != -1) {
                zos.write(c);
            }
            zos.closeEntry();
            zos.flush();

        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            if (fis != null) {
                fis.close();
            }
            if (file != null) {
                file.delete();
            }
        }
    }

    @Override
    public void addMib(List<String> lines) throws IOException {
        try {
            ZipEntry mibdumpEntry = new ZipEntry(getMibZipEntryName());
            zos.putNextEntry(mibdumpEntry);
            for (String line : lines) {
                zos.write(line.getBytes());
                zos.write("\r\n".getBytes());
            }
            zos.closeEntry();
            zos.flush();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}