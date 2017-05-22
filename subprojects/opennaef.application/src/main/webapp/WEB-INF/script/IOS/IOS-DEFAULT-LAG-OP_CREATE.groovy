import voss.nms.inventory.script.renderer.groovy.GroovyUtils

print "■■■■■■■■■■■■■■■■running IOS-DEFAULT-LAG-OP_CREATE.groovy■■■■■■■■■■■■■■■■\n"

pw.println('    <layer_block name="LAG 作成" snmptrap="disable">')
if (GroovyUtils.isEmpty(op.lagId.value)) {
    pw.println('        ERROR: op.lagId is empty.')
}
pw.println('      configure terminal')
pw.println('      interface Port-channel' + op.lagId)
pw.println('      exit')

op.lagMemberPorts.each() { memberPort ->
    pw.println('        interface ' + memberPort.fullIfName)
    pw.println('          channel-group ' + op.lagId + ' mode on')
    pw.println('        exit')
}
pw.println('      exit')
pw.println('    </layer_block>')

print "■■■■■■■■■■■■■■■■quit IOS-DEFAULT-LAG-OP_CREATE.groovy■■■■■■■■■■■■■■■■\n"