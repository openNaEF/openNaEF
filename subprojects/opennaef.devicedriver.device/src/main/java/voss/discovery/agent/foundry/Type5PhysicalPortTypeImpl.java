package voss.discovery.agent.foundry;

import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.iolib.snmp.UnexpectedVarBindException;

@SuppressWarnings("serial")
public class Type5PhysicalPortTypeImpl extends StringSnmpEntry implements PhysicalPortType {

    private int ifIndex;
    private int portIfIndex;
    private int port_number;
    private int slot_number;

    public Type5PhysicalPortTypeImpl(String oid, VarBind varbind) {
        super(oid, varbind);
    }

    public void setup() throws UnexpectedVarBindException, NumberFormatException {

        if (oidSuffix.length != 1) {
            throw new UnexpectedVarBindException(varbind);
        }
        this.ifIndex = getLastOIDIndex().intValue();
        this.portIfIndex = getLastOIDIndex().intValue();
        String[] slotPortName = getValue().split("/");
        if (slotPortName.length == 2) {
            this.slot_number = Integer.parseInt(slotPortName[0]);
            this.port_number = Integer.parseInt(slotPortName[1]);
        } else if (slotPortName.length == 1) {
            this.slot_number = NOT_PRESENT;
            this.port_number = Integer.parseInt(slotPortName[0]);
        } else {
            throw new IllegalStateException("illegal ifname: " + getValue());
        }
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