import voss.nms.inventory.script.renderer.groovy.GroovyUtils

print "■■■■■■■■■■■■■■■■running IOS-DEFAULT-LAG-OP_ADD_UNTAGGED_VLAN.groovy■■■■■■■■■■■■■■■■\n"

vlanId = op.getVlanId()
lagId = op.getLagId()

pw.println('    <layer_block name="LAG に Untagged VLAN 設定" snmptrap="disable">')
if (GroovyUtils.isEmpty(vlanId.value)) {
    pw.println('      ERROR: op.vlanId is empty.')
}
if (GroovyUtils.isEmpty(lagId.value)) {
    pw.println('      ERROR: op.lagId is empty.')
}
pw.println('      configure terminal')
pw.println('      interface Port-channel' + lagId)
pw.println('      switchport access vlan ' + vlanId)
pw.println('      exit')
pw.println('      exit')
pw.println('    </layer_block>')

print "■■■■■■■■■■■■■■■■quit IOS-DEFAULT-LAG-OP_ADD_UNTAGGED_VLAN.groovy■■■■■■■■■■■■■■■■\n"