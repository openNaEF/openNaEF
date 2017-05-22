package voss.nms.inventory.diff.network;

import naef.dto.NaefDto;
import voss.core.server.builder.CommandBuilder;
import voss.model.*;
import voss.nms.inventory.constants.IpAddressModel;
import voss.nms.inventory.diff.DiffOperationType;

import java.util.List;

public interface DiffPolicy {

    void setCalculator(NetworkDiffCalculator calc);

    List<String> getLspRegulationWarnings(MplsTunnel lsp);

    List<String> getLspRegulationErrors(MplsTunnel lsp);

    List<String> getPathRegulationWarnings(LabelSwitchedPathEndPoint path);

    List<String> getPathRegulationErrors(LabelSwitchedPathEndPoint path);

    List<String> getPseudoWireRegulationWarnings(NetworkPseudoWire pw);

    List<String> getPseudoWireRegulationErrors(NetworkPseudoWire pw);

    OnDeleteAction getOnDeleteAction();

    boolean isEdgeAwareDevice(Device device);

    boolean isPrimaryPath(MplsTunnel lsp, LabelSwitchedPathEndPoint path);

    boolean isSecondaryPath(MplsTunnel lsp, LabelSwitchedPathEndPoint path);

    String getDefaultRsvpLspPoolName();

    String getDefaultRsvpLspPathPoolName();

    boolean isPseudoWireStringIdType();

    String getDefaultPseudoWirePoolName();

    String getDefaultVplsPoolName();

    String getDefaultVrfPoolName();

    String getDefaultVlanPoolName();

    String getDefaultIpSubnetPoolName();

    String getDefaultRootIpSubnetAddressName();

    String getDefaultNodePipePoolName();

    String getDefaultFacilityStatus();

    String getFacilityStatus(String oldStatus);

    String getDefaultDeletedFacilityStatus(String status);

    String getRegularName(String rawName);

    String getRegularVendorName(String rawVendorName);

    String getRegularModelName(String rawVendorName);

    String getDefaultNodePurpose(Device device);

    String getPortSwitchPortMode(LogicalEthernetPort port);

    void setExtraAttributes(DiffOperationType opType, CommandBuilder builder, VlanModel discovered, NaefDto onDatabase);

    String getDefaultLocationName(Device device);

    IpAddressModel getIpAddressModel();

    String getVpnName(VlanModel model);
}