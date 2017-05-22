package voss.multilayernms.inventory.nmscore.web.flow.state;

public enum StateId {

    inventoryFilteringFields,
    inventoryList,

    topologyView,

    inventoryRefresh,
    inventoryUpdate,
    inventoryLSPGrouping,
    inventoryCancelReservation,
    inventoryRemoveDisposedLSP,

    inventoryPortEditDialog,
    inventoryPortEditUpdate,
    inventoryPortDelete,
    inventoryL2LinkDelete,
    inventoryLagPortList,
    inventoryGetPortList,
    inventoryVlanStackToPortDialog,
    inventoryVlanStackToPortCreate,
    inventoryVlanUnstackFromPortDialog,
    inventoryVlanUnstackFromPort,
    inventoryLagMemberPortEditUpdate,
    inventoryGetVlanPoolList,
    inventoryVlanIdCreate,
    inventoryGetIpSubnetNamespaces,
    inventoryVlanIdAllocateSubnetAddress,
    inventoryVlanPropertyEditDialog,
    inventoryVlanPropertyEditUpdate,
    inventoryVlanLinkDelete,
    inventoryNodeEditDialog,
    inventoryNodeEditUpdate,
    inventoryGetNodeEditList,
    inventoryNodeBatchProcess,

    inventoryGetVlanPoolSubnetList,
    inventoryRegistVlanAndSubnetAddress,
    inventoryGetIpAddressList,
    inventoryGetGatewayVlanIf,

    inventoryGetRootIpSubnetNamespaces,
    inventoryIpSubnetNamespaceUpdate,

    inventoryIpSubnetDelete,

    inventoryGetIpSubnetNamespaceAndAddressPair,

    saveLayout,

    inventoryCsvImport,

    inventoryGetAAAUserName,

    inventoryFWLB,

    inventorySystemUser,

    unknownRequest,

}