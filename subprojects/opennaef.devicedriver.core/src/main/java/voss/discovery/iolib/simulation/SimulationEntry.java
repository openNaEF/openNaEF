package voss.discovery.iolib.simulation;

import voss.discovery.iolib.console.ConsoleCommand;
import voss.model.Device;
import voss.model.NodeInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipException;

public interface SimulationEntry {

    String getTargetId();

    boolean exists() throws IOException, ZipException;

    void createTargetSimulatorEntry() throws IOException;

    InputStream getMibZipInputStream() throws IOException;

    String getMibZipEntryName();

    InputStream getConsoleCommandResponse(String command) throws IOException;

    InputStream getNetConfCommandResponse(String command) throws IOException;

    InputStream getConfig() throws IOException;

    Device getDevice() throws IOException;

    boolean writable();

    void close();

    void addConsoleResult(ConsoleCommand command, String response) throws IOException;

    void addNetConfResult(String command, String response) throws IOException;

    void addMibDump(NodeInfo nodeinfo, String startingOid) throws IOException;

    void addMib(List<String> lines) throws IOException;

    void addDevice(Device device) throws IOException;
}