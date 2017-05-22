package voss.discovery.agent.cisco.mib;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.MplsModelBuilder;
import voss.discovery.agent.mib.Direction;
import voss.discovery.agent.mib.PseudoWireMplsType;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.ByteSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.utils.ByteArrayUtil;
import voss.model.MplsVlanDevice;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static voss.discovery.agent.cisco.mib.CiscoIetfPwMplsMib.cpwVcMplsNonTeMappingVcIndex;
import static voss.discovery.agent.cisco.mib.CiscoIetfPwMplsMib.cpwVcMplsType;

public class CiscoIetfPwMplsMibImpl {
    private final static Logger log = LoggerFactory.getLogger(CiscoIetfPwMplsMibImpl.class);
    private final SnmpAccess snmp;
    private final MplsVlanDevice device;
    private final MplsModelBuilder builder;

    public CiscoIetfPwMplsMibImpl(SnmpAccess snmp, MplsVlanDevice device) {
        this.snmp = snmp;
        this.device = device;
        this.builder = new MplsModelBuilder(this.device);
    }

    public MplsVlanDevice getDevice() {
        return this.device;
    }

    public synchronized void getPseudoWireSubLayerInformation()
            throws IOException, AbortedException {
        getPseudoWireMplsType();
        analyzePseudoWireMplsNonTeMappingEntry();

    }

    public void getPseudoWireMplsType() throws IOException, AbortedException {
        try {
            List<ByteSnmpEntry> entries = SnmpUtil.getByteSnmpEntries(
                    snmp, cpwVcMplsType);
            for (ByteSnmpEntry entry : entries) {
                int vcIndex = entry.getLastOIDIndex().intValue();
                int[] bits = SnmpUtil.decodeBitList(entry.getValue());
                System.err.println(ByteArrayUtil.byteArrayToHexString(entry
                        .getValue()));
                List<PseudoWireMplsType> types = new ArrayList<PseudoWireMplsType>();
                for (int i = 0; i < bits.length; i++) {
                    PseudoWireMplsType type = PseudoWireMplsType
                            .getById(bits[i] - 1);
                    types.add(type);
                    log.debug("vcIndex: " + vcIndex + " type=" + type);
                }
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public void analyzePseudoWireMplsNonTeMappingEntry() throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(
                    snmp, cpwVcMplsNonTeMappingVcIndex);
            for (IntSnmpEntry entry : entries) {
                BigInteger[] suffix = entry.oidSuffix;
                assert suffix.length == 4;
                Direction tunnelDirection = Direction.getById(suffix[0].intValue());
                int xcTunnelIndex = suffix[1].intValue();
                int ifindex = suffix[2].intValue();
                int vcIndex = suffix[3].intValue();
                log.trace("cpwVcMplsNonTeMappingTunnelDirection: " + tunnelDirection);
                log.trace("cpwVcMplsNonTeMappingTunnelXcTunnelIndex: " + xcTunnelIndex);
                log.trace("cpwVcMplsNonTeMappingIfIndex: " + ifindex);
                log.trace("cpwVcMplsNonTeMappingVcIndex: " + vcIndex);
            }
        } catch (Exception e) {

        }
    }

}