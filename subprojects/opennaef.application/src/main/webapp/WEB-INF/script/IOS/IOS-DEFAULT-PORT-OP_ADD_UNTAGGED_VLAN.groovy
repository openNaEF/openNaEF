import voss.nms.inventory.script.renderer.groovy.GroovyUtils

print "■■■■■■■■■■■■■■■■running IOS-DEFAULT-PORT-OP_ADD_UNTAGGED_VLAN.groovy■■■■■■■■■■■■■■■■\n"

pw.println('    <layer_block name="ポートに Untagged VLAN 設定" snmptrap="disable">')

if (GroovyUtils.isEmpty(op.vlanId.value)) {
    pw.println('        ERROR: op.vlanId is empty.')
}
if (GroovyUtils.isEmpty(op.ifname.value)) {
    pw.println('        ERROR: op.ifname is empty.')
}
if (GroovyUtils.isEmpty(op.fullIfname.value)) {
    pw.println('        ERROR: op.fullIfname is empty.')
}

pw.println('      <set_var var_name="uniIfname" value="' + op.ifname + '" scope="layer_block"/>')
pw.println('      <set_var var_name="vlanId" value="' + op.vlanId + '" scope="layer_block"/>')

pw.println('      configure terminal')
pw.println('      interface ' + op.fullIfname)
pw.println('      switchport access vlan ' + op.vlanId)
pw.println('      exit')
pw.println('      exit')

pw.println('    </layer_block>')

print "■■■■■■■■■■■■■■■■running IOS-DEFAULT-PORT-OP_ADD_UNTAGGED_VLAN.groovy■■■■■■■■■■■■■■■■\n"