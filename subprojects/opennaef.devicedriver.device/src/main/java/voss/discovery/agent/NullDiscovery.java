package voss.discovery.agent;


import voss.discovery.agent.common.DeviceDiscovery;
import voss.discovery.agent.common.DiscoveryLogicUnit;
import voss.discovery.agent.common.DiscoveryStatus;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.model.Device;

import java.io.IOException;
import java.util.List;

public class NullDiscovery implements DeviceDiscovery {

    @Override
    public void getCommandResult(ConsoleCommand command) throws IOException,
            AbortedException {
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
    }

    @Override
    public Device getDevice() {
        return null;
    }

    @Override
    public DeviceAccess getDeviceAccess() {
        return null;
    }

    @Override
    public void getDeviceInformation() throws IOException, AbortedException {
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
    }

    @Override
    public void getLogicalConfiguration() throws IOException, AbortedException {
    }

    @Override
    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public void getPhysicalConfiguration() throws IOException, AbortedException {
    }

    @Override
    public String getTextConfiguration() throws IOException, AbortedException {
        return null;
    }

    @Override
    public boolean isDiscoveryDone(DiscoveryStatus status) {
        return false;
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException,
            ConsoleException {
    }

    @Override
    public void setDiscoveryStatusDone(DiscoveryStatus status) {
    }

    @Override
    public void getCommandResults(List<ConsoleCommand> commands)
            throws IOException, AbortedException {
    }

    @Override
    public void executeCommand(DiscoveryLogicUnit logic) throws ConsoleException, IOException, AbortedException {
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