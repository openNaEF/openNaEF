package voss.discovery.agent.common;


import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.netconf.NetConfException;
import voss.model.Device;

import java.io.IOException;
import java.util.List;

public interface DeviceDiscovery {

    Device getDevice();

    DeviceAccess getDeviceAccess();

    void getConfiguration() throws IOException, AbortedException;

    String getTextConfiguration() throws IOException, AbortedException;

    void getDeviceInformation() throws IOException, AbortedException;

    void getPhysicalConfiguration() throws IOException, AbortedException;

    void getLogicalConfiguration() throws IOException, AbortedException;

    void getDynamicStatus() throws IOException, AbortedException;

    void getNeighbor() throws IOException, AbortedException;

    void getCommandResult(ConsoleCommand command) throws IOException, AbortedException;

    void executeCommand(DiscoveryLogicUnit logic) throws ConsoleException, IOException, AbortedException;

    void getCommandResults(List<ConsoleCommand> commands) throws IOException, AbortedException;

    void record(DeviceRecorder recorder) throws IOException, ConsoleException, NetConfException, AbortedException;

    void setDiscoveryStatusDone(DiscoveryStatus status);

    boolean isDiscoveryDone(DiscoveryStatus status);

    void setRecord(boolean value);

    void saveCacheAsArchive();

    void close();
}