package voss.discovery.agent.juniper;

import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.ByteSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.KeyCreator;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.MplsVlanDevice;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class JuniperJnxAnatomyMibImpl implements JuniperJnxAnatomyMib {
    private static final Logger log = LoggerFactory.getLogger(JuniperJnxAnatomyMibImpl.class);
    private final SnmpAccess snmp;
    private final Map<String, String> oid2name = new HashMap<String, String>();

    public JuniperJnxAnatomyMibImpl(SnmpAccess snmp, Map<String, String> oid2name) {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        this.snmp = snmp;
        this.oid2name.putAll(oid2name);
    }

    public String getBoxTypeName() throws IOException, AbortedException {
        try {
            String serial = SnmpUtil.getString(snmp, jnxBoxDescr);
            return serial;
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public String getBoxSerialNumber() throws IOException, AbortedException {
        try {
            String serial = SnmpUtil.getString(snmp, jnxBoxSerialNo);
            return serial;
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public void getPhysicalConfiguration(MplsVlanDevice device) throws IOException, AbortedException {
        Map<JnxChassisKey, SnmpEntry> jnxContainersViews = SnmpUtil.getWalkResult(snmp, jnxContainersView, jnxChassisKey);
        Map<JnxChassisKey, SnmpEntry> jnxContainersLevels = SnmpUtil.getWalkResult(snmp, jnxContainersLevel, jnxChassisKey);
        Map<JnxChassisKey, SnmpEntry> jnxContainersWithins = SnmpUtil.getWalkResult(snmp, jnxContainersWithin, jnxChassisKey);
        Map<JnxChassisKey, SnmpEntry> jnxContainersTypes = SnmpUtil.getWalkResult(snmp, jnxContainersType, jnxChassisKey);

        for (JnxChassisKey key : jnxContainersViews.keySet()) {
            IntSnmpEntry view = IntSnmpEntry.getInstance(jnxContainersViews.get(key));
            IntSnmpEntry level = IntSnmpEntry.getInstance(jnxContainersLevels.get(key));
            IntSnmpEntry within = IntSnmpEntry.getInstance(jnxContainersWithins.get(key));
            ByteSnmpEntry type = ByteSnmpEntry.getInstance(jnxContainersTypes.get(key));
            String oid = deleteLastDotZero(SnmpUtil.oidToOidString(type.getValue()));
            String typeName = oid2name.get(oid);

            System.out.println(key.toString());
            System.out.println("\tview=" + view.intValue());
            System.out.println("\tlevel=" + level.intValue());
            System.out.println("\twithin=" + within.intValue());
            System.out.println("\ttype=" + oid);
            System.out.println("\t -> " + typeName);
            log.trace("");
        }

        Map<JnxContentsKey, SnmpEntry> jnxContentsTypes =
                SnmpUtil.getWalkResult(snmp, jnxContentsType, jnxContentsKey);
        Map<JnxContentsKey, SnmpEntry> jnxContentsContainerIndexs =
                SnmpUtil.getWalkResult(snmp, jnxContentsContainerIndex, jnxContentsKey);
        Map<JnxContentsKey, SnmpEntry> jnxContentsDescrs =
                SnmpUtil.getWalkResult(snmp, jnxContentsDescr, jnxContentsKey);

        for (JnxContentsKey key : jnxContentsTypes.keySet()) {
            ByteSnmpEntry type = ByteSnmpEntry.getInstance(jnxContentsTypes.get(key));
            IntSnmpEntry container = IntSnmpEntry.getInstance(jnxContentsContainerIndexs.get(key));
            StringSnmpEntry descr = StringSnmpEntry.getInstance(jnxContentsDescrs.get(key));
            String oid = deleteLastDotZero(SnmpUtil.oidToOidString(type.getValue()));
            String typeName = oid2name.get(oid);

            System.out.println(key.toString());
            System.out.println("\ttype=" + oid);
            System.out.println("\t -> \"" + typeName + "\"");
            System.out.println("\tcontainer=" + container.intValue());
            System.out.println("\tdescr=" + descr.getValue());
        }
    }

    private String deleteLastDotZero(String oid) {
        if (oid.endsWith(".0")) {
            return oid.substring(0, oid.length() - 2);
        }
        return oid;
    }

    public static class JnxChassisKey {
        public BigInteger jnxContainersIndex;

        public JnxChassisKey(BigInteger[] oid) {
            if (oid == null || oid.length != 1) {
                throw new IllegalArgumentException();
            }
            this.jnxContainersIndex = oid[0];
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof JnxChassisKey) {
                return this.jnxContainersIndex.intValue()
                        == ((JnxChassisKey) o).jnxContainersIndex.intValue();
            }
            return false;
        }

        @Override
        public int hashCode() {
            int i = this.jnxContainersIndex.intValue();
            return i * i + i + 41;
        }

        @Override
        public String toString() {
            return "jnxContainersIndex=" + this.jnxContainersIndex.intValue();
        }
    }

    private final KeyCreator<JnxChassisKey> jnxChassisKey
            = new KeyCreator<JnxChassisKey>() {
        private static final long serialVersionUID = 1L;

        public JnxChassisKey getKey(BigInteger[] oid) {
            return new JnxChassisKey(oid);
        }
    };

    public static class JnxContentsKey {
        public final BigInteger jnxContentsContainerIndex;
        public final BigInteger jnxContentsL1Index;
        public final BigInteger jnxContentsL2Index;
        public final BigInteger jnxContentsL3Index;

        public JnxContentsKey(BigInteger[] oid) {
            if (oid == null || oid.length != 4) {
                throw new IllegalArgumentException();
            }
            jnxContentsContainerIndex = oid[0];
            jnxContentsL1Index = oid[1];
            jnxContentsL2Index = oid[2];
            jnxContentsL3Index = oid[3];
        }

        public String getKey() {
            return "JnxContentsKey:"
                    + this.jnxContentsContainerIndex.intValue()
                    + "/" + this.jnxContentsL1Index.intValue()
                    + "/" + this.jnxContentsL2Index.intValue()
                    + "/" + this.jnxContentsL3Index.intValue();
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof JnxContentsKey) {
                return this.getKey().equals(((JnxContentsKey) o).getKey());
            }
            return false;
        }

        @Override
        public int hashCode() {
            int i = this.getKey().hashCode();
            return i * i + i + 41;
        }

        @Override
        public String toString() {
            return getKey();
        }
    }

    private final KeyCreator<JnxContentsKey> jnxContentsKey = new KeyCreator<JnxContentsKey>() {
        private static final long serialVersionUID = 1L;

        public JnxContentsKey getKey(BigInteger[] oidSuffix) {
            return new JnxContentsKey(oidSuffix);
        }

    };

}