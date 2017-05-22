package voss.nms.inventory.diff.network;

import naef.dto.NodeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.Util;
import voss.discovery.agent.common.DeviceDiscovery;
import voss.discovery.agent.common.DiscoveryFactory;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.DeviceAccessFactory;
import voss.model.Device;
import voss.model.NodeInfo;
import voss.nms.inventory.diff.DiffExecutionThread.ExecutionTask;

import java.util.concurrent.Callable;

public class DiscoveryTask implements ExecutionTask, Callable<DiscoveryResult> {
    private static final long serialVersionUID = 1L;
    private final DeviceAccessFactory factory;
    private final String taskName;
    private final NodeInfo info;
    private final NodeDto node;
    private DeviceAccess access;
    private Device device;
    private final boolean archive;

    public DiscoveryTask(DeviceAccessFactory factory, String taskName, NodeDto node, NodeInfo info, boolean archive) {
        this.factory = factory;
        assert !Util.isNull(taskName, node, info);
        this.taskName = taskName;
        this.node = node;
        this.info = info;
        this.archive = archive;
    }

    public String getTaskName() {
        return this.taskName;
    }

    public DeviceAccess getDeviceAccess() {
        return this.access;
    }

    public NodeDto getNode() {
        return this.node;
    }

    public NodeInfo getNodeInfo() {
        return this.info;
    }

    public DiscoveryResult call() {
        Logger log = LoggerFactory.getLogger(DiscoveryTask.class);
        log.debug("call() started.");
        DeviceDiscovery discovery = null;
        try {
            this.access = this.factory.getDeviceAccess(this.info);
            if (this.access == null) {
                throw new IllegalStateException("no device-access on "
                        + this.taskName + " ["
                        + this.info.getNodeIdentifier() + "]");
            }
            log.info("discovery started: " + this.taskName + " ["
                    + this.access.getTargetAddress().getHostAddress() + "]");
            discovery = DiscoveryFactory.getInstance().getDiscovery(this.access);
            discovery.setRecord(archive);
            discovery.getDeviceInformation();
            discovery.getPhysicalConfiguration();
            discovery.getLogicalConfiguration();
            this.device = discovery.getDevice();
        } catch (Exception e) {
            log.warn("got exception in Thread.", e);
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            this.device = new ErrorDummyDevice(rootCause.getMessage());
            this.device.setDeviceName(this.info.getNodeIdentifier());
            if (discovery != null) {
                discovery.saveCacheAsArchive();
            }
        } finally {
            if (discovery != null) {
                discovery.close();
            }
        }
        log.debug("call() ended.");
        DiscoveryResult result = new DiscoveryResult();
        result.device = this.device;
        result.node = this.node;
        return result;
    }
}