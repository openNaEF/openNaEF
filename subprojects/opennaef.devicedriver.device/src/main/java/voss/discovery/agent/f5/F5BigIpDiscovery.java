package voss.discovery.agent.f5;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.netconf.NetConfException;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.model.Device;
import voss.model.MplsVlanDevice;

import java.io.IOException;
import java.util.Date;

public class F5BigIpDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(F5BigIpDiscovery.class);
    private String textConfiguration = null;
    private ConfigurationStructure config = null;
    @SuppressWarnings("unused")
    private final AgentConfiguration agentConfig;

    private ConsoleCommand show_running_config = new ConsoleCommand(
            new voss.discovery.iolib.simpletelnet.F5BigIpSshClient.BigIpSysConfigMode(), "list /");

    public F5BigIpDiscovery(DeviceAccess access)
            throws IOException, AbortedException {
        super(access);
        this.agentConfig = AgentConfiguration.getInstance();
    }

    private MplsVlanDevice device;

    public Device getDeviceInner() {
        return this.device;
    }

    public void getDeviceInformation() throws IOException, AbortedException {
        device = new MplsVlanDevice();
        String fqn = Mib2Impl.getSysName(getDeviceAccess().getSnmpAccess());
        String hostname = fqn;
        String domainName = null;
        int idx = fqn.indexOf('.');
        if (idx != -1) {
            hostname = fqn.substring(0, idx);
            domainName = fqn.substring(idx + 1);
        }
        device.setModelTypeName("BIP-IP");
        String sysDescr = Mib2Impl.getSysDescr(getDeviceAccess().getSnmpAccess());
        device.setVendorName(Constants.VENDOR_CISCO);
        device.setOsTypeName(OSType.NX_OS.caption);
        device.setDeviceName(hostname);
        device.setDomainName(domainName);
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setDescription(sysDescr);
        device.setCommunityRO(getDeviceAccess().getSnmpAccess().getCommunityString());
        device.setIpAddress(getDeviceAccess().getSnmpAccess().getSnmpAgentAddress().getAddress()
                .getHostAddress());
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getDeviceAccess().getSnmpAccess()).longValue()));

        if (this.config == null) {
            try {
                this.getConfiguration();
            } catch (Exception e) {
                throw new IOException("Cannot get configuration.", e);
            }
        }
        setDiscoveryStatusDone(DiscoveryStatus.DEVICE_INFORMATION);
    }

    public void getPhysicalConfiguration() throws IOException, AbortedException {
        if (device == null) {
            throw new IllegalArgumentException();
        }
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }
        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }
        if (getDeviceAccess().getConsoleAccess() == null) {
            return;
        }
        if (!isDiscoveryDone(DiscoveryStatus.TEXT_CONFIGURATION)) {
            getConfiguration();
        }
        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    @Override
    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
    }

    @Override
    public String getTextConfiguration() throws IOException, AbortedException {
        ConsoleAccess console = null;
        try {
            console = this.getDeviceAccess().getConsoleAccess();
            if (console == null) {
                return "Cannot connect: no console access.";
            }
            console.connect();
            String res1 = console.getResponse(show_running_config);
            this.textConfiguration = res1;
            setDiscoveryStatusDone(DiscoveryStatus.TEXT_CONFIGURATION);
            log.debug("getTextConfiguration(): ["
                    + this.getSnmpAccess().getSnmpAgentAddress().getAddress().getHostAddress()
                    + "] result:\r\n" + res1 + "\r\n----");
        } catch (ConsoleException ce) {
            throw new IOException(ce);
        } finally {
        }
        setDiscoveryStatusDone(DiscoveryStatus.TEXT_CONFIGURATION);
        return this.textConfiguration;
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
        if (this.textConfiguration == null) {
            getTextConfiguration();
        }
        SimpleF5TmosConfigurationParser parser = new SimpleF5TmosConfigurationParser(this.textConfiguration);
        parser.parse();
        this.config = parser.getConfigurationStructure();
        log.debug("config-structure: " + config.getRootElement().toString());
        this.device.gainConfigurationExtInfo().put(
                ExtInfoNames.DEVICE_CONFIG_STRUCTURE, this.config);
        setDiscoveryStatusDone(DiscoveryStatus.CONFIGURATION);
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException, ConsoleException, NetConfException, AbortedException {
        SimulationEntry entry = recorder.addEntry(getDeviceAccess().getTargetAddress().getHostAddress());

        entry.addMibDump(getDeviceAccess().getNodeInfo(), ".1");

        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        if (console != null) {
            console.connect();
            addConsoleResult(console, show_running_config, entry);
            console.close();
        }

        entry.close();
    }

    private void addConsoleResult(ConsoleAccess console, ConsoleCommand cmd, SimulationEntry entry)
            throws AbortedException, IOException, ConsoleException {
        String res = console.getResponse(cmd);
        entry.addConsoleResult(cmd, res);
    }
}