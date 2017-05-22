package naef.mvo.vlan;

import naef.mvo.AbstractPort;
import tef.skelton.Attribute;
import tef.skelton.Model;

public class VlanAttrs {

    public static final Attribute.SingleEnum<VlanType, Model> ENABLED_NETWORKING_FUNCTION_VLAN
        = new Attribute.SingleEnum<VlanType, Model>("naef.enabled-networking-function.vlan", VlanType.class);

    public static final Attribute.SingleEnum<SwitchPortMode, AbstractPort> SWITCH_PORT_MODE
        = new Attribute.SingleEnum<SwitchPortMode, AbstractPort>("naef.switch-port-mode", SwitchPortMode.class);
}
