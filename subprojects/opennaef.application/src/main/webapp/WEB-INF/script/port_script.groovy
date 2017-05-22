print "into port_script.groovy >>>>\n"

dir = params.dir
configOperations = context.configOperationSet

configOperations.devices.each() { device ->
    print "DeviceName : " + device.deviceName + "\n"
}