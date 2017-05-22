package voss.core.server.naming.naef;

import voss.core.server.database.ATTR;
import voss.core.server.util.ConfigModelUtil;
import voss.model.*;
import voss.model.LogicalEthernetPort.TagChanger;

public class NaefTypeNameFactory {

    public static String getNaefTypeName(VlanModel model) {
        String typeName = null;
        if (model == null) {
            throw new IllegalArgumentException();
        }
        if (Device.class.isInstance(model)) {
            typeName = ATTR.TYPE_NODE;

        } else if (Slot.class.isInstance(model)) {
            typeName = ATTR.TYPE_SLOT;
        } else if (Module.class.isInstance(model)) {
            typeName = ATTR.TYPE_MODULE;

        } else if (EthernetPortsAggregator.class.isInstance(model)) {
            typeName = ATTR.TYPE_LAG_PORT;
        } else if (EthernetProtectionPort.class.isInstance(model)) {
            typeName = ATTR.TYPE_LAG_PORT;
        } else if (DefaultLogicalEthernetPort.class.isInstance(model)) {
            typeName = ATTR.TYPE_ETH_PORT;
        } else if (EthernetPort.class.isInstance(model)) {
            typeName = ATTR.TYPE_ETH_PORT;

        } else if (VlanIf.class.isInstance(model)) {
            typeName = ATTR.TYPE_VLAN_IF;
        } else if (TagChanger.class.isInstance(model)) {
            typeName = ATTR.TYPE_VLAN_SEGMENT_GATEWAY_IF;

        } else if (AtmAPSImpl.class.isInstance(model)) {
            typeName = ATTR.TYPE_ATM_APS_PORT;
        } else if (AtmPort.class.isInstance(model)) {
            typeName = ATTR.TYPE_ATM_PORT;
        } else if (AtmVp.class.isInstance(model)) {
            typeName = ATTR.TYPE_ATM_PVP_IF;
        } else if (AtmPvc.class.isInstance(model)) {
            typeName = ATTR.TYPE_ATM_PVC_IF;

        } else if (POSAPSImpl.class.isInstance(model)) {
            typeName = ATTR.TYPE_POS_APS_PORT;
        } else if (POS.class.isInstance(model)) {
            typeName = ATTR.TYPE_POS_PORT;

        } else if (FrameRelayDLCIEndPoint.class.isInstance(model)) {
            typeName = ATTR.TYPE_FR_PVC_IF;
        } else if (Channel.class.isInstance(model)) {
            typeName = ATTR.TYPE_TDM_SERIAL_PORT;
        } else if (SerialPort.class.isInstance(model)) {
            typeName = ATTR.TYPE_SERIAL_PORT;

        } else if (VplsInstance.class.isInstance(model)) {
            typeName = ATTR.TYPE_VPLS_IF;
        } else if (VrfInstance.class.isInstance(model)) {
            typeName = ATTR.TYPE_VRF_IF;

        } else if (Port.class.isInstance(model)) {
            throw new IllegalArgumentException("Undefined: " + ((Port) model).getFullyQualifiedName());
        }
        if (typeName == null) {
            throw new IllegalArgumentException("Undefined: " + ConfigModelUtil.toName(model));
        } else {
            return typeName;
        }
    }
}