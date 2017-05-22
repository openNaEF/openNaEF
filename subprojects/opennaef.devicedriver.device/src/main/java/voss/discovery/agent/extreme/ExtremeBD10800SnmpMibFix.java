package voss.discovery.agent.extreme;


import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper;
import voss.discovery.iolib.snmp.SnmpHelper.IntegerKey;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.VlanDevice;
import voss.model.VlanIf;
import voss.model.VlanIfImpl;

import java.io.IOException;
import java.util.Map;

public class ExtremeBD10800SnmpMibFix extends ExtremeMibImpl {

    public ExtremeBD10800SnmpMibFix(SnmpAccess snmp) {
        super(snmp);
    }

    @Override
    public void createVlanIf(VlanDevice device) throws IOException, AbortedException {
        extremeVlanIfVlanId(device);
        setupTaggedVlan(device);
        setupUntaggedVlan(device);
    }

    private void extremeVlanIfVlanId(VlanDevice device) throws IOException, AbortedException {
        Map<IntegerKey, IntSnmpEntry> extremeVlanIfVlanIds =
                SnmpUtil.getWalkResult(snmp, extremeVlanIfVlanId_OID, SnmpHelper.intEntryBuilder, SnmpHelper.integerKeyCreator);
        Map<IntegerKey, StringSnmpEntry> extremeVlanIfDescrs =
                SnmpUtil.getWalkResult(snmp, extremeVlanIfDescr_OID, SnmpHelper.stringEntryBuilder, SnmpHelper.integerKeyCreator);

        for (IntegerKey key : extremeVlanIfVlanIds.keySet()) {
            IntSnmpEntry vlanIdEntry = extremeVlanIfVlanIds.get(key);
            StringSnmpEntry vlanNameEntry = extremeVlanIfDescrs.get(key);

            VlanIf vlanIf = new VlanIfImpl();
            vlanIf.initDevice(device);
            vlanIf.initVlanIfIndex(key.getInt());
            vlanIf.initVlanId(vlanIdEntry.intValue());
            vlanIf.initIfName(vlanNameEntry.getValue());
            vlanIf.setVlanName(vlanNameEntry.getValue());
        }
    }
}