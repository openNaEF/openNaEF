package voss.discovery.agent.foundry;

import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.UnexpectedVarBindException;

@SuppressWarnings("serial")
public class VlanType extends IntSnmpEntry {
    private int vlanId;
    private int ifIndex;

    public VlanType(String oid, VarBind varbind) {
        super(oid, varbind);
    }

    public void setup() throws UnexpectedVarBindException {
        if (oidSuffix.length != 2) {
            throw new UnexpectedVarBindException(varbind);
        }
        this.vlanId = intValue();
        this.ifIndex = getLastOIDIndex().intValue();
    }

    public int getVlanID() {
        return this.vlanId;
    }

    public int getIfIndex() {
        return this.ifIndex;
    }
}