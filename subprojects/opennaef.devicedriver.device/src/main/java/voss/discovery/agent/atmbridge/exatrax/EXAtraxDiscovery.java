package voss.discovery.agent.atmbridge.exatrax;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.atmbridge.EAConverterDiscovery;
import voss.discovery.agent.common.Constants;
import voss.discovery.agent.common.OSType;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.DeviceNotCollectableStateException;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.GlobalMode;
import voss.discovery.iolib.simpletelnet.NullMode;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.model.*;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EXAtraxDiscovery extends EAConverterDiscovery {
    private static final Logger log = LoggerFactory.getLogger(EXAtraxDiscovery.class);

    public static final ConsoleCommand show_cpu = new ConsoleCommand(new GlobalMode(), "show cpu");
    public static final ConsoleCommand show_version = new ConsoleCommand(new GlobalMode(), "show version");
    public static final ConsoleCommand show_lag_all = new ConsoleCommand(new GlobalMode(), "show lag all");
    public static final ConsoleCommand show_ip_route = new ConsoleCommand(new GlobalMode(), "show ip route");
    public static final ConsoleCommand show_ip_all = new ConsoleCommand(new GlobalMode(), "show ip all");
    public static final ConsoleCommand show_config_running = new ConsoleCommand(new GlobalMode(), "show config running");

    public EXAtraxDiscovery(DeviceAccess access) throws IOException, AbortedException,
            ConsoleException, DeviceNotCollectableStateException {
        super(access, new EXAtraxTelnetUtil());
        checkCollectableByTelnet();
    }

    @Override
    protected void setVendorSpecificAttributes(Device device) throws IOException, ConsoleException, AbortedException {
        device.setVendorName(Constants.VENDOR_SII);
        device.setOsTypeName(OSType.EXAOS.caption);
    }

    protected void slots() throws IOException, ConsoleException, AbortedException {
        int[] slotIndex =
                ((EXAtraxTelnetUtil) telnetUtil).getSlotIndexs(telnet);
        for (int i = 0; i < slotIndex.length; i++) {
            Slot slotInfo = new SlotImpl();
            slotInfo.initContainer(device);
            slotInfo.initSlotIndex(slotIndex[i]);
            String moduleModelName =
                    ((EXAtraxTelnetUtil) telnetUtil).getModuleName(
                            telnet,
                            slotIndex[i]);
            if (moduleModelName != null && moduleModelName.length() != 0) {
                Module module = new ModuleImpl();
                module.initSlot(slotInfo);
                module.setModelTypeName(moduleModelName);
            }
        }
    }

    private void checkCollectableByTelnet() throws IOException, ConsoleException,
            AbortedException, DeviceNotCollectableStateException {
        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        if (console == null) {
            return;
        }
        console.connect();
        if (config.getCpuLoadLimitCurrent() < getCpuUsingMean5sec(telnet)) {
            log.info("CPU:5sec:" + getCpuUsingMean5sec(telnet));
            throw new DeviceNotCollectableStateException("CPU:5sec:" + getCpuUsingMean5sec(telnet));
        }
        if (config.getCpuLoadLimitIn1min() < getCpuUsingMean1min(telnet)) {
            log.info("CPU:1min:" + getCpuUsingMean1min(telnet));
            throw new DeviceNotCollectableStateException("CPU:1min:" + getCpuUsingMean1min(telnet));
        }
    }

    private static int getCpuUsingMean5sec(ConsoleAccess telnet)
            throws IOException, ConsoleException, AbortedException {
        return parseCpuUtilization(telnet.getResponse(show_cpu), 1);
    }

    private static int getCpuUsingMean1min(ConsoleAccess telnet)
            throws IOException, ConsoleException, AbortedException {
        return parseCpuUtilization(telnet.getResponse(show_cpu), 1);
    }

    public static int parseCpuUtilization(String value, int line) {
        String lines[] = value.split("\n");
        Pattern cpuUtilizationPattern = Pattern.compile("\\s*([0-9]+)\\s%.*");
        Matcher matcher = cpuUtilizationPattern.matcher(lines[line].trim());
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            throw new RuntimeException("!match()");
        }
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException,
            ConsoleException, AbortedException {
        SimulationEntry entry = recorder.addEntry(getDeviceAccess().getTargetAddress().getHostAddress());
        NodeInfo nodeInfo = getDeviceAccess().getNodeInfo();
        entry.addMibDump(nodeInfo, ".1");

        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        if (console != null) {
            console.connect();
            getLogicalConfiguration();
            Map<String, String> cache = getConsoleAccess().getCachedCommandResult();
            for (Map.Entry<String, String> e : cache.entrySet()) {
                ConsoleCommand cmd = new ConsoleCommand(new NullMode(), e.getKey());
                entry.addConsoleResult(cmd, e.getValue());
            }
            if (!cache.keySet().contains(show_config_running.getCommand())) {
                String res1 = console.getResponse(show_config_running);
                entry.addConsoleResult(show_config_running, res1);
            }
            if (!cache.keySet().contains(show_version.getCommand())) {
                String res2 = console.getResponse(show_version);
                entry.addConsoleResult(show_version, res2);
            }
            console.close();
        }

        entry.close();
    }

}