package voss.discovery.agent.common;


import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.model.Device;

import java.io.IOException;
import java.util.List;

public class SimulatedDeviceDiscovery implements DeviceDiscovery {
    private final Device device;

    public SimulatedDeviceDiscovery(Device device) {
        this.device = device;
    }

    @Override
    public Device getDevice() {
        return this.device;
    }

    @Override
    public DeviceAccess getDeviceAccess() {
        return null;
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
    }

    @Override
    public String getTextConfiguration() throws IOException, AbortedException {
        return null;
    }

    @Override
    public void getDeviceInformation() throws IOException, AbortedException {
    }

    @Override
    public void getPhysicalConfiguration() throws IOException, AbortedException {
    }

    @Override
    public void getLogicalConfiguration() throws IOException, AbortedException {
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
    }

    @Override
    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public void getCommandResult(ConsoleCommand command) throws IOException,
            AbortedException {
    }

    @Override
    public void executeCommand(DiscoveryLogicUnit logic)
            throws ConsoleException, IOException, AbortedException {
    }

    @Override
    public void getCommandResults(List<ConsoleCommand> commands)
            throws IOException, AbortedException {
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException,
            ConsoleException, AbortedException {
    }

    @Override
    public void setDiscoveryStatusDone(DiscoveryStatus status) {
    }

    @Override
    public boolean isDiscoveryDone(DiscoveryStatus status) {
        return true;
    }

    @Override
    public void setRecord(boolean value) {
    }

    @Override
    public void saveCacheAsArchive() {
    }

    @Override
    public void close() {
    }
}