package voss.discovery.iolib.simulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.model.Device;
import voss.model.NodeInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class OldSimulationEntryImpl implements SimulationEntry {
    private static final String MIB_SUFFIX = ".pdu";

    private final static Logger log = LoggerFactory.getLogger(OldSimulationEntryImpl.class);
    private final String targetId;
    private final SimulationArchive archive;
    private final ZipFile archiveZip;

    public OldSimulationEntryImpl(String targetId, SimulationArchive archive) throws IOException {
        if (targetId == null) {
            throw new IllegalArgumentException();
        }
        if (archive == null) {
            throw new IllegalArgumentException();
        }
        this.targetId = targetId;
        this.archive = archive;
        this.archiveZip = new ZipFile(archive.getSimulationArchiveFile());
    }

    public OldSimulationEntryImpl(String targetId, ZipOutputStream zos) throws IOException {
        throw new RuntimeException("not supported.");
    }

    @Override
    public String getTargetId() {
        return this.targetId;
    }

    @Override
    public boolean exists() throws IOException, ZipException {
        ZipFile zip = new ZipFile(this.archive.getSimulationArchiveFile());
        ZipEntry entry = zip.getEntry(targetId + MIB_SUFFIX);
        if (entry != null) {
            return true;
        }
        return false;
    }

    @Override
    public void createTargetSimulatorEntry() throws IOException {
        throw new RuntimeException("not supported.");
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
        String name = targetId + MIB_SUFFIX;
        log.debug("getMibZipEntry(): name=" + name);
        return name;
    }

    @Override
    public InputStream getConsoleCommandResponse(String command) throws IOException {
        throw new RuntimeException("not supported.");
    }

    @Override
    public InputStream getNetConfCommandResponse(String command) throws IOException {
        throw new RuntimeException("not supported.");
    }

    @Override
    public InputStream getConfig() throws IOException {
        throw new RuntimeException("not supported.");
    }

    @Override
    public Device getDevice() throws IOException {
        throw new RuntimeException("not supported.");
    }

    @Override
    public boolean writable() {
        return false;
    }

    @Override
    public void close() {
    }

    @Override
    public void addConsoleResult(ConsoleCommand command, String response) throws IOException {
        throw new RuntimeException("not supported.");
    }

    public void addNetConfResult(String command, String response) throws IOException {
        throw new RuntimeException("not supported.");
    }

    @Override
    public void addMibDump(NodeInfo nodeinfo, String startingOid) throws IOException {
        throw new RuntimeException("not supported.");
    }

    @Override
    public void addMib(List<String> lines) throws IOException {
        throw new RuntimeException("not supported.");
    }

    @Override
    public void addDevice(Device device) throws IOException {
        throw new RuntimeException("not supported.");
    }
}