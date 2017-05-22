package voss.discovery.agent.atmbridge.superhub;


import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.atmbridge.EAConverterDiscovery;
import voss.discovery.agent.common.Constants;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.DeviceNotCollectableStateException;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.GlobalMode;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.model.*;

import java.io.IOException;


public class SuperHubCollector extends EAConverterDiscovery {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(SuperHubCollector.class);

    public static final ConsoleCommand show_running_config = new ConsoleCommand(new GlobalMode(), "show running-config");
    public static final ConsoleCommand show_version = new ConsoleCommand(new GlobalMode(), "show version");
    public static final ConsoleCommand show_atm_shapingport = new ConsoleCommand(new GlobalMode(), "show atm shapingport");

    public SuperHubCollector(DeviceAccess access) throws IOException, AbortedException,
            DeviceNotCollectableStateException {
        super(access, new SuperHubTelnetUtil());
        checkCollectableBySnmp();
    }

    @Override
    public void setVendorSpecificAttributes(Device device) throws IOException, ConsoleException, AbortedException {
        device.setVendorName(Constants.VENDOR_NEC);
    }

    protected void slots() throws IOException, AbortedException {
        String[] slotIndex = ((SuperHubTelnetUtil) telnetUtil).getSlotIndexs(telnet);
        for (int i = 0; i < slotIndex.length; i++) {
            if (!isSlotIndex(slotIndex[i])) {
                continue;
            }
            Slot slotInfo = new SlotImpl();
            slotInfo.initContainer(device);
            slotInfo.initSlotIndex(asIndex(slotIndex[i]));
            slotInfo.initSlotId(slotIndex[i]);
            String moduleModelName = ((SuperHubTelnetUtil) telnetUtil).getModuleName(telnet, slotIndex[i]);
            if (moduleModelName != null && moduleModelName.length() != 0) {
                Module module = new ModuleImpl();
                module.initSlot(slotInfo);
                module.setModelTypeName(moduleModelName);
            }
        }
    }

    private boolean isSlotIndex(String slotIndex) {
        if (slotIndex == null) {
            return false;
        }
        if (!slotIndex.endsWith("L") && !slotIndex.endsWith("R")) {
            return false;
        }
        if (slotIndex.length() < 2) {
            return false;
        }
        try {
            Integer.parseInt(slotIndex.substring(0, slotIndex.length() - 1));
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private static int asIndex(String slot) {
        final int preIndex = Integer.parseInt(slot.substring(0, slot.length() - 1));
        final String sufString = slot.substring(slot.length() - 1);
        if (sufString.endsWith("L")) {
            return preIndex * 2 - 2;
        } else {
            return preIndex * 2 - 1;
        }
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException,
            ConsoleException, AbortedException {
        SimulationEntry entry = recorder.addEntry(getDeviceAccess().getTargetAddress().getHostAddress());

        entry.addMibDump(getDeviceAccess().getNodeInfo(), ".1");

        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        if (console != null) {
            console.connect();
            String res1 = console.getResponse(show_running_config);
            entry.addConsoleResult(show_running_config, res1);

            String res3 = console.getResponse(show_version);
            entry.addConsoleResult(show_version, res3);

            console.close();
        }

        entry.close();
    }

    private static final String OID_CPU_LOAD_RATE = ".1.3.6.1.4.1.119.1.51.1.1.2.1";
    private static final String OID_CPU_LOAD_MEAN_1MIN = ".1.3.6.1.4.1.119.1.51.1.1.2.2";
    private static final String OID_CPU_LOAD_MEAN_5MIN = ".1.3.6.1.4.1.119.1.51.1.1.2.3";

    private void checkCollectableBySnmp() throws IOException, AbortedException, DeviceNotCollectableStateException {
        try {
            final int cpuUsingRate = SnmpUtil.getInteger(snmp, OID_CPU_LOAD_RATE);
            if (config.getCpuLoadLimitCurrent() < cpuUsingRate) {
                throw new DeviceNotCollectableStateException("CPU_LOAD_RATE:" + cpuUsingRate);
            }
            final int cputUsingMean1min = SnmpUtil.getInteger(snmp, OID_CPU_LOAD_MEAN_1MIN);
            if (config.getCpuLoadLimitIn1min() < cputUsingMean1min) {
                throw new DeviceNotCollectableStateException("CPU_LOAD_MEAN_1MIN:" + cputUsingMean1min);
            }
            final int cputUsingMean5min = SnmpUtil.getInteger(snmp, OID_CPU_LOAD_MEAN_5MIN);
            if (config.getCpuLoadLimitIn5min() < cputUsingMean5min) {
                throw new DeviceNotCollectableStateException("CPU_LOAD_MEAN_5MIN:" + cputUsingMean5min);
            }
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }
}