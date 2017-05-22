print "■■■■■■■■■■■■■■■■running vSwitchOS-DEFAULT-VLAN-OP_CREATE.groovy■■■■■■■■■■■■■■■■\n"

print "<layer_block name='VLAN作成' snmptrap='disable'>\n"
if (op.vlanId.value == null) {
    print "  ERROR: op.vlanId is empty.\n"
}
if (op.vlanName.value == null) {
    print "  ERROR: op.vlanName is empty.\n"
}
print "/usr/sbin/esxcfg-vswitch --add-pg=" + op.ConfigName + " " + op.virtualDeviceName + "\n"
print "/usr/sbin/esxcfg-vswitch --pg=" + op.ConfigName + " --vlan=" + op.vlanId + " " + op.virtualDeviceName + "\n"
print "</layer_block>\n"

print "■■■■■■■■■■■■■■■■quit vSwitchOS-DEFAULT-VLAN-OP_CREATE.groovy■■■■■■■■■■■■■■■■\n"