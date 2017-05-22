package voss.multilayernms.inventory.nmscore.web.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.config.NmsCoreCommonConfiguration;
import voss.multilayernms.inventory.nmscore.web.ResponseCodeException;
import voss.multilayernms.inventory.nmscore.web.flow.event.Any;
import voss.multilayernms.inventory.nmscore.web.flow.event.Event;
import voss.multilayernms.inventory.nmscore.web.flow.event.EventCondition;
import voss.multilayernms.inventory.nmscore.web.flow.event.ParameterMatch;
import voss.multilayernms.inventory.nmscore.web.flow.state.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Flow {

    static Logger log = LoggerFactory.getLogger(Flow.class);

    private List<Event> events = new ArrayList<Event>();
    private Map<StateId, State> stateMap = new HashMap<StateId, State>();

    public Flow() {

        addState(new InventoryFilteringFieldsState(StateId.inventoryFilteringFields));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryFilteringFields.toString()), StateId.inventoryFilteringFields);

        addState(new InventoryListViewState(StateId.inventoryList));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryList.toString()), StateId.inventoryList);

        addState(new InventoryTopologyViewState(StateId.topologyView));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.topologyView.toString()), StateId.topologyView);

        addState(new InventorySaveLayoutViewState(StateId.saveLayout));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.saveLayout.toString()), StateId.saveLayout);

        addState(new InventoryUpdateState(StateId.inventoryUpdate));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryUpdate.toString()), StateId.inventoryUpdate);

        addState(new InventoryRefreshState(StateId.inventoryRefresh));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryRefresh.toString()), StateId.inventoryRefresh);

        addState(new InventoryRsvpLspGroupingState(StateId.inventoryLSPGrouping));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryLSPGrouping.toString()), StateId.inventoryLSPGrouping);

        addState(new InventoryRsvpLspCancelReservationState(StateId.inventoryCancelReservation));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryCancelReservation.toString()), StateId.inventoryCancelReservation);

        addState(new InventoryRsvpLspRemoveDisposed(StateId.inventoryRemoveDisposedLSP));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryRemoveDisposedLSP.toString()), StateId.inventoryRemoveDisposedLSP);

        addState(new InventoryPortEditDialogState(StateId.inventoryPortEditDialog));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryPortEditDialog.toString()), StateId.inventoryPortEditDialog);
        addState(new InventoryPortEditUpdateState(StateId.inventoryPortEditUpdate));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryPortEditUpdate.toString()), StateId.inventoryPortEditUpdate);

        addState(new InventoryL2LinkDeleteState(StateId.inventoryL2LinkDelete));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryL2LinkDelete.toString()), StateId.inventoryL2LinkDelete);


        addState(new InventoryNodeEditDialogState(StateId.inventoryNodeEditDialog));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryNodeEditDialog.toString()), StateId.inventoryNodeEditDialog);
        addState(new InventoryNodeEditUpdateState(StateId.inventoryNodeEditUpdate));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryNodeEditUpdate.toString()), StateId.inventoryNodeEditUpdate);

        addState(new InventoryGetNodeEditListState(StateId.inventoryGetNodeEditList));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryGetNodeEditList.toString()), StateId.inventoryGetNodeEditList);
        addState(new InventoryNodeBatchProcessState(StateId.inventoryNodeBatchProcess));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryNodeBatchProcess.toString()), StateId.inventoryNodeBatchProcess);

        addState(new InventoryPortEditDialogState(StateId.inventoryVlanStackToPortDialog));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryVlanStackToPortDialog.toString()), StateId.inventoryVlanStackToPortDialog);
        addState(new InventoryVlanStackToPortState(StateId.inventoryVlanStackToPortCreate));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryVlanStackToPortCreate.toString()), StateId.inventoryVlanStackToPortCreate);

        addState(new InventoryVlanUnstackFromPortDialogState(StateId.inventoryVlanUnstackFromPortDialog));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryVlanUnstackFromPortDialog.toString()), StateId.inventoryVlanUnstackFromPortDialog);
        addState(new InventoryVlanUnstackFromPortState(StateId.inventoryVlanUnstackFromPort));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryVlanUnstackFromPort.toString()), StateId.inventoryVlanUnstackFromPort);

        addState(new InventoryLagMemberPortEditUpdateState(StateId.inventoryLagMemberPortEditUpdate));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryLagMemberPortEditUpdate.toString()), StateId.inventoryLagMemberPortEditUpdate);

        addState(new InventoryPortDeleteState(StateId.inventoryPortDelete));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryPortDelete.toString()), StateId.inventoryPortDelete);

        addState(new InventoryLagPortListState(StateId.inventoryLagPortList));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryLagPortList.toString()), StateId.inventoryLagPortList);

        addState(new InventoryVlanPropertyEditDialogState(StateId.inventoryVlanPropertyEditDialog));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryVlanPropertyEditDialog.toString()), StateId.inventoryVlanPropertyEditDialog);
        addState(new InventoryVlanPropertyEditUpdateState(StateId.inventoryVlanPropertyEditUpdate));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryVlanPropertyEditUpdate.toString()), StateId.inventoryVlanPropertyEditUpdate);

        addState(new InventoryGetVlanPoolList(StateId.inventoryGetVlanPoolList));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryGetVlanPoolList.toString()), StateId.inventoryGetVlanPoolList);

        addState(new InventoryVlanIdCreateState(StateId.inventoryVlanIdCreate));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryVlanIdCreate.toString()), StateId.inventoryVlanIdCreate);

        addState(new InventoryGetIpSubnetNamespacesState(StateId.inventoryGetIpSubnetNamespaces));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryGetIpSubnetNamespaces.toString()), StateId.inventoryGetIpSubnetNamespaces);

        addState(new InventoryVlanIdAllocateSubnetAddressState(StateId.inventoryVlanIdAllocateSubnetAddress));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryVlanIdAllocateSubnetAddress.toString()), StateId.inventoryVlanIdAllocateSubnetAddress);

        addState(new InventoryVlanLinkDeleteState(StateId.inventoryVlanLinkDelete));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryVlanLinkDelete.toString()), StateId.inventoryVlanLinkDelete);

        addState(new InventoryGetIpSubnetNamespaceAndAddressPairState(StateId.inventoryGetIpSubnetNamespaceAndAddressPair));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryGetIpSubnetNamespaceAndAddressPair.toString()), StateId.inventoryGetIpSubnetNamespaceAndAddressPair);

        addState(new InventoryGetPortList(StateId.inventoryGetPortList));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryGetPortList.toString()), StateId.inventoryGetPortList);

        addState(new InventoryGetVlanPoolSubnetListState(StateId.inventoryGetVlanPoolSubnetList));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryGetVlanPoolSubnetList.toString()), StateId.inventoryGetVlanPoolSubnetList);
        addState(new InventoryResistVlanAndSubnetAddressState(StateId.inventoryRegistVlanAndSubnetAddress));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryRegistVlanAndSubnetAddress.toString()), StateId.inventoryRegistVlanAndSubnetAddress);
        addState(new InventoryGetIpAddressListState(StateId.inventoryGetIpAddressList));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryGetIpAddressList.toString()), StateId.inventoryGetIpAddressList);
        addState(new InventoryGetGatewayVlanIf(StateId.inventoryGetGatewayVlanIf));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryGetGatewayVlanIf.toString()), StateId.inventoryGetGatewayVlanIf);

        addState(new InventoryIpSubnetNamespaceUpdateState(StateId.inventoryIpSubnetNamespaceUpdate));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryIpSubnetNamespaceUpdate.toString()), StateId.inventoryIpSubnetNamespaceUpdate);

        addState(new InventoryGetRootIpSubnetNamespacesState(StateId.inventoryGetRootIpSubnetNamespaces));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryGetRootIpSubnetNamespaces.toString()), StateId.inventoryGetRootIpSubnetNamespaces);

        addState(new InventoryIpSubnetDeleteState(StateId.inventoryIpSubnetDelete));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryIpSubnetDelete.toString()), StateId.inventoryIpSubnetDelete);

        addState(new InventoryCsvImportState(StateId.inventoryCsvImport));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryCsvImport.toString()), StateId.inventoryCsvImport);

        addState(new InventoryGetAAAUserNameState(StateId.inventoryGetAAAUserName));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryGetAAAUserName.toString()), StateId.inventoryGetAAAUserName);

        addState(new InventoryFWLBState(StateId.inventoryFWLB));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventoryFWLB.toString()), StateId.inventoryFWLB);

        addState(new InventorySystemUserState(StateId.inventorySystemUser));
        addEvent(new ParameterMatch(Operation.COMMAND_NAME, StateId.inventorySystemUser.toString()), StateId.inventorySystemUser);

        addState(new UnknownRequestState(StateId.unknownRequest));
        addEvent(new Any(), StateId.unknownRequest);
    }

    private void addState(State state) {
        stateMap.put(state.getStateId(), state);
    }

    private void addEvent(EventCondition condition, StateId stateId) {
        State state = stateMap.get(stateId);
        if (state == null) {
            throw new IllegalStateException("state " + stateId + " was not found");
        }
        events.add(new Event(condition, state));
    }

    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {
        context.dumpParams();

        checkRedirectorAccess(context);

        for (Event event : events) {
            if (event.isAdaptable(context)) {
                event.execute(context);
                break;
            }
        }
    }

    private void checkRedirectorAccess(FlowContext context) throws ResponseCodeException, IOException {
        String remoteAddress = context.getHttpServletRequest().getRemoteAddr();

        if (!NmsCoreCommonConfiguration.getInstance().getDispatcherIpAddress().contains(remoteAddress)) {
            String message = "Not used redirector.[" + remoteAddress + "]";
            throw new ResponseCodeException(HttpServletResponse.SC_UNAUTHORIZED, message);
        }

    }


}
