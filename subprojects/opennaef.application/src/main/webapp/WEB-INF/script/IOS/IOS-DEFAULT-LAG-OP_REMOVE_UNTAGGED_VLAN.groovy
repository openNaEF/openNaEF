import voss.nms.inventory.script.renderer.groovy.GroovyUtils

print "■■■■■■■■■■■■■■■■running IOS-DEFAULT-LAG-OP_REMOVE_UNTAGGED_VLAN.groovy■■■■■■■■■■■■■■■■\n"

lagId = op.getLagId()

pw.println('    <layer_block name="LAG から Untagged VLAN 削除" snmptrap="disable">')
if (GroovyUtils.isEmpty(lagId.value)) {
    pw.println('        ERROR: op.lagId is empty.')
}

pw.println('      configure terminal')
pw.println('      interface Port-channel' + lagId)
pw.println('      default switchport access vlan')
pw.println('      exit')
pw.println('      exit')
pw.println('    </layer_block>')

print "■■■■■■■■■■■■■■■■running IOS-DEFAULT-LAG-OP_REMOVE_UNTAGGED_VLAN.groovy■■■■■■■■■■■■■■■■\n"