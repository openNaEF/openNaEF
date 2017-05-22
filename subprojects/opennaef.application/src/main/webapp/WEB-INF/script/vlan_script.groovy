import voss.nms.inventory.script.renderer.groovy.GetConfigOperationsByDevice
import voss.nms.inventory.script.renderer.groovy.GroovyUtils
import voss.nms.inventory.script.renderer.groovy.TemplateImporter
import voss.nms.inventory.script.renderer.groovy.TemplateSelector

print "■■■■■■■■■■■■■■■■running vlan_script.groovy■■■■■■■■■■■■■■■■\n"
dir = params.dir
configOperations = context.configOperationSet

configOperations.devices.each() { device ->
    print "//////////////////////device ditails//////////////////////" + "\n"
    print "  DeviceName : " + device.deviceName + "\n"
    print "  DeviceTypeName : " + device.deviceTypeName + "\n"
    print "  VendorName : " + device.vendorName + "\n"
    print "  OsTypeName : " + device.osTypeName + "\n"
    print "  OsVersion : " + device.osVersion + "\n"
    print "  VirtualDeviceName : " + device.virtualDeviceName + "\n"

    deviceOpList = GetConfigOperationsByDevice.run(device, configOperations)

    //dump OP
    print "  Device under Operation info:\n"
    deviceOpList.each() { op ->
        print "    --------------------Operation ditails--------------------" + "\n"
        print "    full : " + op.debug(1) + "\n"
        print "    DeviceOsType : " + op.deviceOsType + "\n"
        print "    DeviceOsVersion : " + op.deviceOsVersion + "\n"
        print "    TargetType : " + op.getTargetType().toString() + "\n"
        print "    OperationType : " + op.getOperationType().name() + "\n"
        print "    Select Groovy Script : " + op.deviceOsType + "-" + (op.deviceOsVersion == null ? "DEFAULT" : op.deviceOsVersion) + "-" + op.getTargetType().toString() + "-" + op.getOperationType().name() + ".groovy" + "\n"
        print "    ---------------------------------------------------------" + "\n"

        //デバッグ
        /*if(op.getTargetType().toString() == 'VLAN' && op.getOperationType().name() != 'OP_DELETE'){
            if(op.deviceOsVersion == null && op.deviceOsType == 'vSwitchOS' && op.getOperationType().name() == 'OP_CREATE'){
                TemplateSelector.run(dir, "/" + device.osTypeName , op,
                    GroovyUtils.getEntry("op",op))
            }
        }*/
    }


    pw.println('  <node_block node_name="' + device.deviceName + '" config_order="無関係" port_type="none" dummy_check="disable" node_confirm="disable">')

    //header
    TemplateImporter.run(dir, "/" + device.osTypeName, device.osTypeName + "-header.groovy",
            GroovyUtils.getEntry("device", device), GroovyUtils.getEntry("pw", pw))

    //ALIAS OP_DELETE
    deviceOpList.each() { op ->
        if (op.getTargetType().toString() == 'ALIAS' && op.getOperationType().name() == 'OP_DELETE') {
            TemplateSelector.run(dir, "/" + device.osTypeName, op,
                    GroovyUtils.getEntry("op", op))
        }
    }

    //VLAN OP_CREATE/VLAN OP_CHANGE
    deviceOpList.each() { vif ->
        if (vif.getTargetType().toString() == 'VLAN' && vif.getOperationType().name() != 'OP_DELETE') {
            TemplateSelector.run(dir, "/" + device.osTypeName, vif,
                    GroovyUtils.getEntry("op", vif), GroovyUtils.getEntry("pw", pw))

            //OP_REMOVE_UNTAGGED_VLAN
            vif.removedUntaggedPorts.each() { op ->
                TemplateSelector.run(dir, "/" + device.osTypeName, op,
                        GroovyUtils.getEntry("op", op), GroovyUtils.getEntry("pw", pw))
            }

            //OP_REMOVE_TAGGED_VLAN
            vif.removedTaggedPorts.each() { op ->
                TemplateSelector.run(dir, "/" + device.osTypeName, op,
                        GroovyUtils.getEntry("op", op), GroovyUtils.getEntry("pw", pw))
            }

            //OP_ADD_TAGGED_VLAN
            vif.addedTaggedPorts.each() { op ->
                TemplateSelector.run(dir, "/" + device.osTypeName, op,
                        GroovyUtils.getEntry("op", op), GroovyUtils.getEntry("pw", pw))
            }

            //OP_ADD_UNTAGGED_VLAN
            vif.addedUntaggedPorts.each() { op ->
                TemplateSelector.run(dir, "/" + device.osTypeName, op,
                        GroovyUtils.getEntry("op", op), GroovyUtils.getEntry("pw", pw))
            }
        }
    }

    //LAG/PORT/VLAN_SUBIF/VLAN_IF OP_CREATE
    deviceOpList.each() { op ->
        if (op.getTargetType().toString() == 'LAG' || op.getTargetType().toString() == 'PORT'
                || op.getTargetType().toString() == 'VLAN_SUBIF' || op.getTargetType().toString() == 'VLAN_IF') {
            TemplateSelector.run(dir, "/" + device.osTypeName, op,
                    GroovyUtils.getEntry("op", op))
        }
    }

    //LAG/PORT/VLAN_SUBIF OP_CHANGE
    deviceOpList.each() { op ->
        if ((op.getTargetType().toString() == 'LAG' || op.getTargetType().toString() == 'PORT' || op.getTargetType().toString() == 'VLAN_SUBIF')
                && op.getOperationType().name() == 'OP_CHANGE') {
            TemplateSelector.run(dir, "/" + device.osTypeName, op,
                    GroovyUtils.getEntry("op", op))
        }
    }

    //LAG/PORT/VLAN_SUBIF/VLAN_IF OP_DELETE
    deviceOpList.each() { op ->
        if ((op.getTargetType().toString() == 'LAG' || op.getTargetType().toString() == 'PORT' || op.getTargetType().toString() == 'VLAN_SUBIF' || op.getTargetType().toString() == 'VLAN_IF')
                && op.getOperationType().name() == 'OP_DELETE') {
            TemplateSelector.run(dir, "/" + device.osTypeName, op,
                    GroovyUtils.getEntry("op", op))
        }
    }

    //VLAN OP_DELETE
    deviceOpList.each() { vif ->
        if (vif.getTargetType().toString() == 'VLAN' && vif.getOperationType().name() == 'OP_DELETE') {

            //OP_REMOVE_UNTAGGED_VLAN
            vif.removedUntaggedPorts.each() { op ->
                TemplateSelector.run(dir, "/" + device.osTypeName, op,
                        GroovyUtils.getEntry("op", op), GroovyUtils.getEntry("pw", pw))
            }

            //OP_REMOVE_TAGGED_VLAN
            vif.removedTaggedPorts.each() { op ->
                TemplateSelector.run(dir, "/" + device.osTypeName, op,
                        GroovyUtils.getEntry("op", op), GroovyUtils.getEntry("pw", pw))
            }

            //OP_ADD_TAGGED_VLAN
            vif.addedTaggedPorts.each() { op ->
                TemplateSelector.run(dir, "/" + device.osTypeName, op,
                        GroovyUtils.getEntry("op", op), GroovyUtils.getEntry("pw", pw))
            }

            //OP_ADD_UNTAGGED_VLAN
            vif.addedUntaggedPorts.each() { op ->
                TemplateSelector.run(dir, "/" + device.osTypeName, op,
                        GroovyUtils.getEntry("op", op), GroovyUtils.getEntry("pw", pw))
            }

            TemplateSelector.run(dir, "/" + device.osTypeName, vif,
                    GroovyUtils.getEntry("op", vif), GroovyUtils.getEntry("pw", pw))
        }
    }

    //ALIAS OP_CREATE
    deviceOpList.each() { op ->
        if (op.getTargetType().toString() == 'ALIAS' && op.getOperationType().name() == 'OP_CREATE') {
            TemplateSelector.run(dir, "/" + device.osTypeName, op,
                    GroovyUtils.getEntry("op", op))
        }
    }

    //ALIAS OP_CHANGE
    deviceOpList.each() { op ->
        if (op.getTargetType().toString() == 'ALIAS' && op.getOperationType().name() == 'OP_CHANGE') {
            TemplateSelector.run(dir, "/" + device.osTypeName, op,
                    GroovyUtils.getEntry("op", op))
        }
    }

    //trailer
    deviceOpList.each() { op ->
        TemplateImporter.run(dir, "/" + device.osTypeName, device.osTypeName + "-trailer.groovy",
                GroovyUtils.getEntry("op", op), GroovyUtils.getEntry("pw", pw))
    }

    pw.println("  </node_block>")

    print "/////////////////////////////////////////////////////////" + "\n"
    print "\n"
}

print "■■■■■■■■■■■■■■■■quit vlan_script.groovy■■■■■■■■■■■■■■■■\n"