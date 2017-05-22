package voss.multilayernms.inventory.redirector;

import jp.iiga.nmt.core.model.*;
import net.phalanx.core.models.*;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;
import voss.multilayernms.inventory.nmscore.web.flow.state.StateId;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;

public class OperationNameResolver {
    private byte[] inputStreamData = null;

    public byte[] getInputStreamData() {
        return inputStreamData;
    }

    @SuppressWarnings("unchecked")
    public String getOperationName(HttpServletRequest req) throws IOException {
        String operationName = req.getParameter(Operation.COMMAND_NAME);

        if (operationName.equals(StateId.inventoryFilteringFields.toString())) {
            String objectType = Operation.getObjectType(req);

            if (objectType.equals(Device.class.getSimpleName())) {
                return "objectFilterNode";
            } else if (objectType.equals(PhysicalEthernetPort.class.getSimpleName())) {
                return "objectFilterPort";
            } else if (objectType.equals(PhysicalLink.class.getSimpleName())) {
                return "objectFilterLink";
            } else if (objectType.equals(LabelSwitchedPath.class.getSimpleName())) {
                return "objectFilterLsp";
            } else if (objectType.equals(PseudoWire.class.getSimpleName())) {
                return "objectFilterPw";
            } else if (objectType.equals(Vpls.class.getSimpleName())) {
                return "objectFilterVpls";
            } else if (objectType.equals(Vrf.class.getSimpleName())) {
                return "objectFilterVrf";
            } else if (objectType.equals(Vlan.class.getSimpleName())) {
                return "objectFilterVlan";
            } else if (objectType.equals(Subnet.class.getSimpleName())) {
                return "objectFilterSubnet";
            } else if (objectType.equals(SubnetIp.class.getSimpleName())) {
                return "objectFilterSunetIp";
            } else if (objectType.equals(VlanLink.class.getSimpleName())) {
                return "objectFilterVlanLink";
            } else if (objectType.equals(CustomerInfo.class.getSimpleName())) {
                return "objectFilterCustomerInfo";
            } else {
                throw new IllegalArgumentException("Forwarder met an unsupported filter request: " + objectType);
            }
        } else if (operationName.equals(StateId.topologyView.toString())) {
            inputStreamData = Operation.getInputData(req);
            String targetClassName = Operation.getQuery(inputStreamData).getTarget().getName();

            if (targetClassName.equals(PhysicalLink.class.getName())) {
                return "showLinkTopology";
            } else if (targetClassName.equals(LabelSwitchedPath.class.getName())) {
                return "showLspTopology";
            } else if (targetClassName.equals(PseudoWire.class.getName())) {
                return "showPwTopology";
            } else if (targetClassName.equals(IDiagram.class.getName())) {
                return "showWholeTopology";
            } else {
                throw new IllegalArgumentException("Forwarder met an unsupported topology request: " + targetClassName);
            }
        } else if (operationName.equals(StateId.inventoryList.toString())) {
            inputStreamData = Operation.getInputData(req);
            String targetClassName = Operation.getQuery(inputStreamData).getTarget().getName();
            if (targetClassName.equals(Device.class.getName())) {
                return "listNode";
            } else if (targetClassName.equals(PhysicalEthernetPort.class.getName())) {
                return "listPort";
            } else if (targetClassName.equals(PhysicalLink.class.getName())) {
                return "listLink";
            } else if (targetClassName.equals(LabelSwitchedPath.class.getName())) {
                return "listLsp";
            } else if (targetClassName.equals(PseudoWire.class.getName())) {
                return "listPw";
            } else if (targetClassName.equals(Vpls.class.getName())) {
                return "listVpls";
            } else if (targetClassName.equals(Vrf.class.getName())) {
                return "listVrf";
            } else if (targetClassName.equals(Vlan.class.getName())) {
                return "listVlan";
            } else if (targetClassName.equals(Subnet.class.getName())) {
                return "listSubnet";
            } else if (targetClassName.equals(SubnetIp.class.getName())) {
                return "listSubnetIp";
            } else if (targetClassName.equals(VlanLink.class.getName())) {
                return "listVlanLink";
            } else if (targetClassName.equals(CustomerInfo.class.getName())) {
                return "listCustomerInfo";
            } else {
                throw new IllegalArgumentException("Forwarder met an unsupported list request: " + targetClassName);
            }
        } else if (operationName.equals(StateId.inventoryRefresh.toString())) {
            inputStreamData = Operation.getInputData(req);
            String targetClassName = ((Collection<? extends IModel>) Operation.getTargets(inputStreamData)).iterator().next().getClass().getName();
            if (targetClassName.equals(PhysicalEthernetPort.class.getName())) {
                return "updatePortStatus";
            } else if (targetClassName.equals(PhysicalLink.class.getName())) {
                return "updateLinkStatus";
            } else if (targetClassName.equals(LabelSwitchedPath.class.getName())) {
                return "updateLspStatus";
            } else if (targetClassName.equals(PseudoWire.class.getName())) {
                return "updatePwStatus";
            } else if (targetClassName.equals(Vpls.class.getName())) {
                return "updateVplsStatus";
            } else if (targetClassName.equals(Vrf.class.getName())) {
                return "updateVrfStatus";
            } else {
                throw new IllegalArgumentException("Forwarder met an unsupported refresh request: " + targetClassName);
            }
        } else if (operationName.equals(StateId.inventoryUpdate.toString())) {
            inputStreamData = Operation.getInputData(req);
            String targetClassName = ((Collection<? extends IModel>) Operation.getTargets(inputStreamData)).iterator().next().getClass().getName();

            if (targetClassName.equals(LabelSwitchedPath.class.getName())) {
                return "saveLsp";
            } else if (targetClassName.equals(PseudoWire.class.getName())) {
                return "savePw";
            } else if (targetClassName.equals(Vpls.class.getName())) {
                return "saveVpls";
            } else if (targetClassName.equals(Vrf.class.getName())) {
                return "saveVrf";
            } else {
                throw new IllegalArgumentException("Forwarder met an unsupported update request: " + targetClassName);
            }
        } else if (operationName.equals(StateId.inventoryLSPGrouping.toString())) {
            inputStreamData = Operation.getInputData(req);
            return "pairingLsps";
        } else if (operationName.equals(StateId.inventoryCancelReservation.toString())) {
            inputStreamData = Operation.getInputData(req);
            return "deleteReservedLsp";
        } else if (operationName.equals(StateId.inventoryRemoveDisposedLSP.toString())) {
            inputStreamData = Operation.getInputData(req);
            return "deleteLsp";
        } else {
            return operationName;
        }
    }

}