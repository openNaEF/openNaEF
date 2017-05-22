package voss.discovery.iolib.snmp;

import net.snmp.OidTLV;
import net.snmp.SnmpUtils;
import net.snmp.VarBind;

import java.io.Serializable;
import java.math.BigInteger;

public class SnmpEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    public String rootOidStr;
    public BigInteger[] rootOid;
    public BigInteger[] oidSuffix;
    public byte[] value;
    public VarBind varbind;

    public SnmpEntry(String rootOidString, VarBind varbind) {
        this.rootOidStr = rootOidString;
        rootOid = OidTLV.parseOid(rootOidString);

        String oidString = varbind.getOid().getOidString();
        BigInteger[] oid = OidTLV.parseOid(oidString);
        oidSuffix = new BigInteger[oid.length - rootOid.length];
        System.arraycopy(oid, rootOid.length, oidSuffix, 0, oidSuffix.length);

        String valueString = varbind.getValueAsHexString();
        value =
                valueString.equals("null")
                        ? null
                        : SnmpUtils.parseHexExpression(
                        valueString);
        this.varbind = varbind;
    }

    public BigInteger getValueAsBigInteger() {
        return new BigInteger(value);
    }

    public BigInteger getLastOIDIndex() {
        return oidSuffix[oidSuffix.length - 1];
    }

    public String getRootOidStr() {
        return this.rootOidStr;
    }

    public VarBind getVarBind() {
        return this.varbind;
    }
}