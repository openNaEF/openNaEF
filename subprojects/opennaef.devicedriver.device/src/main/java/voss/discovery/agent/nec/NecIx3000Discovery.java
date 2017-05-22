package voss.discovery.agent.nec;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.Constants;
import voss.discovery.agent.common.DeviceDiscoveryImpl;
import voss.discovery.agent.common.DiscoveryStatus;
import voss.discovery.agent.common.OSType;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.GlobalMode;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.model.Device;
import voss.model.MplsVlanDevice;

import java.io.IOException;
import java.util.Date;

public class NecIx3000Discovery extends DeviceDiscoveryImpl {
    private static final Logger log = LoggerFactory.getLogger(NecIx3000Discovery.class);
    private final MplsVlanDevice device;
    private String textConfiguration;

    private ConsoleCommand show_running_config = new ConsoleCommand(new GlobalMode(), "show running-config");
    private ConsoleCommand show_version = new ConsoleCommand(new GlobalMode(), "show version");

    public NecIx3000Discovery(DeviceAccess access) {
        super(access);
        this.device = new MplsVlanDevice();
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
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
    public void getDeviceInformation() throws IOException, AbortedException {
        String fqn = Mib2Impl.getSysName(getDeviceAccess().getSnmpAccess());
        String hostname = fqn;
        String domainName = null;
        int idx = fqn.indexOf('.');
        if (idx != -1) {
            hostname = fqn.substring(0, idx);
            domainName = fqn.substring(idx + 1);
        }
        String sysObjectId = Mib2Impl.getSysObjectId(getDeviceAccess().getSnmpAccess());
        device.setModelTypeName(getDeviceType(sysObjectId));
        String sysDescr = Mib2Impl.getSysDescr(getDeviceAccess().getSnmpAccess());
        device.setVendorName(Constants.VENDOR_NEC);
        device.setOsTypeName(OSType.PICO.caption);
        device.setDeviceName(hostname);
        device.setDomainName(domainName);
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setDescription(sysDescr);
        device.setCommunityRO(getDeviceAccess().getSnmpAccess().getCommunityString());
        device.setIpAddress(getDeviceAccess().getSnmpAccess().getSnmpAgentAddress().getAddress()
                .getHostAddress());
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getDeviceAccess().getSnmpAccess()).longValue()));
    }

    private String getDeviceType(String oid) {
        String[] s = oid.split("\\.");
        try {
            int typeID = Integer.parseInt(s[s.length - 2]);
            int modelID = Integer.parseInt(s[s.length - 1]);
            switch (typeID) {
                case 1:
                    return getIx1000DeviceType(modelID);
                case 2:
                    return getIx2000DeviceType(modelID);
                case 3:
                    return "AG500-series";
                case 4:
                    return "IX2200";
                case 5:
                    return "IX2003";
                case 6:
                    return "AG431";
                case 7:
                    return getIx3000DeviceType(modelID);
                case 8:
                    return "IX2004";
                case 9:
                    return "AG600-series";
                case 10:
                    return getIx3100DeviceType(modelID);
                case 11:
                    return "RTU";
                case 12:
                    return "IX2025";
                case 13:
                    return "IX2105";
            }
        } catch (NumberFormatException e) {
        }
        return "unknown[" + oid + "]";
    }

    private String getIx1000DeviceType(int modelID) {
        switch (modelID) {
            case 1:
                return "IX1010";
            case 2:
                return "IX1020";
            case 3:
                return "IX1050";
            case 4:
                return "IX1011";
            case 5:
                return "IX1035";
            case 6:
                return "IX1035i";
            case 7:
                return "IX1036";
            case 8:
                return "IX1036i";
        }
        return "Unknown[IX1000-" + modelID + "]";
    }

    private String getIx2000DeviceType(int modelID) {
        switch (modelID) {
            case 1:
                return "IX2010";
            case 2:
                return "IX2015";
        }
        return "Unknown[IX2000-" + modelID + "]";
    }

    private String getIx3000DeviceType(int modelID) {
        switch (modelID) {
            case 1:
                return "IX3010";
        }
        return "unknown[IX3000-" + modelID + "]";
    }

    private String getIx3100DeviceType(int modelID) {
        switch (modelID) {
            case 1:
                return "IX3110";
        }
        return "unknown[IX3100-" + modelID + "]";
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
    public void record(DeviceRecorder recorder) throws IOException, ConsoleException, AbortedException {
        SimulationEntry entry = recorder.addEntry(getDeviceAccess().getTargetAddress().getHostAddress());

        entry.addMibDump(getDeviceAccess().getNodeInfo(), ".1");

        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        if (console != null) {
            console.connect();
            String res1 = console.getResponse(show_running_config);
            entry.addConsoleResult(show_running_config, res1);
            String res4 = console.getResponse(show_version);
            entry.addConsoleResult(show_version, res4);
            console.close();
        }
        entry.close();
    }

    @Override
    public Device getDeviceInner() {
        return this.device;
    }

}