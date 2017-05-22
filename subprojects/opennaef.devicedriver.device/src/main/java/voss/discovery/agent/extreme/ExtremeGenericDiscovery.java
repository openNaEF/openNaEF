package voss.discovery.agent.extreme;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.agent.mib.MauMib;
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
import voss.model.GenericEthernetSwitch;

import java.io.IOException;
import java.util.Date;

public class ExtremeGenericDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(ExtremeGenericDiscovery.class);

    protected final GenericEthernetSwitch device;
    protected final Mib2Impl mib2;
    protected final InterfaceMibImpl ifmib;
    protected final MauMib mauMib;
    protected final ExtremeMibImpl extremeMib;

    private ConsoleCommand show_configuration = new ConsoleCommand(new GlobalMode(), "show configuration");
    private ConsoleCommand show_version = new ConsoleCommand(new GlobalMode(), "show version");

    public ExtremeGenericDiscovery(DeviceAccess access) {
        super(access);
        this.device = new GenericEthernetSwitch();

        this.mib2 = new Mib2Impl(getDeviceAccess().getSnmpAccess());
        this.ifmib = new InterfaceMibImpl(getDeviceAccess().getSnmpAccess());
        this.mauMib = new MauMib(getDeviceAccess().getSnmpAccess());
        this.extremeMib = new ExtremeMibImpl(getDeviceAccess().getSnmpAccess());
    }

    public ExtremeGenericDiscovery(DeviceAccess access, ExtremeMibImpl patchedMibImpl) {
        super(access);
        this.device = new GenericEthernetSwitch();
        this.device.setVendorName(Constants.VENDOR_EXTREME);

        this.mib2 = new Mib2Impl(getDeviceAccess().getSnmpAccess());
        this.ifmib = new InterfaceMibImpl(getDeviceAccess().getSnmpAccess());
        this.mauMib = new MauMib(getDeviceAccess().getSnmpAccess());
        this.extremeMib = patchedMibImpl;
    }

    @Override
    public Device getDeviceInner() {
        try {
            extremeMib.updateIpAddress(device);
        } catch (IOException e) {
            log.error(e.toString());
        } catch (AbortedException e) {
            log.error(e.toString());
        }
        return this.device;
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
    }

    @Override
    public void getDeviceInformation() throws IOException, AbortedException {
        device.setCommunityRO(getDeviceAccess().getSnmpAccess().getCommunityString());
        device.setIpAddress(getDeviceAccess().getTargetAddress().getHostAddress());
        device.setDeviceName(mib2.getSysName());
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        String sysDescr = Mib2Impl.getSysDescr(getDeviceAccess().getSnmpAccess());
        device.setDescription(sysDescr);
        device.setContactInfo(Mib2Impl.getSysContact(getDeviceAccess().getSnmpAccess()));
        device.setLocation(Mib2Impl.getSysLocation(getDeviceAccess().getSnmpAccess()));

        device.setModelTypeName(extremeMib.getModelName());
        device.setOsVersion(extremeMib.getOsVersion());
        device.setOsTypeName(extremeMib.getOsType(device.getOsVersion()));
        device.setGatewayAddress(mib2.getGatewayAddress());
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getDeviceAccess().getSnmpAccess()).longValue()));

        setDiscoveryStatusDone(DiscoveryStatus.DEVICE_INFORMATION);
    }

    @Override
    public void getPhysicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }
        extremeMib.prepare(device);
        extremeMib.createSlots(device);
        extremeMib.createPhysicalPorts(device);
        extremeMib.createAggregationGroup(device);

        mauMib.setMauTypes(device);
        ifmib.setAllIfAdminStatus(device);
        ifmib.setAllIfOperStatus(device);
        ifmib.setAllIfSpeed(device);
        ifmib.setAllIfDescriptions(device);

        DeviceInfoUtil.supplementDefaultLogicalEthernetPortInfo(device);
        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);

    }

    @Override
    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }
        extremeMib.createVlanIf(device);
        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
    }

    @Override
    public String getTextConfiguration() throws IOException, AbortedException {
        return null;
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException,
            ConsoleException, AbortedException {
        SimulationEntry entry = null;
        try {
            entry = recorder.addEntry(getDeviceAccess().getTargetAddress().getHostAddress());

            entry.addMibDump(getDeviceAccess().getNodeInfo(), ".1");

            ConsoleAccess console = getDeviceAccess().getConsoleAccess();
            if (console != null) {
                console.connect();
                String res1 = console.getResponse(show_configuration);
                entry.addConsoleResult(show_configuration, res1);
                String res2 = console.getResponse(show_version);
                entry.addConsoleResult(show_version, res2);

                console.close();
            }
        } finally {
            if (entry != null) {
                entry.close();
            }
        }
    }
}