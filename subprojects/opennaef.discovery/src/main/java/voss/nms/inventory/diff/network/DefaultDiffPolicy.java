package voss.nms.inventory.diff.network;

import naef.dto.NaefDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.LocationUtil;
import voss.core.server.util.Util;
import voss.model.*;
import voss.nms.inventory.constants.IpAddressModel;
import voss.nms.inventory.diff.DiffOperationType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultDiffPolicy implements DiffPolicy {

    @Override
    public void setCalculator(NetworkDiffCalculator calc) {
    }

    @Override
    public List<String> getLspRegulationWarnings(MplsTunnel lsp) {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getLspRegulationErrors(MplsTunnel lsp) {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getPathRegulationWarnings(LabelSwitchedPathEndPoint path) {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getPathRegulationErrors(LabelSwitchedPathEndPoint path) {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getPseudoWireRegulationWarnings(NetworkPseudoWire pw) {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getPseudoWireRegulationErrors(NetworkPseudoWire pw) {
        if (Util.isNull(pw.getAc1(), pw.getAc2())) {
            throw new IllegalStateException("pseudowire must have 2 endpoints.");
        }
        return new ArrayList<String>();
    }

    @Override
    public OnDeleteAction getOnDeleteAction() {
        return OnDeleteAction.StatusChange;
    }

    @Override
    public boolean isEdgeAwareDevice(Device device) {
        String vendorName = device.getVendorName();
        if (vendorName == null) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isPrimaryPath(MplsTunnel lsp, LabelSwitchedPathEndPoint path) {
        if (path == null) {
            throw new IllegalArgumentException();
        }
        if (lsp.getMemberLsps().size() == 1) {
            return true;
        }
        if (path.getIfName() != null) {
            String pathName = path.getIfName().toLowerCase();
            if (pathName.endsWith("primary")
                    || pathName.endsWith("pri")
                    || pathName.endsWith("main")) {
                return true;
            }
        }
        Integer minPriority = 0;
        Integer thisPriority = null;
        for (Map.Entry<Integer, LabelSwitchedPathEndPoint> entry : lsp.getMemberLsps().entrySet()) {
            Integer priority = entry.getKey();
            minPriority = Math.min(minPriority, priority);
            LabelSwitchedPathEndPoint ep = entry.getValue();
            if (ep.equals(path)) {
                thisPriority = priority;
            }
        }
        return minPriority == thisPriority;
    }

    @Override
    public boolean isSecondaryPath(MplsTunnel lsp, LabelSwitchedPathEndPoint path) {
        if (path == null) {
            throw new IllegalArgumentException();
        }
        if (lsp.getMemberLsps().size() == 1) {
            return false;
        }
        if (path.getIfName() != null) {
            String pathName = path.getIfName().toLowerCase();
            if (pathName.endsWith("secondary")
                    || pathName.endsWith("sec")
                    || pathName.endsWith("backup")
                    || pathName.endsWith("bkup")
                    || pathName.endsWith("bk")) {
                return true;
            }
        }
        Integer minPriority = 0;
        Integer thisPriority = null;
        for (Map.Entry<Integer, LabelSwitchedPathEndPoint> entry : lsp.getMemberLsps().entrySet()) {
            Integer priority = entry.getKey();
            minPriority = Math.min(minPriority, priority);
            LabelSwitchedPathEndPoint ep = entry.getValue();
            if (ep.equals(path)) {
                thisPriority = priority;
            }
        }
        return minPriority != thisPriority;
    }

    @Override
    public boolean isPseudoWireStringIdType() {
        return false;
    }

    @Override
    public String getDefaultPseudoWirePoolName() {
        return "pseudowires";
    }

    @Override
    public String getDefaultNodePipePoolName() {
        return "nodepipes";
    }

    @Override
    public String getDefaultRsvpLspPoolName() {
        return "rsvplsps";
    }

    @Override
    public String getDefaultRsvpLspPathPoolName() {
        return "hops";
    }

    @Override
    public String getDefaultVplsPoolName() {
        return "vpls_pool";
    }

    @Override
    public String getDefaultVrfPoolName() {
        return "vrf_pool";
    }

    @Override
    public String getDefaultVlanPoolName() {
        return "vlan_pool";
    }

    @Override
    public String getDefaultIpSubnetPoolName() {
        return "links";
    }

    @Override
    public String getDefaultFacilityStatus() {
        return "UNKNOWN";
    }

    @Override
    public String getFacilityStatus(String status) {
        return status;
    }

    @Override
    public String getDefaultDeletedFacilityStatus(String status) {
        return "DELETED";
    }

    @Override
    public String getRegularName(String rawName) {
        return rawName;
    }

    @Override
    public String getRegularVendorName(String rawVendorName) {
        return rawVendorName;
    }

    @Override
    public String getRegularModelName(String rawVendorName) {
        return rawVendorName;
    }

    @Override
    public String getDefaultNodePurpose(Device device) {
        return null;
    }

    @Override
    public String getPortSwitchPortMode(LogicalEthernetPort port) {
        return null;
    }

    @Override
    public void setExtraAttributes(DiffOperationType opType, CommandBuilder builder,
                                   VlanModel discovered, NaefDto onDatabase) {
    }

    @Override
    public String getDefaultLocationName(Device device) {
        return LocationUtil.getRootLocation().getAbsoluteName();
    }

    @Override
    public String getDefaultRootIpSubnetAddressName() {
        return "Global IPv4";
    }

    @Override
    public IpAddressModel getIpAddressModel() {
        return IpAddressModel.IP_IF;
    }

    @Override
    public String getVpnName(VlanModel model) {
        return null;
    }
}