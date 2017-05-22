package voss.discovery.iolib.snmp;

import net.snmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.ProgressMonitor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;

public class SnmpAccessImpl implements SnmpAccess {
    private static final Logger log = LoggerFactory.getLogger(SnmpAccessImpl.class);

    private static class KnownOIDException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        KnownOIDException(String oid) {
            super(oid);
        }
    }

    private SnmpClient snmpClient;
    private String communityString;
    private int timeoutInSeconds = 15;
    private int retry = 3;

    private Map<String, VarBind> cachedResponses = new HashMap<String, VarBind>();
    private Map<String, List<VarBind>> cachedWalkResponses = new HashMap<String, List<VarBind>>();

    private final static Logger logger = LoggerFactory.getLogger(SnmpAccessImpl.class);
    private final static Logger snmpLogger = LoggerFactory.getLogger("SnmpAccessLog");
    private String targetIpAddress;
    private ProgressMonitor monitor;

    public SnmpAccessImpl(SnmpClient snmpClient) {
        if (snmpClient == null) {
            throw new IllegalArgumentException();
        }
        this.snmpClient = snmpClient;
        this.targetIpAddress = snmpClient.getSnmpAgentAddress().getAddress().getHostAddress();
        this.monitor = null;
    }

    public SnmpClient getSnmpClient() {
        return this.snmpClient;
    }

    @Override
    public void clearCache() {
        this.cachedResponses.clear();
        this.cachedWalkResponses.clear();
    }

    public void setMonitor(ProgressMonitor monitor) {
        monitor.addAccess(this);
        this.monitor = monitor;
    }

    public ProgressMonitor getMonitor() {
        return this.monitor;
    }

    public void setCommunityString(String community) {
        this.communityString = community;
    }

    public String getCommunityString() {
        return this.communityString;
    }

    @Override
    public void setSnmpTimeoutSeconds(int sec) {
        this.timeoutInSeconds = sec;
        this.snmpClient.setSocketTimeout(sec * 1000);
    }

    @Override
    public int getSnmpTimeoutSeconds() {
        return this.timeoutInSeconds;
    }

    @Override
    public void setSnmpRetry(int retry) {
        this.retry = retry;
        this.snmpClient.setRetry(retry);
    }

    @Override
    public int getSnmpRetry() {
        return this.retry;
    }

    public VarBind get(String oid)
            throws SocketTimeoutException, SocketException, IOException, SnmpResponseException, AbortedException {
        assert monitor != null;
        if (!monitor.isRunning()) {
            logger.info("aborted.");
            throw new AbortedException();
        }
        VarBind responseVarbind = cachedResponses.get(oid);
        if (responseVarbind == null) {
            responseVarbind = snmpClient.snmpGet(OidTLV.getInstance(oid));
            if (responseVarbind != null) {
                snmpLogger.info(varbindToString(responseVarbind));
            }
            cachedResponses.put(oid, responseVarbind);
        }
        return responseVarbind;
    }

    public Map<String, VarBind> multiGet(String[] oids)
            throws SocketTimeoutException, SocketException, IOException, SnmpResponseException, AbortedException {
        assert monitor != null;
        if (!monitor.isRunning()) {
            logger.info("aborted.");
            throw new AbortedException();
        }

        final Map<String, VarBind> result = new HashMap<String, VarBind>();
        List<OidTLV> targets = new ArrayList<OidTLV>();
        for (String oid : oids) {
            VarBind cachedVarbind = cachedResponses.get(oid);
            if (cachedVarbind == null) {
                targets.add(OidTLV.getInstance(oid));
            } else {
                result.put(oid, cachedVarbind);
            }
        }

        SnmpResponse.Get response = snmpClient.snmpGet(targets.toArray(new OidTLV[0]));
        for (OidTLV oid : targets) {
            VarBind varbind = response.getResponseVarBind(oid);
            if (varbind != null) {
                snmpLogger.info(varbindToString(varbind));
            }
            cachedResponses.put(oid.getOidString(), varbind);
            result.put(oid.getOidString(), varbind);
        }

        return Collections.unmodifiableMap(result);
    }

    public VarBind getNextChild(String oid)
            throws AbortedException, SocketTimeoutException, SocketException, IOException,
            SnmpResponseException {

        assert monitor != null;
        if (!monitor.isRunning()) {
            logger.info("aborted.");
            throw new AbortedException();
        }
        VarBind responseVarbind = cachedResponses.get(oid);
        if (responseVarbind == null) {
            responseVarbind = snmpClient.snmpGetNextChild(OidTLV.getInstance(oid));
            if (responseVarbind != null) {
                snmpLogger.info(varbindToString(responseVarbind));
            }
            cachedResponses.put(oid, responseVarbind);
        }
        return responseVarbind;
    }

    public void walk(String oid, final SerializableWalkProcessor walkProcessor)
            throws AbortedException, SocketTimeoutException, SocketException, IOException,
            RepeatedOidException, SnmpResponseException {

        assert monitor != null;
        if (!monitor.isRunning()) {
            logger.debug("aborted.");
            throw new AbortedException();
        }
        VarBind[] cachedResponses =
                cachedWalkResponses.get(oid) == null
                        ? null
                        : cachedWalkResponses.get(oid).toArray(new VarBind[0]);
        if (cachedResponses != null) {
            for (int i = 0; i < cachedResponses.length; i++) {
                walkProcessor.process(cachedResponses[i]);
            }
        } else {
            final List<VarBind> responses = new ArrayList<VarBind>();
            cachedWalkResponses.put(oid, responses);
            final Set<String> knownOIDs = new HashSet<String>();
            try {
                snmpClient.snmpWalk(OidTLV.getInstance(oid), new WalkProcessor() {
                    public void process(final VarBind varbind) {
                        String oid = varbind.getOid().getOidString();
                        if (knownOIDs.contains(oid)) {
                            throw new KnownOIDException(oid);
                        }
                        snmpLogger.info(varbindToString(varbind));
                        responses.add(varbind);
                        walkProcessor.process(varbind);
                    }

                    public void close() {
                        walkProcessor.close();
                    }
                });
            } catch (KnownOIDException koe) {
                System.out.println(
                        snmpClient
                                .getSnmpAgentAddress()
                                .getAddress()
                                .getHostAddress()
                                + ": Known OID: "
                                + koe.getMessage());
            }
        }
    }

    private String varbindToString(VarBind varbind) {
        OidTLV oid = varbind.getOid();
        if (oid == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(Thread.currentThread().getId());
        sb.append(":");
        sb.append(this.targetIpAddress);
        sb.append(":");
        sb.append(Integer.toHexString(this.hashCode()));
        sb.append(":");
        sb.append(Long.toHexString(System.currentTimeMillis()));
        sb.append(":");
        sb.append(oid.getOidString());
        sb.append("(");
        sb.append(Integer.toHexString(varbind.getValue().getType() & 0xff));
        sb.append(")=");
        sb.append(varbind.getValueAsHexString());
        return sb.toString();
    }

    public List<String> getCachedVarbinds() {
        List<String> result = new ArrayList<String>();
        result.add(this.targetIpAddress);
        result.add("");
        List<String> oidKeys = new ArrayList<String>();
        Map<String, VarBind> varbinds = new HashMap<String, VarBind>();
        for (Map.Entry<String, VarBind> entry : cachedResponses.entrySet()) {
            oidKeys.add(entry.getKey());
            varbinds.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, List<VarBind>> entry : cachedWalkResponses.entrySet()) {
            for (VarBind vb : entry.getValue()) {
                if (oidKeys.contains(vb.getOid().getOidString())) {
                    continue;
                }
                oidKeys.add(vb.getOid().getOidString());
                varbinds.put(vb.getOid().getOidString(), vb);
            }
        }

        Collections.sort(oidKeys, new Comparator<String>() {
            public int compare(String s1, String s2) {
                String[] arr1 = s1.split("\\.");
                String[] arr2 = s2.split("\\.");
                int length = Math.min(arr1.length, arr2.length);
                for (int i = 0; i < length; i++) {
                    long id1 = 0;
                    if (arr1[i] != null && arr1[i].length() > 0) {
                        id1 = Long.parseLong(arr1[i]);
                    }
                    long id2 = 0;
                    if (arr2[i] != null && arr2[i].length() > 0) {
                        id2 = Long.parseLong(arr2[i]);
                    }
                    long diff = id1 - id2;
                    if (diff > 0) {
                        return 1;
                    } else if (diff < 0) {
                        return -1;
                    }
                }
                return arr1.length - arr2.length;
            }
        });
        for (String oidKey : oidKeys) {
            VarBind varbind = varbinds.get(oidKey);
            if (varbind == null) {
                log.debug("[BUG?] no varbind found. " + oidKey);
                continue;
            }
            OidTLV oid = varbind.getOid();
            if (oid == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(Long.toHexString(System.currentTimeMillis()));
            sb.append(":");
            sb.append(oid.getOidString());
            sb.append("(");
            sb.append(Integer.toHexString(varbind.getValue().getType() & 0xff));
            sb.append(")=");
            sb.append(varbind.getValueAsHexString());
            result.add(sb.toString());
        }
        return result;
    }

    public void close() {
        snmpClient.close();
    }

    public InetSocketAddress getSnmpAgentAddress() {
        return snmpClient.getSnmpAgentAddress();
    }
}