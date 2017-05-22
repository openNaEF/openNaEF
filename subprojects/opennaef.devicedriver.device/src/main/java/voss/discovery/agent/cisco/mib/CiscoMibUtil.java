package voss.discovery.agent.cisco.mib;

import voss.discovery.iolib.snmp.SnmpUtil.KeyCreator;

import java.math.BigInteger;

public class CiscoMibUtil {

    public static class CiscoModulePortKey {
        public final int slotIndex;
        public final int portIndex;

        public CiscoModulePortKey(BigInteger[] oidSuffix) {
            if (oidSuffix == null || oidSuffix.length != 2) {
                throw new IllegalArgumentException();
            }
            this.slotIndex = oidSuffix[0].intValue();
            this.portIndex = oidSuffix[1].intValue();
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof CiscoModulePortKey) {
                return this.slotIndex == ((CiscoModulePortKey) o).slotIndex
                        && this.portIndex == ((CiscoModulePortKey) o).portIndex;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int i = this.toString().hashCode();
            return i * i + i + 41;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + ":" + slotIndex + "/" + portIndex;
        }
    }

    @SuppressWarnings("serial")
    public final static KeyCreator<CiscoModulePortKey> ciscoModulePortKeyCreator =
            new KeyCreator<CiscoModulePortKey>() {
                public CiscoModulePortKey getKey(BigInteger[] oidSuffix) {
                    return new CiscoModulePortKey(oidSuffix);
                }

            };

}