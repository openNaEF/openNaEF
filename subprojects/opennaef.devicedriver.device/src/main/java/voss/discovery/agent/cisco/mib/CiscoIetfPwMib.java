package voss.discovery.agent.cisco.mib;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import voss.discovery.agent.common.MplsModelBuilder;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.model.MplsVlanDevice;
import voss.model.PseudoWireOperStatus;
import voss.model.PseudoWirePort;
import voss.model.PseudoWireType;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CiscoIetfPwMib {
    private final SnmpAccess snmp;
    private final MplsVlanDevice device;
    private final MplsModelBuilder builder;
    private final Map<Integer, Integer> vcIndexToPwID = new HashMap<Integer, Integer>();

    public static final String cpwVcMib = ".1.3.6.1.4.1.9.10.106";
    public static final String cpwVcObjects = cpwVcMib + ".1";
    public static final String cpwVcMplsMib = ".1.3.6.1.4.1.9.10.107";
    public static final String cpwVcEnetMib = ".1.3.6.1.4.1.9.10.107";

    public static final String cpwVcType = cpwVcObjects + ".2.1.2";
    public static final String cpwVcPeerAddr = cpwVcObjects + ".2.1.9";
    public static final String cpwVcPeerVcId = cpwVcObjects + ".2.1.10";
    public static final String cpwVcName = cpwVcObjects + ".2.1.21";
    public static final String cpwVcDescr = cpwVcObjects + ".2.1.22";
    public static final String cpwVcAdminStatus = cpwVcObjects + ".2.1.25";
    public static final String cpwVcOperStatus = cpwVcObjects + ".2.1.26";

    public CiscoIetfPwMib(SnmpAccess snmp, MplsVlanDevice device) {
        this.snmp = snmp;
        this.device = device;
        this.builder = new MplsModelBuilder(this.device);
    }

    public void getPseudoWires() throws IOException, AbortedException {
        getPseudoWirePeerPwIds();
        getPseudoWireTypes();
        getPseudoWirePeerIpAddresses();
        getPseudoWireDescriptions();
        getPseudoWireNames();
        getPseudoWireAdminStatus();
        getPseudoWireOperStatus();
    }

    public void getPseudoWirePeerPwIds() throws IOException, AbortedException {
        try {
            List<SnmpUtil.IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(
                    snmp, cpwVcPeerVcId);
            for (SnmpUtil.IntSnmpEntry entry : entries) {
                int vcIndex = entry.getLastOIDIndex().intValue();
                int pwID = entry.getValueAsBigInteger().intValue();
                this.vcIndexToPwID.put(vcIndex, pwID);
                PseudoWirePort pw = this.builder.buildPseudoWire(pwID);
                this.builder.setPseudoWireVcIndex(pw, vcIndex);
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public void getPseudoWireTypes() throws IOException, AbortedException {
        try {
            List<SnmpUtil.IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(
                    snmp, cpwVcType);
            for (SnmpUtil.IntSnmpEntry entry : entries) {
                int vcIndex = entry.getLastOIDIndex().intValue();
                PseudoWireType type = PseudoWireType.getById(entry
                        .getValueAsBigInteger().intValue());
                int pwID = vcIndexToPwID.get(vcIndex);
                this.builder.setPseudoWireType(pwID, type);
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public void getPseudoWirePeerIpAddresses() throws IOException, AbortedException {
        try {
            List<SnmpUtil.ByteSnmpEntry> entries = SnmpUtil.getByteSnmpEntries(snmp, cpwVcPeerAddr);
            for (SnmpUtil.ByteSnmpEntry entry : entries) {
                int vcIndex = entry.getLastOIDIndex().intValue();
                InetAddress peerIpAddress =
                        InetAddress.getByAddress(entry.getValue());
                int pwID = vcIndexToPwID.get(vcIndex);
                this.builder.setPseudoWirePeerAddress(pwID, peerIpAddress);
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public void getPseudoWireNames() throws IOException, AbortedException {
        try {
            List<SnmpUtil.StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(snmp, cpwVcName);
            for (SnmpUtil.StringSnmpEntry entry : entries) {
                int vcIndex = entry.getLastOIDIndex().intValue();
                int pwID = vcIndexToPwID.get(vcIndex);
                String name = entry.getValue();
                this.builder.setPseudoWireName(pwID, name);
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public void getPseudoWireDescriptions() throws IOException, AbortedException {
        try {
            List<SnmpUtil.StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(snmp, cpwVcDescr);
            for (SnmpUtil.StringSnmpEntry entry : entries) {
                int vcIndex = entry.getLastOIDIndex().intValue();
                int pwID = vcIndexToPwID.get(vcIndex);
                String descr = entry.getValue();
                this.builder.setPseudoWireDescription(pwID, descr);
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public void getPseudoWireAdminStatus() throws IOException, AbortedException {
        try {
            List<SnmpUtil.IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(
                    snmp, cpwVcAdminStatus);
            for (SnmpUtil.IntSnmpEntry entry : entries) {
                int vcIndex = entry.getLastOIDIndex().intValue();
                int pwID = vcIndexToPwID.get(vcIndex);
                PseudoWireOperStatus status =
                        PseudoWireOperStatus.getById(entry.getValueAsBigInteger().intValue());
                this.builder.setPseudoWireAdminStatus(pwID, status);
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public void getPseudoWireOperStatus() throws IOException, AbortedException {
        try {
            List<SnmpUtil.IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(
                    snmp, cpwVcOperStatus);
            for (SnmpUtil.IntSnmpEntry entry : entries) {
                int vcIndex = entry.getLastOIDIndex().intValue();
                int pwID = vcIndexToPwID.get(vcIndex);
                PseudoWireOperStatus status =
                        PseudoWireOperStatus.getById(entry.getValueAsBigInteger().intValue());
                this.builder.setPseudoWireOperStatus(pwID, status);
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

}