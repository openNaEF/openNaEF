package voss.discovery.agent.foundry;

import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;

@SuppressWarnings("serial")
public class SnIfIndexLookupInterfaceId extends IntSnmpEntry implements PhysicalPortType {

    private int ifIndex;
    private int slot_number;
    private int port_number;
    private int mediaType;

    public SnIfIndexLookupInterfaceId(String rootOidString, VarBind varbind) {
        super(rootOidString, varbind);
    }

    public void setup() {
        assert oidSuffix.length == 1;
        this.ifIndex = oidSuffix[0].intValue();

        this.slot_number = Integer.valueOf(value[value.length - 2] & 0xff);
        this.port_number = Integer.valueOf(value[value.length - 1] & 0xff);
        this.mediaType = Integer.valueOf(value[value.length - 3] & 0xff);
    }

    public int getIfIndex() {
        return ifIndex;
    }

    public int getSlotIndex() {
        return slot_number;
    }

    public int getPortNumber() {
        return port_number;
    }

    public int getMediaType() {
        return mediaType;
    }

    public int getPortIfIndex() {
        return (slot_number - 1) * 64 + port_number;
    }
}