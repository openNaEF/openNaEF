package voss.discovery.agent.foundry;

import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.UnexpectedVarBindException;

@SuppressWarnings("serial")
public class Type1PhysicalPortTypeImpl extends IntSnmpEntry implements PhysicalPortType {

    private int ifIndex;
    private int portIfIndex;
    private int port_number;
    private int slot_number;

    public Type1PhysicalPortTypeImpl(String oid, VarBind varbind) {
        super(oid, varbind);
    }

    public void setup() throws UnexpectedVarBindException {

        if (oidSuffix.length != 1) {
            throw new UnexpectedVarBindException(varbind);
        }
        this.ifIndex = intValue();
        this.portIfIndex = oidSuffix[oidSuffix.length - 1].intValue();
        this.port_number = portIfIndex % 256;
        this.slot_number = portIfIndex / 256;
    }

    public int getIfIndex() {
        return ifIndex;
    }

    public int getPortIfIndex() {
        return portIfIndex;
    }

    public int getPortNumber() {
        return port_number;
    }

    public int getSlotIndex() {
        return slot_number;
    }
}