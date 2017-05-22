import voss.nms.inventory.script.renderer.groovy.GroovyUtils

print "■■■■■■■■■■■■■■■■running IOS-DEFAULT-LAG-OP_DELETE.groovy■■■■■■■■■■■■■■■■\n"

pw.println('    <layer_block name="LAG 削除" snmptrap="disable">')
if (GroovyUtils.isEmpty(op.lagId.pre)) {
    pw.println('       ERROR: op.lagId.pre is empty.')
}
pw.println('      configure terminal')
pw.println('      no interface Port-channel' + op.lagId.pre)
pw.println('      exit')

pw.println('    </layer_block>')

print "■■■■■■■■■■■■■■■■running IOS-DEFAULT-LAG-OP_DELETE.groovy■■■■■■■■■■■■■■■■\n"