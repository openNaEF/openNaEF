package voss.model;

import java.io.Serializable;
import java.util.*;

public class ConfigTargets implements Serializable {
    private static final long serialVersionUID = 1L;

    private Device device_;

    private Set<LogicalPort> activatedLogicalPorts_ = new HashSet<LogicalPort>();
    private Set<LogicalPort> deactivatedLogicalPorts_ = new HashSet<LogicalPort>();
    private List<IfStack> ifStacks_ = new ArrayList<IfStack>();
    private Map<Port, List<ConfigProperty>> modifiedPortConfigProperties_ = new HashMap<Port, List<ConfigProperty>>();

    protected ConfigTargets() {
    }

    public ConfigTargets(Device device) {
        device_ = device;
    }

    public synchronized void addActivatedLogicalPort(LogicalPort logicalPort) {
        activatedLogicalPorts_.add(logicalPort);
    }

    public synchronized LogicalPort[] getActivatedLogicalPorts() {
        return activatedLogicalPorts_.toArray(new LogicalPort[0]);
    }

    public synchronized boolean isActivatedLogicalPort(LogicalPort logicalPort) {
        return activatedLogicalPorts_.contains(logicalPort);
    }

    public synchronized void addDeactivatedLogicalPort(LogicalPort port) {
        deactivatedLogicalPorts_.add(port);
    }

    public synchronized LogicalPort[] getDeactivatedLogicalPorts() {
        return deactivatedLogicalPorts_.toArray(new LogicalPort[0]);
    }

    public synchronized boolean isDeactivatedLogicalPort(LogicalPort port) {
        return deactivatedLogicalPorts_.contains(port);
    }

    public synchronized void addIfStack(IfStack ifStack) {
        for (Iterator<IfStack> i = ifStacks_.iterator(); i.hasNext(); ) {
            IfStack element = (IfStack) i.next();
            if (element.getHigherPort() == ifStack.getHigherPort() && element.getLowerPort() == ifStack.getLowerPort()) {
                throw new IllegalStateException("duplicated ifstack: " + ifStack.getDevice().getDeviceName() + ", higher="
                        + ifStack.getHigherPort().getIfName() + ", lower=" + ifStack.getLowerPort().getIfName());
            }
        }

        ifStacks_.add(ifStack);
    }

    public synchronized IfStack[] getIfStacks() {
        return (IfStack[]) ifStacks_.toArray(new IfStack[0]);
    }

    public IfStack getIfStack(Port higherPort, Port lowerPort) {
        IfStack result = null;

        IfStack[] ifStacks = getIfStacks();
        for (int i = 0; i < ifStacks.length; i++) {
            IfStack ifStack = ifStacks[i];
            if (ifStack.getHigherPort() == higherPort && ifStack.getLowerPort() == lowerPort) {
                if (result != null) {
                    throw new IllegalStateException("duplicated if-stack exists: higher=" + higherPort.getFullyQualifiedName()
                            + ", lower=" + lowerPort.getFullyQualifiedName());
                }

                result = ifStack;
            }
        }

        return result;
    }

    public IfStack[] getIfStacksOfHigherPort(Port higherPort) {
        IfStack[] ifStacks = getIfStacks();
        List<IfStack> result = new ArrayList<IfStack>();
        for (int i = 0; i < ifStacks.length; i++) {
            IfStack ifStack = ifStacks[i];
            if (ifStack.getHigherPort() == higherPort) {
                result.add(ifStack);
            }
        }
        return (IfStack[]) result.toArray(new IfStack[0]);
    }

    public IfStack[] getIfStacksOfLowerPort(Port lowerPort) {
        IfStack[] ifStacks = getIfStacks();
        List<IfStack> result = new ArrayList<IfStack>();
        for (int i = 0; i < ifStacks.length; i++) {
            IfStack ifStack = ifStacks[i];
            if (ifStack.getLowerPort() == lowerPort) {
                result.add(ifStack);
            }
        }
        return (IfStack[]) result.toArray(new IfStack[0]);
    }

    public LogicalEthernetPort[] getConnectConfigTargetLogicalEthernetPorts(VlanIf vlanIf) {
        return getConfigTargetLogicalEthernetPorts(vlanIf, IfStack.ConfigType.CONNECT);
    }

    public LogicalEthernetPort[] getDisconnectConfigTargetLogicalEthernetPorts(VlanIf vlanIf) {
        return getConfigTargetLogicalEthernetPorts(vlanIf, IfStack.ConfigType.DISCONNECT);
    }

    public LogicalEthernetPort[] getNoChangeLogicalEthernetPorts(VlanIf vlanIf) {
        return getConfigTargetLogicalEthernetPorts(vlanIf, IfStack.ConfigType.NO_CHANGE);
    }

    public LogicalEthernetPort[] getBaseLogicalEthernetPorts(VlanIf vlanIf) {
        Set<LogicalEthernetPort> result = new HashSet<LogicalEthernetPort>();
        result.addAll(Arrays.asList(getNoChangeLogicalEthernetPorts(vlanIf)));
        result.addAll(Arrays.asList(getDisconnectConfigTargetLogicalEthernetPorts(vlanIf)));
        return (LogicalEthernetPort[]) result.toArray(new LogicalEthernetPort[0]);
    }

    private LogicalEthernetPort[] getConfigTargetLogicalEthernetPorts(VlanIf vlanIf, IfStack.ConfigType configType) {
        Set<LogicalEthernetPort> result = new HashSet<LogicalEthernetPort>();

        LogicalEthernetPort[] taggedPorts = vlanIf.getTaggedPorts();
        for (int i = 0; i < taggedPorts.length; i++) {
            LogicalEthernetPort taggedPort = taggedPorts[i];
            IfStack ifStack = getIfStack(vlanIf, taggedPort);
            if (ifStack.getConfigType().equals(configType)) {
                result.add(taggedPort);
            }
        }

        LogicalEthernetPort[] untaggedPorts = vlanIf.getUntaggedPorts();
        for (int i = 0; i < untaggedPorts.length; i++) {
            LogicalEthernetPort untaggedPort = untaggedPorts[i];
            IfStack ifStack = getIfStack(vlanIf, untaggedPort);
            if (ifStack.getConfigType().equals(configType)) {
                result.add(untaggedPort);
            }
        }

        return (LogicalEthernetPort[]) result.toArray(new LogicalEthernetPort[0]);
    }

    public boolean isConnectConfigTargetEthernetPort(EthernetPort ethernetPort) {
        VlanDevice device = (VlanDevice) ethernetPort.getDevice();
        return Arrays.asList(getConnectConfigTargetEthernetPorts(device)).contains(ethernetPort);
    }

    public boolean isConnectConfigTargetLogicalEthernetPort(LogicalEthernetPort logicalEth) {
        VlanDevice device = (VlanDevice) logicalEth.getDevice();
        return Arrays.asList(getConnectConfigTargetLogicalEthernetPorts(device)).contains(logicalEth);
    }

    public boolean isDisconnectConfigTargetEthernetPort(EthernetPort ethernetPort) {
        VlanDevice device = (VlanDevice) ethernetPort.getDevice();
        return Arrays.asList(getDisconnectConfigTargetEthernetPorts(device)).contains(ethernetPort);
    }

    public boolean isDisconnectConfigTargetLogicalEthernetPort(LogicalEthernetPort logicalEth) {
        VlanDevice device = (VlanDevice) logicalEth.getDevice();
        return Arrays.asList(getDisconnectConfigTargetLogicalEthernetPorts(device)).contains(logicalEth);
    }

    private EthernetPort[] getPhysicalPorts(LogicalEthernetPort[] logicalEthernetPorts) {
        List<EthernetPort> result = new ArrayList<EthernetPort>();
        for (int i = 0; i < logicalEthernetPorts.length; i++) {
            result.addAll(Arrays.asList(logicalEthernetPorts[i].getPhysicalPorts()));
        }

        return (EthernetPort[]) result.toArray(new EthernetPort[0]);
    }

    private EthernetPort[] getConnectConfigTargetEthernetPorts(VlanDevice device) {
        return getPhysicalPorts(getConnectConfigTargetLogicalEthernetPorts(device));
    }

    private LogicalEthernetPort[] getConnectConfigTargetLogicalEthernetPorts(VlanDevice device) {
        Set<LogicalEthernetPort> result = new HashSet<LogicalEthernetPort>();

        VlanIf[] vlanIfs = device.getVlanIfs();
        for (int i = 0; i < vlanIfs.length; i++) {
            result.addAll(Arrays.asList(getConnectConfigTargetLogicalEthernetPorts(vlanIfs[i])));
        }

        return (LogicalEthernetPort[]) result.toArray(new LogicalEthernetPort[0]);
    }

    private EthernetPort[] getDisconnectConfigTargetEthernetPorts(VlanDevice device) {
        return getPhysicalPorts(getDisconnectConfigTargetLogicalEthernetPorts(device));
    }

    private LogicalEthernetPort[] getDisconnectConfigTargetLogicalEthernetPorts(VlanDevice device) {
        Set<LogicalEthernetPort> result = new HashSet<LogicalEthernetPort>();

        VlanIf[] vlanIfs = device.getVlanIfs();
        for (int i = 0; i < vlanIfs.length; i++) {
            result.addAll(Arrays.asList(getDisconnectConfigTargetLogicalEthernetPorts(vlanIfs[i])));
        }

        return (LogicalEthernetPort[]) result.toArray(new LogicalEthernetPort[0]);
    }

    public boolean isActivated(VlanIf vlanIf) {
        IfStack[] logicalPortBinds = getIfStacksOfHigherPort(vlanIf);
        for (int i = 0; i < logicalPortBinds.length; i++) {
            IfStack bind = logicalPortBinds[i];
            if (!bind.getConfigType().equals(IfStack.ConfigType.CONNECT)) {
                return false;
            }
        }
        return true;
    }

    public synchronized void addModifiedPortConfigProperty(Port port, ConfigProperty configProperty) {
        if (port == null || configProperty == null) {
            throw new NullArgumentIsNotAllowedException();
        }

        List<ConfigProperty> properties = modifiedPortConfigProperties_.get(port);
        if (properties == null) {
            properties = new ArrayList<ConfigProperty>();
            modifiedPortConfigProperties_.put(port, properties);
        }
        properties.add(configProperty);
    }

    public synchronized boolean hasModified(Port port) {
        return modifiedPortConfigProperties_.get(port) != null;
    }

    public synchronized Port[] getModifyConfigTargetPorts() {
        List<Port> result = new ArrayList<Port>();

        Port[] ports = device_.getPorts();
        for (int i = 0; i < ports.length; i++) {
            if (hasModified(ports[i])) {
                result.add(ports[i]);
            }
        }

        return (Port[]) result.toArray(new Port[0]);
    }
}