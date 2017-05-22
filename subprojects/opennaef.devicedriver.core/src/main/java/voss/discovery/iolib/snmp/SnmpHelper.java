package voss.discovery.iolib.snmp;

import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpUtil.*;

import java.io.Serializable;
import java.math.BigInteger;

public class SnmpHelper implements Serializable {
    private static final long serialVersionUID = 1L;

    public final static KeyCreator<IfIndexKey> ifIndexKeyCreator = new KeyCreator<IfIndexKey>() {
        private static final long serialVersionUID = 1L;

        public IfIndexKey getKey(BigInteger[] oidSuffix) {
            return new IfIndexKey(oidSuffix);
        }
    };

    public static class IfIndexKey extends IntegerKey {
        private static final long serialVersionUID = 2L;

        public IfIndexKey(BigInteger[] oidSuffix) {
            super(oidSuffix);
        }

        public int getIfIndex() {
            return super.getInt();
        }
    }

    public final static KeyCreator<IntegerKey> integerKeyCreator = new KeyCreator<IntegerKey>() {
        private static final long serialVersionUID = 3L;

        public IntegerKey getKey(BigInteger[] oidSuffix) {
            return new IntegerKey(oidSuffix);
        }
    };

    public static class IntegerKey implements Serializable {
        private static final long serialVersionUID = 4L;
        private final BigInteger integer;

        public IntegerKey(BigInteger[] oidSuffix) {
            if (oidSuffix.length != 1) {
                throw new IllegalArgumentException(
                        "too long oid: " + SnmpUtil.bigIntArrayToString(oidSuffix));
            }
            this.integer = oidSuffix[0];
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof IntegerKey) {
                return this.integer.intValue() == ((IntegerKey) o).integer.intValue();
            }
            return false;
        }

        @Override
        public int hashCode() {
            int i = this.toString().hashCode();
            return i * i + i + 41;
        }

        public BigInteger getBigInteger() {
            return this.integer;
        }

        public int getInt() {
            return this.integer.intValue();
        }

        public long getLong() {
            return this.integer.longValue();
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + ":" + this.integer.intValue();
        }
    }

    public final static KeyCreator<OidKey> oidKeyCreator = new KeyCreator<OidKey>() {
        private static final long serialVersionUID = 5L;

        public OidKey getKey(BigInteger[] oidSuffix) {
            return new OidKey(oidSuffix);
        }
    };

    public static class OidKey implements Serializable {
        private static final long serialVersionUID = 8789225989655136159L;
        private final BigInteger[] oid;

        public OidKey(BigInteger[] oidSuffix) {
            this.oid = new BigInteger[oidSuffix.length];
            System.arraycopy(oidSuffix, 0, this.oid, 0, oidSuffix.length);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof OidKey)) {
                return false;
            }
            OidKey other = (OidKey) o;
            if (this.oid.length != other.oid.length) {
                return false;
            }
            for (int i = 0; i < oid.length; i++) {
                if (!this.oid[i].equals(other.oid[i])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int i = this.toString().hashCode();
            return i * i + i + 41;
        }

        public BigInteger[] getOID() {
            BigInteger[] result = new BigInteger[oid.length];
            System.arraycopy(this.oid, 0, result, 0, this.oid.length);
            return result;
        }

        public BigInteger getBigInteger(int index) {
            return this.oid[index];
        }

        public int getInt(int index) {
            return this.oid[index].intValue();
        }

        public long getLong(int index) {
            return this.oid[index].longValue();
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + ":" + SnmpUtil.bigIntArrayToString(this.oid);
        }
    }

    public static final KeyCreator<TwoIntegerKey> twoIntegerKeyCreator = new KeyCreator<TwoIntegerKey>() {
        private static final long serialVersionUID = 6L;

        public TwoIntegerKey getKey(BigInteger[] oidSuffix) {
            return new TwoIntegerKey(oidSuffix);
        }
    };

    public static class TwoIntegerKey implements Serializable {
        private static final long serialVersionUID = 1L;
        private final BigInteger integer1;
        private final BigInteger integer2;

        public TwoIntegerKey(BigInteger[] oidSuffix) {
            if (oidSuffix.length != 2) {
                throw new IllegalArgumentException(
                        "too long oid: " + SnmpUtil.bigIntArrayToString(oidSuffix));
            }
            this.integer1 = oidSuffix[0];
            this.integer2 = oidSuffix[1];
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof TwoIntegerKey) {
                return this.integer1.intValue() == ((TwoIntegerKey) o).integer1.intValue()
                        && this.integer2.intValue() == ((TwoIntegerKey) o).integer2.intValue();
            }
            return false;
        }

        @Override
        public int hashCode() {
            int i = this.toString().hashCode();
            return i * i + i + 41;
        }

        public BigInteger getBigInteger1() {
            return this.integer1;
        }

        public int intValue1() {
            return this.integer1.intValue();
        }

        public long longValue1() {
            return this.integer1.longValue();
        }

        public BigInteger getBigInteger2() {
            return this.integer2;
        }

        public int intValue2() {
            return this.integer2.intValue();
        }

        public long longValue2() {
            return this.integer2.longValue();
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + ":" + this.integer1.intValue() + ":" + this.integer2.intValue();
        }
    }

    public static final EntryBuilder<SnmpEntry> defaultEntryBuilder =
            new EntryBuilder<SnmpEntry>() {
                private static final long serialVersionUID = 7L;

                public SnmpEntry buildEntry(String oid, VarBind varbind) {
                    return new SnmpEntry(oid, varbind);
                }
            };

    public static final EntryBuilder<IntSnmpEntry> intEntryBuilder =
            new EntryBuilder<IntSnmpEntry>() {
                private static final long serialVersionUID = 8L;

                public IntSnmpEntry buildEntry(String oid, VarBind varbind) {
                    return new IntSnmpEntry(oid, varbind);
                }
            };

    public static final EntryBuilder<StringSnmpEntry> stringEntryBuilder =
            new EntryBuilder<StringSnmpEntry>() {
                private static final long serialVersionUID = 9L;

                public StringSnmpEntry buildEntry(String oid, VarBind varbind) {
                    return new StringSnmpEntry(oid, varbind);
                }
            };

    public static final EntryBuilder<ByteSnmpEntry> byteEntryBuilder =
            new EntryBuilder<ByteSnmpEntry>() {
                private static final long serialVersionUID = 10L;

                public ByteSnmpEntry buildEntry(String oid, VarBind varbind) {
                    return new ByteSnmpEntry(oid, varbind);
                }
            };

    public static final EntryBuilder<IpAddressSnmpEntry> ipAddressEntryBuilder =
            new EntryBuilder<IpAddressSnmpEntry>() {
                private static final long serialVersionUID = 11L;

                public IpAddressSnmpEntry buildEntry(String oid, VarBind varbind) {
                    return new IpAddressSnmpEntry(oid, varbind);
                }
            };


}