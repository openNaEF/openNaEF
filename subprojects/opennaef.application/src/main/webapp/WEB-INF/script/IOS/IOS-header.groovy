print "■■■■■■■■■■■■■■■■running IOS-header.groovy■■■■■■■■■■■■■■■■\n"
pw.println('    <layer_block name="特権モード移行" snmptrap="disable">')
pw.println('      enable')
//pw.println('    <!--call_check name="装置名確認">')
//pw.println('      <argument name="deviceName">' + device.deviceName + '</argument>')
//pw.println('    </call_check>')
//pw.println('    <call_check name="OSバージョン確認">')
//pw.println('      <argument name="version">' + device.osVersion + '</argument>')
//pw.println('    </call_check-->')
pw.println('    </layer_block>')
print "■■■■■■■■■■■■■■■■quit IOS-header.groovy■■■■■■■■■■■■■■■■\n"
