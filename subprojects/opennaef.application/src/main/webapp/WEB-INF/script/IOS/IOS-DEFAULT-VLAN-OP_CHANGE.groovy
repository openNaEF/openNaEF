import voss.nms.inventory.script.renderer.groovy.GroovyUtils

print "■■■■■■■■■■■■■■■■running IOS-DEFAULT-VLAN-OP_CHANGE.groovy■■■■■■■■■■■■■■■■\n"
vlanName = op.getVlanName()
description = op.description //get(OP_ATTR.DESCRIPTION)
ipAddress = op.getIpAddress()
subnetMask = op.getSubnetMask()
vlanId = op.getVlanId()

print "VlanName : " + (vlanName != null ? "alive object [" + vlanName + "]" : "null object") + "\n"
print "VlanName value : " + (vlanName != null ? vlanName.value : "null") + "\n"
print "VlanName isChanged : " + (vlanName != null ? vlanName.isChanged() : "null") + "\n"
print "description : " + (description != null ? "alive object [" + description + "]" : "null object") + "\n"
print "description value : " + (description != null ? description.value : "null") + "\n"
print "description isChanged : " + (description != null ? description.isChanged() : "null") + "\n"
print "ipAddress : " + (ipAddress != null ? "alive object [" + ipAddress + "]" : "null object") + "\n"
print "ipAddress value : " + (ipAddress != null ? ipAddress.value : "null") + "\n"
print "ipAddress isChanged : " + (ipAddress != null ? ipAddress.isChanged() : "null") + "\n"
print "subnetMask : " + (subnetMask != null ? "alive object [" + subnetMask + "]" : "null object") + "\n"
print "subnetMask value : " + (subnetMask != null ? subnetMask.value : "null") + "\n"
print "subnetMask isChanged : " + (subnetMask != null ? subnetMask.isChanged() : "null") + "\n"
print "vlanId : " + (vlanId != null ? "alive object [" + vlanId + "]" : "null object") + "\n"
print "vlanId value : " + (vlanId != null ? vlanId.value : "null") + "\n"
print "vlanId isChanged : " + (vlanId != null ? vlanId.isChanged() : "null") + "\n"

if ((!GroovyUtils.isNull(vlanName) && vlanName.isChanged()) ||
        (!GroovyUtils.isNull(description) && description.isChanged()) ||
        (!GroovyUtils.isNull(ipAddress) && ipAddress.isChanged()) ||
        (!GroovyUtils.isNull(subnetMask) && subnetMask.isChanged())) {

    pw.println('    <layer_block name="VLAN設定変更" snmptrap="disable">')

    if (!GroovyUtils.isNull(vlanName) && vlanName.isChanged()) {
        if (GroovyUtils.isEmpty(vlanId.value)) {
            pw.println('        XXX: ERROR: op.vlanId is empty.')
        }
        pw.println('      configure terminal')
        pw.println('      vlan ' + vlanId)
        if (vlanName.isChanged()) {
            if (!GroovyUtils.isEmpty(vlanName.value)) {
                pw.println('        name ' + vlanName)
            } else {
                pw.println('        no name')
            }
        }
    }

    pw.println('      configure terminal')
    pw.println('      interface vlan' + op.getVlanId())
    if (!GroovyUtils.isNull(description) && description.isChanged()) {
        if (GroovyUtils.isEmpty(description.value)) {
            pw.println('        no description')
        } else {
            pw.println('        description ' + description)
        }
    }

    if ((!GroovyUtils.isNull(ipAddress) && ipAddress.isChanged()) || (!GroovyUtils.isNull(subnetMask) && subnetMask.isChanged())) {
        if (GroovyUtils.isNull(ipAddress) || (!GroovyUtils.isNull(ipAddress) && GroovyUtils.isEmpty(ipAddress.value))) {
            //ipAddress が null なら subnetMask に関わりなく消す
            pw.println('        no ip address')
        } else if ((!GroovyUtils.isNull(ipAddress) && !GroovyUtils.isEmpty(ipAddress.value)) &&
                (GroovyUtils.isNull(subnetMask) || (!GroovyUtils.isNull(subnetMask) && GroovyUtils.isEmpty(subnetMask.value)))) {
            //ipAddress があるのに subnetMask が null は設定不可
            pw.println('        ERROR: op.subnetMask is empty.')
        } else {
            pw.println('        ip address ' + ipAddress + ' ' + subnetMask)
        }

    }
    pw.println('      exit')
    pw.println('      exit')
    pw.println('    </layer_block>')

}
print "■■■■■■■■■■■■■■■■quit IOS-DEFAULT-VLAN-OP_CHANGE.groovy■■■■■■■■■■■■■■■■\n"