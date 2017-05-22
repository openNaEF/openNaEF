import voss.nms.inventory.script.renderer.groovy.GroovyUtils

print "■■■■■■■■■■■■■■■■running IOS-DEFAULT-VLAN-OP_CREATE.groovy■■■■■■■■■■■■■■■■\n"
vlanId = op.getVlanId()
print "vlanId : " + (vlanId != null ? "alive object [" + vlanId + "]" : "null object") + "\n"
print "vlanId value : " + (vlanId != null ? vlanId.value : "null") + "\n"
print "vlanId isChanged : " + (vlanId != null ? vlanId.isChanged() : "null") + "\n"


pw.println('    <layer_block name="VLAN作成" snmptrap="disable">')
if (GroovyUtils.isEmpty(vlanId)) {
    pw.println('        XXX: ERROR: op.vlanId is empty.')
}
pw.println('      <set_var var_name="vlanId" value="' + vlanId + '" scope="layer_block"/>')
pw.println('      configure terminal')
pw.println('      no spanning-tree vlan ' + vlanId)
pw.println('      vlan ' + vlanId)

pw.println('      exit')
pw.println('      exit')
pw.println('    </layer_block>')

print "■■■■■■■■■■■■■■■■quit IOS-DEFAULT-VLAN-OP_CREATE.groovy■■■■■■■■■■■■■■■■\n"