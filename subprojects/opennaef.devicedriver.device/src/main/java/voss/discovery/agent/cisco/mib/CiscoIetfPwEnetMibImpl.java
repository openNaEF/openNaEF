package voss.discovery.agent.cisco.mib;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.cisco.CiscoIosCommandParseUtil;
import voss.discovery.agent.common.MplsModelBuilder;
import voss.discovery.agent.dsl.VlanModelBuilder;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.mib.PseudoWireVlanMode;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.KeyCreator;
import voss.model.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

public class CiscoIetfPwEnetMibImpl implements CiscoIetfPwEnetMib {
    private final static Logger log = LoggerFactory.getLogger(CiscoIetfPwEnetMibImpl.class);
    private final SnmpAccess snmp;
    private final MplsVlanDevice device;
    private final MplsModelBuilder mplsBuilder;
    private final VlanModelBuilder vlanBuilder;

    public CiscoIetfPwEnetMibImpl(SnmpAccess snmp, MplsVlanDevice device) {
        this.snmp = snmp;
        this.device = device;
        this.mplsBuilder = new MplsModelBuilder(this.device);
        this.vlanBuilder = new VlanModelBuilder(this.device);
    }


    public void getPwEnetMib() throws IOException, AbortedException {
        connectPwAndEthernetPort();
    }

    public void connectPwAndEthernetPort() throws IOException, AbortedException {
        Map<VcEnetEntryKey, SnmpEntry> cpwVcEnetVlanModes =
                SnmpUtil.getWalkResult(snmp, cpwVcEnetVlanMode, vcEnetEntryKeyCreator);
        Map<VcEnetEntryKey, SnmpEntry> cpwVcEnetPortVlans =
                SnmpUtil.getWalkResult(snmp, cpwVcEnetPortVlan, vcEnetEntryKeyCreator);
        Map<VcEnetEntryKey, SnmpEntry> cpwVcEnetPortIfIndices =
                SnmpUtil.getWalkResult(snmp, cpwVcEnetPortIfIndex, vcEnetEntryKeyCreator);

        for (Map.Entry<VcEnetEntryKey, SnmpEntry> entry : cpwVcEnetPortVlans.entrySet()) {
            boolean isTaggedVlan = false;

            VcEnetEntryKey key = entry.getKey();
            int vcIndex = key.vcIndex.intValue();
            int vlanId = IntSnmpEntry.getInstance(entry.getValue()).intValue();

            PseudoWirePort pw = device.getPseudoWirePortByVcId(vcIndex);
            if (pw == null) {
                throw new IllegalArgumentException("unknown vcIndex: " + vcIndex);
            }

            IntSnmpEntry portIfIndexMib = IntSnmpEntry.getInstance(cpwVcEnetPortIfIndices.get(key));
            int ethernetPortIfIndex = portIfIndexMib.intValue();

            String ifname = Mib2Impl.getIfName(snmp, ethernetPortIfIndex);
            ifname = "interface " + CiscoIosCommandParseUtil.getFullyQualifiedInterfaceName(ifname);
            ifname = CiscoIosCommandParseUtil.getParentInterfaceName(ifname);
            ifname = CiscoIosCommandParseUtil.getShortIfName(ifname);

            EthernetPort port = (EthernetPort) device.getPortByIfName(ifname);
            if (port == null) {
                throw new IllegalStateException("no ethernet port found: " +
                        "cpwVcEnetEntry.portIfIndex=" + ethernetPortIfIndex
                        + ", ifname=" + ifname);
            }
            LogicalEthernetPort logical = device.getLogicalEthernetPort(port);
            if (logical == null) {
                throw new IllegalStateException("no logical ethernet port found: "
                        + port.getFullyQualifiedName());
            }

            SnmpEntry vlanModeEntry = cpwVcEnetVlanModes.get(key);
            IntSnmpEntry vlanModeMib = IntSnmpEntry.getInstance(vlanModeEntry);
            PseudoWireVlanMode vlanMode = PseudoWireVlanMode.getById(vlanModeMib.intValue());

            switch (vlanMode) {
                case addVlan:
                    log.trace("logical->" + logical.getIfName() + ", vlanId->" + vlanId);
                    VlanIf vlanIf = device.getPortRelatedVlanIf(logical, null, vlanId);
                    if (vlanIf == null) {
                        vlanIf = vlanBuilder.buildRouterVlanIf(logical, null, vlanId);
                        vlanBuilder.setVlanIfIndex(vlanIf, 0 - vlanId);
                        vlanBuilder.setVlanName(vlanIf, "[disable]" + vlanId);
                        vlanBuilder.setVlanOperationalStatus(vlanIf, "disabled");
                    }
                    mplsBuilder.buildVlanConnection(pw, vlanIf, logical);
                    isTaggedVlan = true;
                    break;
                case noChange:
                    throw new IllegalStateException("unknown case.");
                case changeVlan:
                    throw new IllegalStateException("unknown case.");
                case portBased:
                    if (vlanId == 4096) {
                        throw new IllegalStateException("unknown state");
                    } else if (vlanId == 4097) {
                        mplsBuilder.buildDirectConnection(pw, logical);
                    } else {
                        throw new IllegalStateException("unknown state");
                    }
                    isTaggedVlan = false;
                    break;
                case removeVlan:
                    throw new IllegalStateException("unknown case.");
                case other:
                    throw new IllegalStateException("unknown case.");
                case unknown:
                default:
                    throw new IllegalStateException("unknown cpwVcEnetVlanMode: " + vlanMode.getId());
            }

            log.debug("connect: pw=" + pw.getFullyQualifiedName()
                    + "->" + logical.getFullyQualifiedName()
                    + ":vlan" + vlanId
                    + " " + (isTaggedVlan ? "Tagged" : "Untagged") + "[" + vlanMode + "]");
        }
    }

    public final static class VcEnetEntryKey {
        public final BigInteger vcIndex;
        public final BigInteger cpwVcEnetPwVlan;

        public VcEnetEntryKey(BigInteger[] suffix) {
            if (suffix.length != 2) {
                throw new IllegalArgumentException();
            }
            this.vcIndex = suffix[0];
            this.cpwVcEnetPwVlan = suffix[1];
        }

        @Override
        public int hashCode() {
            int seed = this.toString().hashCode();
            return seed * seed + seed + 41;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof VcEnetEntryKey) {
                return this.hashCode() == ((VcEnetEntryKey) o).hashCode();
            }
            return false;
        }

        @Override
        public String toString() {
            String key = "VcEnetEntryKey:" + this.vcIndex.toString() + ":" + this.cpwVcEnetPwVlan.toString();
            return key;
        }
    }

    @SuppressWarnings("serial")
    private final static KeyCreator<VcEnetEntryKey> vcEnetEntryKeyCreator
            = new KeyCreator<VcEnetEntryKey>() {
        public VcEnetEntryKey getKey(BigInteger[] oidSuffix) {
            return new VcEnetEntryKey(oidSuffix);
        }

    };
}