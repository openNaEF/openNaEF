package voss.discovery.agent.alcatel.mib;

import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.alcatel.Alcatel7710SRDiscovery;
import voss.discovery.agent.alcatel.AlcatelExtInfoNames;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.ByteSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.model.MplsTunnel;
import voss.model.MplsVlanDevice;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class TimetraSdpMibImpl implements TimetraSdpMib {
    private static final Logger log = LoggerFactory.getLogger(TimetraSdpMibImpl.class);
    private final SnmpAccess snmp;
    private final MplsVlanDevice device;

    private final TimetraMplsMibImpl timetraMplsMib;

    private final Map<OidKey, ByteSnmpEntry> sdpBindIdMap;

    public TimetraSdpMibImpl(Alcatel7710SRDiscovery discovery) throws IOException, AbortedException {

        this.snmp = discovery.getSnmpAccess();
        this.device = (MplsVlanDevice) discovery.getDevice();

        this.timetraMplsMib = discovery.getTimetraMplsMib();

        sdpBindIdMap = SnmpUtil.getWalkResult(snmp, sdpBindId, byteEntryBuilder, oidKeyCreator);
        log.getClass();
    }

    public String getFarEndIpAddress(int sdpId) throws IOException, AbortedException {
        try {
            return SnmpUtil.getIpAddress(snmp, sdpFarEndIpAddress + "." + sdpId);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public boolean getBindOperControlWord(int svcId) throws IOException, AbortedException {

        Map<OidKey, IntSnmpEntry> sdpBindOperControlWordMap =
                SnmpUtil.getWalkResult(snmp, sdpBindOperControlWord, intEntryBuilder, oidKeyCreator);

        for (OidKey key : sdpBindOperControlWordMap.keySet()) {
            if (key.getInt(0) == svcId) {
                return sdpBindOperControlWordMap.get(key).intValue() == 1;
            }
        }
        throw new IllegalStateException();
    }

    public long getSdpBindOperBandwidth(int svcId) throws IOException, AbortedException {
        Map<OidKey, IntSnmpEntry> sdpBindOperBandwidthMap =
                SnmpUtil.getWalkResult(snmp, sdpBindOperBandwidth, intEntryBuilder, oidKeyCreator);

        for (OidKey key : sdpBindOperBandwidthMap.keySet()) {
            if (key.getInt(0) == svcId) {
                long bandwidth = sdpBindOperBandwidthMap.get(key).longValue();
                bandwidth = bandwidth * 1000L;
                return bandwidth;
            }
        }
        throw new IllegalStateException();
    }

    public int getSdpId(int svcId) throws IOException, AbortedException {
        for (OidKey key : sdpBindIdMap.keySet()) {
            BigInteger bi[] = key.getOID();
            if (bi[0].intValue() == svcId) {
                int sdpId = bi[3].intValue() * 0x100 + bi[4].intValue();
                return sdpId;
            }
        }
        return 0;
    }

    public int getVcId(SnmpAccess snmp, int svcId) throws IOException, AbortedException {
        for (OidKey key : sdpBindIdMap.keySet()) {
            BigInteger bi[] = key.getOID();
            if (bi[0].intValue() == svcId) {
                int vcId = bi[5].intValue() * 0x1000000 + bi[6].intValue() * 0x10000
                        + bi[7].intValue() * 0x100 + bi[8].intValue();
                return vcId;
            }
        }
        return 0;
    }

    public int getVcId(int svcId) throws IOException, AbortedException {
        return getVcId(snmp, svcId);
    }

    public int getSvcId(int sdpId) {
        for (OidKey key : sdpBindIdMap.keySet()) {
            BigInteger bi[] = key.getOID();
            int _sdpId = bi[3].intValue() * 0x100 + bi[4].intValue();
            if (_sdpId == sdpId) {
                return bi[0].intValue();
            }
        }
        return 0;
    }

    public List<Integer> getLspList(int sdpId) throws AbortedException, IOException {
        try {
            byte b[] = SnmpUtil.getByte(snmp, sdpLspList + "." + sdpId);
            return getLspList(b);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public List<Integer> getLspList(byte[] b) throws IOException {
        if (b.length % 4 != 0) {
            throw new IOException();
        }

        List<Integer> lspList = new ArrayList<Integer>();

        for (int i = 0; i < b.length / 4 - 1; i++) {
            int lsp = (b[i * 4] & 0xff) * 0x1000000 + (b[i * 4 + 1] & 0xff) * 0x10000 +
                    (b[i * 4 + 2] & 0xff) * 0x100 + (b[i * 4 + 3] & 0xff);
            lspList.add(lsp);
        }

        return lspList;
    }

    public void setSdpIdToLsp() throws IOException, AbortedException {
        Map<IntegerKey, ByteSnmpEntry> sdpLspListMap =
                SnmpUtil.getWalkResult(snmp, sdpLspList, byteEntryBuilder, integerKeyCreator);

        for (IntegerKey key : sdpLspListMap.keySet()) {
            int sdpId = key.getInt();
            List<Integer> lspList = getLspList(sdpLspListMap.get(key).value);

            for (int lspId : lspList) {
                int svcId = getSvcId(sdpId);
                MplsTunnel tunnel = (MplsTunnel) device.getPortByIfName(timetraMplsMib.getMplsTunnelName(lspId));
                tunnel.gainConfigurationExtInfo().put(AlcatelExtInfoNames.CONFIG_LSP_SDP_ID, Integer.valueOf(sdpId));
                tunnel.gainConfigurationExtInfo().put(AlcatelExtInfoNames.CONFIG_LSP_TUNNEL_ID, Integer.valueOf(lspId));
                if (svcId != 0) {
                    tunnel.gainConfigurationExtInfo().put(AlcatelExtInfoNames.CONFIG_LSP_SERVICE_ID, Integer.valueOf(svcId));
                }
            }
        }
    }

    public List<Integer> getSdpBindPwPeerStatusBits(int sdpId) throws IOException, AbortedException {
        Map<OidKey, ByteSnmpEntry> sdpBindPwPeerStatusBitsMap =
                SnmpUtil.getWalkResult(snmp, sdpBindPwPeerStatusBits, byteEntryBuilder, oidKeyCreator);

        List<Integer> statusList = new ArrayList<Integer>();
        for (OidKey key : sdpBindPwPeerStatusBitsMap.keySet()) {
            if (key.getInt(0) == sdpId) {
                for (int status : SnmpUtil.decodeBitList(sdpBindPwPeerStatusBitsMap.get(key).getValue())) {
                    statusList.add(status);
                }
                break;
            }
        }
        return statusList;
    }
}