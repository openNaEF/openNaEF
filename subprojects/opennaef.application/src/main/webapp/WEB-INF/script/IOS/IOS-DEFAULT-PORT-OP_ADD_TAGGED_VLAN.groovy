import voss.nms.inventory.script.renderer.groovy.GroovyUtils

print "■■■■■■■■■■■■■■■■running IOS-DEFAULT-PORT-OP_ADD_TAGGED_VLAN.groovy■■■■■■■■■■■■■■■■\n"

pw.println('    <layer_block name="ポートに Tagged VLAN 追加" snmptrap="disable">')

if (GroovyUtils.isEmpty(op.vlanId.value)) {
    pw.println('        ERROR: op.vlanId is empty.')
}
if (GroovyUtils.isEmpty(op.ifname.value)) {
    pw.println('        ERROR: op.ifname is empty.')
}
if (GroovyUtils.isEmpty(op.fullIfname.value)) {
    pw.println('        ERROR: op.fullIfname is empty.')
}

pw.println('      configure terminal')
pw.println('      interface ' + op.fullIfname)
pw.println('      switchport trunk allowed vlan add ' + op.vlanId)
pw.println('      exit')
pw.println('      exit')

pw.println('    </layer_block>')

print "■■■■■■■■■■■■■■■■running IOS-DEFAULT-PORT-OP_ADD_TAGGED_VLAN.groovy■■■■■■■■■■■■■■■■\n"