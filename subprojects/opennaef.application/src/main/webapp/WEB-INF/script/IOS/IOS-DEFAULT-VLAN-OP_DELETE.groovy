import voss.nms.inventory.script.renderer.groovy.GroovyUtils

print "■■■■■■■■■■■■■■■■running IOS-DEFAULT-VLAN-OP_DELETE.groovy■■■■■■■■■■■■■■■■\n"
vlanId = op.getVlanId()
print "vlanId : " + (vlanId != null ? "alive object [" + vlanId + "]" : "null object") + "\n"
print "vlanId value : " + (vlanId != null ? vlanId.value : "null") + "\n"
print "vlanId isChanged : " + (vlanId != null ? vlanId.isChanged() : "null") + "\n"

pw.println('    <layer_block name="VLAN削除" snmptrap="disable">')
if (GroovyUtils.isEmpty(vlanId.pre)) {
    pw.println('      ERROR: op.vlanId.pre is empty.')
}

pw.println('      configure terminal')
pw.println('      no vlan ' + vlanId.pre)
pw.println('      exit')
pw.println('    </layer_block>')

print "■■■■■■■■■■■■■■■■quit IOS-DEFAULT-VLAN-OP_DELETE.groovy■■■■■■■■■■■■■■■■\n"