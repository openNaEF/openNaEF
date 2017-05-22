package voss.discovery.agent.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.VossExtInfoKeys;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.netconf.NetConfAccess;
import voss.discovery.iolib.simpletelnet.NullMode;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.model.Device;
import voss.model.NonConfigurationExtInfo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;


public abstract class DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(DeviceDiscoveryImpl.class);
    public static final String KEY_ARCHIVE_DIR = "voss.discovery.debug-archive";
    private final DeviceAccess access;
    private final Set<DiscoveryStatus> doneTasks = new HashSet<DiscoveryStatus>();
    private boolean record = false;

    public DeviceDiscoveryImpl(DeviceAccess access) {
        this.access = access;
    }

    @Override
    public final Device getDevice() {
        Device device = getDeviceInner();
        device.setSite(this.access.getNodeInfo().getSiteName());
        if (needIpAddressSupplement()) {
            supplementInetAddressWithMask(device);
        }
        return device;
    }

    protected String getDeviceFqn() throws IOException, AbortedException {
        return Mib2Impl.getSysName(getSnmpAccess());
    }

    protected String getHostNamePart(String fqn) {
        int idx = fqn.indexOf('.');
        if (idx != -1) {
            return fqn.substring(0, idx);
        }
        return fqn;
    }

    protected String getDomainNamePart(String fqn) {
        int idx = fqn.indexOf('.');
        if (idx != -1) {
            return fqn.substring(idx + 1);
        }
        return null;
    }

    protected String getSysObjectID() throws IOException, AbortedException {
        return Mib2Impl.getSysObjectId(getSnmpAccess());
    }

    protected int getLastSysObjectIDNumber() throws IOException, AbortedException {
        String oid = getSysObjectID();
        if (oid == null) {
            throw new IllegalStateException("no sysObjectId");
        }
        int idx = oid.lastIndexOf('.');
        String lastPart = oid.substring(idx + 1);
        return Integer.parseInt(lastPart);
    }

    protected boolean needIpAddressSupplement() {
        return true;
    }

    private void supplementInetAddressWithMask(Device device) {
        try {
            Mib2Impl.setIpAddressWithIfIndex(getDeviceAccess().getSnmpAccess(), device);
        } catch (Exception e) {
            log.warn(e.toString(), e);
        }
    }

    public void saveCacheAsArchive() {
        if (!this.record) {
            return;
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");
        String prefix = df.format(new Date());
        String hostName = getSnmpAccess().getSnmpAgentAddress().getAddress().getHostAddress();
        hostName = hostName.replace(':', '.');
        String fileName = prefix + "_" + hostName;
        DeviceRecorder recorder = null;
        try {
            String dirName = System.getProperty(KEY_ARCHIVE_DIR);
            if (dirName != null) {
                File dir = new File(dirName);
                recorder = new DeviceRecorder(dir, fileName);
            } else {
                recorder = new DeviceRecorder(fileName);
            }
            SimulationEntry entry = recorder.addEntry(getDeviceAccess().getTargetAddress().getHostAddress());
            entry.addMib(getSnmpAccess().getCachedVarbinds());
            if (getConsoleAccess() != null) {
                saveConsoleResult(entry);
            }
            if (getNetConfAccess() != null) {
                saveNetConfResult(entry);
            }
            entry.close();
        } catch (Exception e) {
            log.warn("failed to record().", e);
        } finally {
            if (recorder != null) {
                try {
                    recorder.close();
                } catch (IOException ex) {
                    log.warn("failed to recorder.close().", ex);
                }
            }
        }
    }

    private void saveConsoleResult(SimulationEntry entry) throws IOException {
        Map<String, String> cache = getConsoleAccess().getCachedCommandResult();
        for (Map.Entry<String, String> e : cache.entrySet()) {
            ConsoleCommand cmd = new ConsoleCommand(new NullMode(), e.getKey());
            entry.addConsoleResult(cmd, e.getValue());
        }
    }

    private void saveNetConfResult(SimulationEntry entry) throws IOException {
        Map<String, String> cache = getNetConfAccess().getCachedCommandResult();
        for (Map.Entry<String, String> e : cache.entrySet()) {
            entry.addNetConfResult(e.getKey(), e.getValue());
        }
    }

    public final void close() {
        try {
            getSnmpAccess().close();
        } catch (Exception e) {
        }
        try {
            if (getConsoleAccess() != null) {
                getConsoleAccess().close();
            }
        } catch (Exception e) {
            log.warn("exception occurred on console.close().", e);
        }
        try {
            if (getNetConfAccess() != null) {
                getNetConfAccess().close();
            }
        } catch (Exception e) {
            log.warn("exception occurred on netconf.close().", e);
        }
    }

    public abstract Device getDeviceInner();

    public DeviceAccess getDeviceAccess() {
        return this.access;
    }

    public SnmpAccess getSnmpAccess() {
        return this.access.getSnmpAccess();
    }

    public ConsoleAccess getConsoleAccess() {
        return this.access.getConsoleAccess();
    }

    public NetConfAccess getNetConfAccess() {
        return this.access.getNetConfAccess();
    }

    protected final void connect() throws IOException, AbortedException, ConsoleException {
        if (getConsoleAccess() == null) {
            throw new ConsoleException("no console access");
        }
        if (!getConsoleAccess().isConnected()) {
            getConsoleAccess().connect();
        }
    }

    protected final void disconnect() throws IOException, AbortedException, ConsoleException {
        if (getConsoleAccess() == null) {
            return;
        }
        if (getConsoleAccess().isConnected()) {
            getConsoleAccess().close();
        }
    }

    @Override
    public boolean isDiscoveryDone(DiscoveryStatus status) {
        return doneTasks.contains(status);
    }

    @Override
    public void setDiscoveryStatusDone(DiscoveryStatus status) {
        this.doneTasks.add(status);
        log.info("Discovery Task done: " + status.toString());
    }

    @Override
    public void executeCommand(DiscoveryLogicUnit logic) throws IOException, AbortedException {
        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        boolean connectOnThisSession = false;
        try {
            if (logic.isConsoleNeeded()) {
                if (console == null) {
                    log.warn("getCommandResult(): no console info.");
                    return;
                }
                if (!console.isConnected()) {
                    console.connect();
                    connectOnThisSession = true;
                }
            }
            logic.execute(getDeviceAccess());
        } catch (ConsoleException ce) {
            throw new IOException(ce);
        } finally {
            if (console != null && console.isConnected() && connectOnThisSession) {
                try {
                    console.close();
                } catch (ConsoleException ce) {
                    throw new IOException(ce);
                }
            }
        }
    }

    @Override
    public void getCommandResult(ConsoleCommand command) throws IOException, AbortedException {
        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        boolean connectOnThisSession = false;
        if (console == null) {
            log.warn("getCommandResult(): no console info.");
            return;
        }
        try {
            if (!console.isConnected()) {
                console.connect();
                connectOnThisSession = true;
            }
            getCommandResult(console, command);
        } catch (ConsoleException ce) {
            throw new IOException(ce);
        } finally {
            if (console != null && console.isConnected() && connectOnThisSession) {
                try {
                    console.close();
                } catch (ConsoleException ce) {
                    throw new IOException(ce);
                }
            }
        }
    }

    @Override
    public void getCommandResults(List<ConsoleCommand> commands) throws IOException, AbortedException {
        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        boolean connectOnThisSession = false;
        if (console == null) {
            log.warn("getCommandResults(): no console connection.");
            return;
        }
        try {
            if (!console.isConnected()) {
                console.connect();
                connectOnThisSession = true;
            }
            for (ConsoleCommand command : commands) {
                getCommandResult(console, command);
            }
        } catch (ConsoleException ce) {
            throw new IOException(ce);
        } finally {
            if (console != null && console.isConnected() && connectOnThisSession) {
                try {
                    console.close();
                } catch (ConsoleException ce) {
                    throw new IOException(ce);
                }
            }
        }
    }

    private void getCommandResult(ConsoleAccess console, ConsoleCommand command) throws IOException, AbortedException {
        try {
            if (console == null) {
                return;
            }
            String result = console.getResponse(command);
            if (result == null) {
                result = "N/A";
            }
            NonConfigurationExtInfo extinfo = getDeviceInner().gainNonConfigurationExtInfo();
            @SuppressWarnings("unchecked")
            Map<String, String> adhocCommandResults =
                    (Map<String, String>) extinfo.get(VossExtInfoKeys.SNAPSHOT_EXT_INFO_KEYS);
            if (adhocCommandResults == null) {
                adhocCommandResults = new HashMap<String, String>();
                extinfo.put(VossExtInfoKeys.SNAPSHOT_EXT_INFO_KEYS, adhocCommandResults);
            }
            adhocCommandResults.put(command.getExtInfoKeyName(), result);
            log.info("command " + command.getExtInfoKeyName() + " executed.");
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void setRecord(boolean record) {
        this.record = record;
    }

    public boolean isRecord() {
        return this.record;
    }
}