import voss.nms.inventory.script.renderer.groovy.GroovyUtils

print "■■■■■■■■■■■■■■■■running IOS-DEFAULT-LAG-OP_ADD_TAGGED_VLAN.groovy■■■■■■■■■■■■■■■■\n"

vlanId = op.getVlanId()
lagId = op.getLagId()
lagMemberPorts = op.getLagMemberPorts()

pw.println('    <layer_block name="LAG に Tagged VLAN 追加" snmptrap="disable">')
if (GroovyUtils.isEmpty(vlanId.value)) {
    pw.println('      ERROR: op.vlanId is empty.')
}
if (GroovyUtils.isEmpty(lagId.value)) {
    pw.println('      ERROR: op.lagId is empty.')
}

pw.println('      <set_var var_name="vlanId" value="' + vlanId + '" scope="layer_block"/>')
pw.println('      <set_var var_name="lagId" value="' + lagId + '" scope="layer_block"/>')

showEtherchannelText = lagId + ' Po' + lagId + '\(SU\) (-|PAgP|LACP)'
lagMemberPorts.each() { memberPort ->
    showEtherchannelText = showEtherchannelText + ' ' + memberPort.ifname + '\(P\)'
}

pw.println('      configure terminal')
pw.println('      interface Port-channel' + lagId)
pw.println('      switchport trunk allowed vlan add ' + vlanId)
pw.println('      exit')
pw.println('      exit')
pw.println('    </layer_block>')

print "■■■■■■■■■■■■■■■■quit IOS-DEFAULT-LAG-OP_ADD_TAGGED_VLAN.groovy■■■■■■■■■■■■■■■■\n"