package voss.model;


import java.util.*;

public class MplsVlanDevice extends GenericEthernetSwitch {

    private static final long serialVersionUID = 2L;

    private final List<MplsTunnel> teTunnels = new ArrayList<MplsTunnel>();
    private IPv4RoutingEngine v4RoutingEngine = null;

    public void setIPv4RoutingEngine(IPv4RoutingEngine engine) {
        this.v4RoutingEngine = engine;
    }

    public IPv4RoutingEngine getIPv4RoutingEngine() {
        return this.v4RoutingEngine;
    }

    public void addSlot(int slotIndex, String slotId) {
        Slot current = this.getSlotBySlotIndex(slotIndex);
        if (current != null) {
            throw new IllegalArgumentException("exists slotIndex=" + slotIndex);
        }
        Slot slot = new SlotImpl();
        slot.initContainer(this);
        slot.initSlotIndex(slotIndex);
        slot.initSlotId(slotId);
    }

    public void clearTeTunnels() {
        this.teTunnels.clear();
    }

    public void addTeTunnel(MplsTunnel tunnel) {
        this.teTunnels.add(tunnel);
    }

    public void addTeTunnels(Collection<MplsTunnel> tunnels) {
        this.teTunnels.addAll(tunnels);
    }

    public List<MplsTunnel> getTeTunnels() {
        List<MplsTunnel> result = new ArrayList<MplsTunnel>();
        result.addAll(this.teTunnels);
        return result;
    }

    public List<PseudoWirePort> getPseudoWirePorts() {
        return Arrays.asList(selectPorts(PseudoWirePort.class));
    }

    public PseudoWirePort getPseudoWirePortByPwId(int pwId) {
        for (PseudoWirePort port : getPseudoWirePorts()) {
            if (port.getPseudoWireID() == pwId) {
                return port;
            }
        }
        return null;
    }

    public PseudoWirePort getPseudoWirePortByVcId(int vcId) {
        for (PseudoWirePort port : getPseudoWirePorts()) {
            if (port.getVcIndex() == vcId) {
                return port;
            }
        }
        return null;
    }

    public PseudoWirePort getPseudoWirePortByName(String name) {
        for (PseudoWirePort port : getPseudoWirePorts()) {
            if (port.getPwName() != null && port.getPwName().equals(name)) {
                return port;
            }
        }
        return null;
    }

    public VlanIf getVlanIfByVlanKey(String key) {
        return (VlanIf) getVlankeys().getUniqueValue(key);
    }

    public Set<VlanIf> getPortRelatedVlanIfs(Port port) {
        Set<VlanIf> result = new HashSet<VlanIf>();
        for (VlanIf vlanIf : getVlanIfs()) {
            if (Arrays.asList(vlanIf.getBindedPorts()).contains(port)) {
                result.add(vlanIf);
            }
        }
        return result;
    }

    public VlanIf getPortRelatedVlanIf(Port port, Integer eoeID, int vlanID) {
        for (VlanIf vlanIf : getVlanIfs()) {
            if (vlanIf instanceof RouterVlanIf) {
                if (vlanIf.isSameVlan(eoeID, vlanID)
                        && ((RouterVlanIf) vlanIf).getRouterPort().equals(port)) {
                    return (RouterVlanIf) vlanIf;
                }
            } else if (vlanIf instanceof VlanIf) {
                if (vlanIf.isSameVlan(eoeID, vlanID)) {
                    return vlanIf;
                }
            }
        }
        return null;
    }

    public List<VlanIf> getRouterVlanIfsOn(Port port) {
        List<VlanIf> result = new ArrayList<VlanIf>();
        if (port == null) {
            return result;
        }
        for (VlanIf vif : this.getVlanIfs()) {
            if (!RouterVlanIf.class.isInstance(vif)) {
                continue;
            }
            RouterVlanIf rvif = (RouterVlanIf) vif;
            if (port.equals(rvif.getRouterPort())) {
                result.add(rvif);
            }
        }
        return result;
    }

    public Set<VlanIf> getVlanIfsByVlanId(Integer id) {
        return getVlanIfsByVlanIdWithoutMe(null, id, null);
    }

    public Set<VlanIf> getVlanIfsByVlanIdWithoutMe(Integer eoeID, Integer vlanID, VlanIf exclude) {
        if (vlanID == null) {
            throw new IllegalArgumentException("vlanID is null.");
        }
        Set<VlanIf> result = new HashSet<VlanIf>();
        VlanIf[] allVlans = getVlanIfs();
        for (VlanIf vlanIf : allVlans) {
            if (exclude != null && vlanIf == exclude) {
                continue;
            }
            if (vlanIf instanceof RouterVlanIf) {
                if (vlanIf.getEoeId() == eoeID && vlanIf.getVlanId() == vlanID.intValue()) {
                    result.add((RouterVlanIf) vlanIf);
                }
            } else if (vlanIf instanceof VlanIf) {
                if (vlanIf.getEoeId() == eoeID && vlanIf.getVlanId() == vlanID.intValue()) {
                    result.add((VlanIf) vlanIf);
                }
            } else {
                throw new IllegalStateException(
                        "vlan is not instance of VlanIf or RouterVlanIf: "
                                + vlanIf.getFullyQualifiedName()
                                + "(" + vlanIf.getClass().getName() + ")");
            }
        }
        return result;
    }

    public VrfInstance getVrfByVrfKey(String key) {
        return getVrfKeys().getUniqueValue(key);
    }

    public VrfInstance getPortRelatedVrf(Port port) {
        for (VrfInstance vrf : getVrfs()) {
            if (vrf.getAttachmentPorts().contains(port)) {
                return vrf;
            }
        }
        return null;
    }

    public List<VrfInstance> getVrfs() {
        VrfInstance[] instances = selectPorts(VrfInstance.class);
        return Arrays.asList(instances);
    }

    @Override
    protected synchronized <T extends Port> void sortPorts(Class<T> type, List<Port> ports) {
        if (PseudoWirePort.class.isAssignableFrom(type)) {
            VlanModelUtils.sortPseudoWirePort(ports);
        } else {
            super.sortPorts(type, ports);
        }
    }

    public boolean hasIpAddress(String ip) {
        if (ip == null) {
            return false;
        }

        if (this.getIpAddress() != null && this.getIpAddress().equals(ip)) {
            return true;
        } else if (this.getIpAddresses().length > 0) {
            for (String ipAddress : this.getIpAddresses()) {
                if (ipAddress.equals(ip)) {
                    return true;
                }
            }
        }
        return false;
    }

    private transient PortIndex<String, VlanIf> vlanKeyIndex;

    private synchronized PortIndex<String, VlanIf> getVlankeys() {
        if (vlanKeyIndex == null) {
            vlanKeyIndex = new PortIndex<String, VlanIf>(
                    "vlandevice:eoeid+vlanid-vlanif") {

                protected boolean isTargetPort(Port port) {
                    return port instanceof VlanIf;
                }

                protected boolean isInitializable(VlanIf vlanif) {
                    try {
                        return vlanif.getVlanKey() != null;
                    } catch (NotInitializedException nie) {
                        return false;
                    }
                }

                protected boolean isUniqueKey(String key) {
                    return true;
                }

                @Override
                protected boolean isMultipleKeyEntity() {
                    return false;
                }

                @Override
                protected List<String> getKeys(VlanIf vlanif) {
                    throw new IllegalStateException();
                }

                protected String getKey(VlanIf vlanif) {
                    return vlanif.getVlanKey();
                }

                protected Set<VlanIf> getInitialValues() {
                    return new HashSet<VlanIf>(Arrays
                            .asList(MplsVlanDevice.this.getVlanIfs()));
                }

                protected String getKeyString(String key) {
                    return key;
                }
            };
        }
        return vlanKeyIndex;
    }

    private transient PortIndex<String, VrfInstance> vrfKeyIndex;

    private synchronized PortIndex<String, VrfInstance> getVrfKeys() {
        if (vrfKeyIndex == null) {
            vrfKeyIndex = new PortIndex<String, VrfInstance>(
                    "vrfinstance") {

                protected boolean isTargetPort(Port port) {
                    return port instanceof VrfInstance;
                }

                protected boolean isInitializable(VrfInstance vrf) {
                    try {
                        return vrf.getVrfID() != null;
                    } catch (NotInitializedException nie) {
                        return false;
                    }
                }

                protected boolean isUniqueKey(String key) {
                    return true;
                }

                @Override
                protected boolean isMultipleKeyEntity() {
                    return false;
                }

                @Override
                protected List<String> getKeys(VrfInstance vrf) {
                    throw new IllegalStateException();
                }

                protected String getKey(VrfInstance vrf) {
                    return vrf.getVrfID();
                }

                protected Set<VrfInstance> getInitialValues() {
                    return new HashSet<VrfInstance>(MplsVlanDevice.this.getVrfs());
                }

                protected String getKeyString(String key) {
                    return key;
                }
            };
        }
        return vrfKeyIndex;
    }

}