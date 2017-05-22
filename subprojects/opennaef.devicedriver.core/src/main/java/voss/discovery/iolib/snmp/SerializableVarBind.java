package voss.discovery.iolib.snmp;

import net.snmp.OidTLV;
import net.snmp.PrimitiveTLV;
import net.snmp.VarBind;

import java.io.Serializable;

public class SerializableVarBind implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String oid;
    private final byte type;
    private final byte[] value;

    public SerializableVarBind(VarBind varbind) {
        this.oid = varbind.getOid().getOidString();
        this.type = varbind.getValue().getType();
        this.value = varbind.getValue().getValue();
    }

    public VarBind getVarBind() {
        OidTLV oid = new OidTLV(this.oid);
        PrimitiveTLV value = PrimitiveTLV.m0ee(this.type, this.value);
        return new VarBind(oid, value);
    }
}