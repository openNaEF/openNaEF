package voss.discovery.agent.dsl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.model.*;

import java.util.HashMap;
import java.util.Map;

public class VlanModelBuilder {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(VlanModelBuilder.class);
    private static final Logger dslLog = LoggerFactory.getLogger("DSL");

    private final VlanDevice device;
    private final Map<String, VlanIf> vlanIfs = new HashMap<String, VlanIf>();

    public VlanModelBuilder(VlanDevice device) {
        if (device == null) {
            throw new IllegalArgumentException();
        }
        this.device = device;
        for (VlanIf vlanIf : this.device.getVlanIfs()) {
            String key = VlanIfImpl.getExtendedVlanId(vlanIf.getEoeId(), vlanIf.getVlanId());
            this.vlanIfs.put(key, vlanIf);
        }
    }

    public VlanIf buildVlanIf(int vlanID) {
        return buildVlanIf(null, vlanID);
    }

    public VlanIf buildVlanIf(Integer eoeID, int vlanID) {
        VlanIf vlanIf = new VlanIfImpl();
        vlanIf.initDevice(this.device);
        vlanIf.initVlanId(eoeID, vlanID);
        vlanIf.initVlanKey(vlanIf.getExtendedVlanId());
        this.device.addPort(vlanIf);
        this.vlanIfs.put(vlanIf.getExtendedVlanId(), vlanIf);
        dslLog.debug("@device '" + device.getDeviceName() + "'; " +
                "create vlan '" + vlanIf.getVlanKey() + "'");
        return vlanIf;
    }

    public VlanIf buildRouterVlanIf(LogicalEthernetPort routerPort, Integer eoeID, int vlanID) {
        RouterVlanIf vlanIf = new RouterVlanIfImpl();
        vlanIf.initDevice(this.device);
        this.device.addPort(vlanIf);
        vlanIf.initVlanId(eoeID, vlanID);
        String key = routerPort.getIfName() + ":" + vlanIf.getExtendedVlanId();
        vlanIf.initVlanKey(key);
        vlanIf.initRouterPort(routerPort);
        this.vlanIfs.put(key, vlanIf);
        dslLog.debug("@device '" + device.getDeviceName() + "';" +
                "create vlan '" + vlanIf.getVlanKey() + "'");
        dslLog.debug("@device '" + device.getDeviceName() + "';" +
                "set vlan '" + vlanIf.getVlanKey() + "' key '" + key + "'");
        return vlanIf;
    }

    public VlanIf buildRouterVlanIf(LogicalEthernetPort routerPort, Integer eoeID, int vlanID, String key) {
        RouterVlanIf vlanIf = new RouterVlanIfImpl();
        vlanIf.initDevice(this.device);
        this.device.addPort(vlanIf);
        vlanIf.initVlanId(eoeID, vlanID);
        vlanIf.initVlanKey(key);
        vlanIf.initRouterPort(routerPort);
        this.vlanIfs.put(key, vlanIf);
        dslLog.debug("@device '" + device.getDeviceName() + "';" +
                "create vlan '" + vlanIf.getVlanKey() + "'");
        dslLog.debug("@device '" + device.getDeviceName() + "';" +
                "set vlan '" + vlanIf.getVlanKey() + "' key '" + key + "'");
        return vlanIf;
    }

    public void setVlanIfIndex(Integer eoeID, int vlanID, int ifIndex) {
        VlanIf vlanIf = getVlanIf(eoeID, vlanID);
        setVlanIfIndex(vlanIf, ifIndex);
    }

    public void setVlanIfIndex(VlanIf vlanIf, int ifIndex) {
        vlanIf.initIfIndex(ifIndex);
        vlanIf.initVlanIfIndex(ifIndex);
        dslLog.debug("@device '" + device.getDeviceName() + "';" +
                "set vlan '" + vlanIf.getExtendedVlanId() + "' ifindex '" + ifIndex + "'");
    }

    public void setVlanName(Integer eoeID, int vlanID, String vlanName) {
        VlanIf vlanIf = getVlanIf(eoeID, vlanID);
        setVlanName(vlanIf, vlanName);
    }

    public void setVlanName(VlanIf vlanIf, String vlanName) {
        vlanIf.initIfName(vlanName);
        vlanIf.setVlanName(vlanName);
        dslLog.debug("@device '" + device.getDeviceName() + "'; " +
                "set vlan '" + vlanIf.getExtendedVlanId() + "' name '" + vlanName + "'");
    }

    public void setVlanOperationalStatus(Integer eoeID, int vlanID, String status) {
        VlanIf vlanIf = getVlanIf(eoeID, vlanID);
        setVlanOperationalStatus(vlanIf, status);
    }

    public void setVlanOperationalStatus(VlanIf vlanIf, String status) {
        vlanIf.setOperationalStatus(status);
        dslLog.debug("@device '" + device.getDeviceName() + "'; " +
                "set vlan '" + vlanIf.getExtendedVlanId() + "' status '" + status + "'");
    }

    public void setVlanPortUsage(LogicalEthernetPort logical, VlanPortUsage usage) {
        logical.setVlanPortUsage(usage);
        dslLog.debug("@device '" + device.getDeviceName() + "'; " +
                "set port '" + logical.getIfName() + "' vlan-mode '" + usage.toString() + "'");
    }

    public void addTaggedVlan(Integer eoeID, int vlanID, LogicalEthernetPort logical) {
        VlanIf vlanIf = getVlanIf(eoeID, vlanID);
        addTaggedVlan(vlanIf, logical);
    }

    public void addTaggedVlan(VlanIf vlanIf, LogicalEthernetPort logical) {
        addBoundPort(vlanIf, logical);
        vlanIf.addTaggedPort(logical);
        dslLog.debug("@device '" + device.getDeviceName() + "'; " +
                "set port '" + logical.getIfName() + "' add-tagged-vlan '" + vlanIf.getExtendedVlanId() + "'");
    }

    public void setUntaggedVlan(Integer eoeID, int vlanID, LogicalEthernetPort logical) {
        VlanIf vlanIf = getVlanIf(eoeID, vlanID);
        setUntaggedVlan(vlanIf, logical);
    }

    public void setUntaggedVlan(VlanIf vlanIf, LogicalEthernetPort logical) {
        addBoundPort(vlanIf, logical);
        vlanIf.addUntaggedPort(logical);
        dslLog.debug("@device '" + device.getDeviceName() + "'; " +
                "set port '" + logical.getIfName() + "' untagged-vlan '" + vlanIf.getExtendedVlanId() + "'");
    }

    private void addBoundPort(VlanIf vlanIf, LogicalEthernetPort logical) {
        if (vlanIf instanceof RouterVlanIf) {
            ((RouterVlanIf) vlanIf).initRouterPort(logical);
        }
    }

    private VlanIf getVlanIf(Integer eoeID, int vlanID) {
        String key = VlanIfImpl.getExtendedVlanId(eoeID, vlanID);
        VlanIf vlanIf = vlanIfs.get(key);
        if (vlanIf == null) {
            throw new IllegalArgumentException("no vlan found: " + key);
        }
        return vlanIf;
    }

}