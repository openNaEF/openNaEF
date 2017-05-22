package voss.discovery.agent.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.dsl.VlanModelBuilder;
import voss.model.*;
import voss.model.impl.PortCrossConnectionImpl;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class MplsModelBuilder {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(MplsModelBuilder.class);
    private static final Logger dslLog = LoggerFactory.getLogger("DSL");

    private final MplsVlanDevice device;
    private final VlanModelBuilder vlanBuilder;
    private final Map<Integer, PseudoWirePortImpl> pseudoWires = new HashMap<Integer, PseudoWirePortImpl>();

    public MplsModelBuilder(MplsVlanDevice device) {
        if (device == null) {
            throw new IllegalArgumentException();
        }
        this.device = device;
        this.vlanBuilder = new VlanModelBuilder(this.device);
    }

    public MplsVlanDevice getDevice() {
        return this.device;
    }

    public PseudoWirePort buildPseudoWire(int pwID) {
        PseudoWirePortImpl pw = new PseudoWirePortImpl();
        pw.initDevice(device);
        pw.initIfName("PseudoWire-" + pwID);
        pw.initPseudoWireID(pwID);
        pw.setPeerPwId(pwID);
        this.pseudoWires.put(pwID, pw);
        dslLog.info("@device '" + this.device.getDeviceName() + "';" +
                "create pseudo-wire '" + pwID + "'");
        dslLog.info("@device '" + this.device.getDeviceName() + "';" +
                "set pseudo-wire '" + pwID + "' if-name '" + pw.getIfName() + "'");
        return pw;
    }

    public void setPseudoWireTransmitLsp(int pwID, MplsTunnel lsp) {
        PseudoWirePort pw = getPseudoWirePort(pwID);
        pw.setTransmitLsp(lsp);
    }

    public void setPseudoWireReceiveLspName(int pwID, String lspName) {
        PseudoWirePort pw = getPseudoWirePort(pwID);
        pw.setReceiveLspName(lspName);
    }

    public void setAlcatelPipeType(int pwID, AlcatelPipeType pipeType) {
        PseudoWirePort pw = getPseudoWirePort(pwID);
        setAlcatelPipeType(pw, pipeType);
    }

    public void setAlcatelPipeType(PseudoWirePort pw, AlcatelPipeType pipeType) {
        pw.setAlcatelPipeType(pipeType);
        dslLog.info("@device '" + this.device.getDeviceName() + "';" +
                "set pseudo-wire '" + pw.getPseudoWireID() + "' pipe-type '" + pipeType.name() + "'");
    }

    public void setPseudoWireType(int pwID, PseudoWireType type) {
        PseudoWirePort pw = getPseudoWirePort(pwID);
        setPseudoWireType(pw, type);
    }

    public void setPseudoWireType(PseudoWirePort pw, PseudoWireType type) {
        pw.setPseudoWireType(type);

        dslLog.info("@device '" + this.device.getDeviceName() + "';" +
                "set pseudo-wire '" + pw.getPseudoWireID() + "' type '" + type.toString() + "'");
    }

    public void setPseudoWireVcIndex(int pwID, int vcIndex) {
        PseudoWirePort pw = getPseudoWirePort(pwID);
        setPseudoWireVcIndex(pw, vcIndex);
    }

    public void setPseudoWireVcIndex(PseudoWirePort pw, int vcIndex) {
        pw.setVcIndex(vcIndex);
    }

    public void setPseudoWirePeerAddress(int pwID, InetAddress peerIpAddress) {
        PseudoWirePort pw = getPseudoWirePort(pwID);
        setPseudoWirePeerAddress(pw, peerIpAddress);
    }

    public void setPseudoWirePeerAddress(PseudoWirePort pw, InetAddress peerIpAddress) {
        pw.setPeerIpAddress(peerIpAddress);
    }

    public void setPseudoWireName(int pwID, String name) {
        PseudoWirePort pw = getPseudoWirePort(pwID);
        setPseudoWireName(pw, name);
    }

    public void setPseudoWireName(PseudoWirePort pw, String name) {
        pw.setPwName(name);
    }

    public void setPseudoWireDescription(int pwID, String descr) {
        PseudoWirePort pw = getPseudoWirePort(pwID);
        setPseudoWireDescription(pw, descr);
    }

    public void setPseudoWireDescription(PseudoWirePort pw, String descr) {
        pw.setIfDescr(descr);
        pw.setUserDescription(descr);
    }

    public void setPseudoWireAdminStatus(PseudoWirePort pw, PseudoWireOperStatus status) {
        pw.setPseudoWireAdminStatus(status);
        pw.setAdminStatus(status.toString());
    }

    public void setPseudoWireAdminStatus(int pwID, PseudoWireOperStatus status) {
        PseudoWirePort pw = getPseudoWirePort(pwID);
        setPseudoWireAdminStatus(pw, status);
    }

    public void setPseudoWireOperStatus(int pwID, PseudoWireOperStatus status) {
        PseudoWirePort pw = getPseudoWirePort(pwID);
        setPseudoWireOperStatus(pw, status);
    }

    public void setPseudoWireOperStatus(PseudoWirePort pw, PseudoWireOperStatus status) {
        pw.setPseudoWireOperStatus(status);
        pw.setOperationalStatus(status.toString());
    }

    public void buildIPv4RoutingEngine() {
        new IPv4RoutingEngine(this.device);
    }

    public void buildDirectConnection(int pwID, LogicalEthernetPort logical) {
        PseudoWirePort pw = getPseudoWirePort(pwID);
        buildDirectConnection(pw, logical);
    }

    public void buildDirectConnection(PseudoWirePort pw, LogicalEthernetPort logical) {
        assert pw != null;
        assert logical != null;

        pw.setAttachedCircuitPort(logical);
        PortCrossConnection xc = new PortCrossConnectionImpl();
        xc.initDevice(this.device);
        xc.addPort(pw);
        xc.addPort(logical);
        this.device.getPortCrossConnectionEngine().addPortCrossConnection(xc);
    }

    public void buildVlanConnection(int pwID, int vlanId, LogicalEthernetPort logical) {
        buildVlanConnection(pwID, null, vlanId, logical);
    }

    public void buildVlanConnection(int pwID, Integer eoeID, int vlanId, LogicalEthernetPort logical) {
        PseudoWirePort pw = getPseudoWirePort(pwID);
        buildVlanConnection(pw, eoeID, vlanId, logical);
    }

    public void buildVlanConnection(PseudoWirePort pw, Integer eoeID, int vlanID, LogicalEthernetPort logical) {
        VlanIf vlanIf = this.device.getPortRelatedVlanIf(logical, eoeID, vlanID);
        if (vlanIf == null) {
            vlanIf = vlanBuilder.buildRouterVlanIf(logical, eoeID, vlanID);
            vlanBuilder.setVlanName(vlanIf, vlanIf.getVlanKey());
        }
        buildVlanConnection(pw, vlanIf, logical);
    }

    public void buildVlanConnection(PseudoWirePort pw, VlanIf vlanIf, LogicalEthernetPort logical) {
        assert logical != null;
        assert vlanIf != null;

        if (pw.getEmulationPort() != null) {
            throw new IllegalStateException("duplicated ethernet port simulation port: " +
                    "device=" + device.getDeviceName() + "; pwID=" + pw.getPseudoWireID());
        }
        createPseudoWireEhternetEmulationPort(pw);
        if (!(pw.getEmulationPort() instanceof PseudoWireEthernetEmulationPort)) {
            throw new IllegalStateException("not ethernet port simulation port: " +
                    "device=" + device.getDeviceName() + "; pwID=" + pw.getPseudoWireID());
        }
        LogicalEthernetPort pwEther = (LogicalEthernetPort) pw.getEmulationPort();
        vlanBuilder.addTaggedVlan(vlanIf, logical);
        vlanBuilder.setUntaggedVlan(vlanIf, pwEther);
    }

    private void createPseudoWireEhternetEmulationPort(PseudoWirePort pw) {
        PseudoWireEthernetEmulationPort eth = new PseudoWireEthernetEmulationPortImpl();
        eth.initDevice(device);
        eth.initIfName(pw.getIfName() + "/ethernetVlan");
        pw.setEmulationPort(eth);
        eth.initPseudoWirePort(pw);
    }

    private PseudoWirePortImpl getPseudoWirePort(int pwID) {
        if (pseudoWires.get(pwID) != null) {
            return pseudoWires.get(pwID);
        }
        throw new IllegalStateException("unknown pwID: " + pwID);
    }
}