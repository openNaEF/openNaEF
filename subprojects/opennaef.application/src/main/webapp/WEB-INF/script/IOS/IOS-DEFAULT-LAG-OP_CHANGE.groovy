import voss.nms.inventory.script.renderer.groovy.GroovyUtils

print "■■■■■■■■■■■■■■■■running IOS-DEFAULT-LAG-OP_CHANGE.groovy■■■■■■■■■■■■■■■■\n"

pw.println('    <layer_block name="LAG 設定変更" snmptrap="disable">')

if (GroovyUtils.isEmpty(op.lagId.value)) {
    pw.println('        ERROR: op.lagId is empty.')
}
pw.println('      configure terminal')
pw.println('      interface Port-channel' + op.lagId)

//ポート shut / no shut 設定-
if (op.isChanged('adminStatus')) {
    if (op.adminStatus == 'enabled') { //adminStatus が enabledに変更された場合
        pw.println('          no shutdown')
    } else if (op.adminStatus == 'disabled') { //operStatus が disabledに変更された場合
        pw.println('          shutdown')
    } else {
        pw.println('          ERROR: unsupported adminStatus type ' + op.adminStatus + ' .')
    }
}

//description設定
if (op.isChanged('description') && !GroovyUtils.isEmpty(op.description.value)) {
    pw.println('        description ' + op.description)
}
if (op.isChanged('description') && GroovyUtils.isEmpty(op.description.value)) {
    pw.println('        no description')
}
pw.println('      exit')

//メンバポート追加
op.LagAddedMembers.each() { memberPort ->
    pw.println('        interface ' + memberPort.fullIfName)
    pw.println('          channel-group ' + op.lagId + ' mode on')
    pw.println('        exit')
}

//メンバポート削除
op.LagRemovedMembers.each() { memberPort ->
    pw.println('        interface ' + memberPort.fullIfName)
    pw.println('          no channel-group ' + op.lagId + ' mode on')
    pw.println('        exit')
}

pw.println('     exit')
pw.println('    </layer_block>')


print "■■■■■■■■■■■■■■■■quit IOS-DEFAULT-LAG-OP_CHANGE.groovy■■■■■■■■■■■■■■■■\n"