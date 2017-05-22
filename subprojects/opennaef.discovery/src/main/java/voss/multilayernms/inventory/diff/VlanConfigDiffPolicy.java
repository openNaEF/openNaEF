package voss.multilayernms.inventory.diff;

import naef.dto.NaefDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.config.CoreConfiguration;
import voss.core.server.util.Util;
import voss.discovery.agent.common.Constants;
import voss.discovery.agent.flashwave.FlashWaveExtInfoNames;
import voss.model.*;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.nms.inventory.builder.EthernetEPSCommandBuilder;
import voss.nms.inventory.builder.EthernetLAGCommandBuilder;
import voss.nms.inventory.builder.NodeCommandBuilder;
import voss.nms.inventory.builder.PhysicalPortCommandBuilder;
import voss.nms.inventory.constants.IpAddressModel;
import voss.nms.inventory.constants.SwitchPortMode;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.DiffOperationType;
import voss.nms.inventory.diff.network.*;
import voss.nms.inventory.util.LocationUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VlanConfigDiffPolicy implements DiffPolicy {
    private CoreConfiguration config = null;

    protected CoreConfiguration getConfig() {
        if (this.config == null) {
            this.config = CoreConfiguration.getInstance();
        }
        return this.config;
    }

    @Override
    public void setCalculator(NetworkDiffCalculator calc) {
    }

    @Override
    public List<String> getLspRegulationWarnings(MplsTunnel lsp) {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getLspRegulationErrors(MplsTunnel lsp) {
        List<String> result = new ArrayList<String>();
        return result;
    }

    @Override
    public List<String> getPathRegulationWarnings(LabelSwitchedPathEndPoint path) {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getPathRegulationErrors(LabelSwitchedPathEndPoint path) {
        List<String> errors = new ArrayList<String>();
        if (path.getLspName() == null) {
            int ifIndex = -1;
            try {
                ifIndex = path.getIfIndex();
            } catch (Exception e) {
            }
            errors.add("ERROR: [discovery]path has no name. id=" + path.getLspName() + "[" + ifIndex + "]");
        }
        return errors;
    }

    @Override
    public List<String> getPseudoWireRegulationWarnings(NetworkPseudoWire pw) {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getPseudoWireRegulationErrors(NetworkPseudoWire pw) {
        List<String> errors = new ArrayList<String>();
        if (Util.isNull(pw.getAc1(), pw.getAc2())) {
        }
        if (Util.isNull(pw.getAc1(), pw.getAc2())) {
            errors.add("ERROR: [discovery]pseudowire must have 2 endpoints.");
        } else {
            if (pw.getAc1().getAttachedCircuitPort() == null) {
                errors.add("ERROR: [discovery]no attachment circuit:" + pw.getAc1().getFullyQualifiedName());
            }
            if (pw.getAc2().getAttachedCircuitPort() == null) {
                errors.add("ERROR: [discovery]no attachment circuit:" + pw.getAc2().getFullyQualifiedName());
            }
        }
        return errors;
    }

    @Override
    public OnDeleteAction getOnDeleteAction() {
        return OnDeleteAction.StatusChange;
    }

    @Override
    public boolean isEdgeAwareDevice(Device device) {
        return true;
    }

    @Override
    public boolean isPrimaryPath(MplsTunnel lsp, LabelSwitchedPathEndPoint path) {
        if (path == null || path.getIfName() == null) {
            return false;
        }
        if (isNameBasedRuleApplicable(lsp)) {
            if (hasPrimaryName(path.getLspName())) {
                return true;
            }
        } else {
            if (lsp.getMemberLsps().size() < 1) {
                return false;
            }
            int rank = getNameRank(lsp, path);
            return rank == 0;
        }
        return false;
    }

    @Override
    public boolean isSecondaryPath(MplsTunnel lsp, LabelSwitchedPathEndPoint path) {
        if (path == null || path.getIfName() == null) {
            return false;
        }
        if (isNameBasedRuleApplicable(lsp)) {
            if (hasSecondaryName(path.getLspName())) {
                return true;
            }
        } else {
            if (lsp.getMemberLsps().size() < 2) {
                return false;
            }
            int rank = getNameRank(lsp, path);
            return rank == 1;
        }
        return false;
    }

    private boolean isNameBasedRuleApplicable(MplsTunnel lsp) {
        for (LabelSwitchedPathEndPoint path : lsp.getMemberLsps().values()) {
            boolean primaryOrBackup = hasPrimaryName(path.getLspName()) | hasSecondaryName(path.getLspName());
            if (!primaryOrBackup) {
                return false;
            }
        }
        return true;
    }

    private int getNameRank(MplsTunnel lsp, LabelSwitchedPathEndPoint path) {
        List<String> names = getSortedLspNames(lsp);
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (path.getLspName() == null) {
                continue;
            } else if (path.getLspName().equals(name)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private List<String> getSortedLspNames(MplsTunnel lsp) {
        List<String> names = new ArrayList<String>();
        for (LabelSwitchedPathEndPoint path : lsp.getMemberLsps().values()) {
            names.add(path.getLspName());
        }
        Collections.sort(names);
        return names;
    }

    private boolean hasPrimaryName(String pathName) {
        if (pathName == null) {
            return false;
        }
        pathName = pathName.toLowerCase();
        if (pathName.endsWith("pri")
                || pathName.endsWith("primary")
                || pathName.endsWith("main")) {
            return true;
        }
        return false;
    }

    private boolean hasSecondaryName(String pathName) {
        if (pathName == null) {
            return false;
        }
        pathName = pathName.toLowerCase();
        if (pathName.endsWith("bak")
                || pathName.endsWith("backup")
                || pathName.endsWith("bkup")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isPseudoWireStringIdType() {
        return true;
    }

    @Override
    public String getDefaultPseudoWirePoolName() {
        return "pseudowires";
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
    public String getDefaultIpSubnetPoolName() {
        return "subnets";
    }

    @Override
    public String getDefaultVlanPoolName() {
        return "vlans";
    }

    @Override
    public String getDefaultVplsPoolName() {
        return "vplses";
    }

    @Override
    public String getDefaultVrfPoolName() {
        return "vrfs";
    }

    @Override
    public String getDefaultFacilityStatus() {
        return FacilityStatus.UNKNOWN.getDisplayString();
    }

    @Override
    public String getFacilityStatus(String old) {
        if (old == null) {
            return FacilityStatus.UNKNOWN.getDisplayString();
        }
        FacilityStatus fs = FacilityStatus.getByDisplayString(old);
        if (fs == FacilityStatus.LOST || fs == FacilityStatus.REVOKED) {
            return FacilityStatus.UNKNOWN.getDisplayString();
        } else if (fs == FacilityStatus.RESERVED) {
            return FacilityStatus.UNKNOWN.getDisplayString();
        }
        return old;
    }

    @Override
    public String getDefaultDeletedFacilityStatus(String status) {
        Logger log = log();
        if (status == null) {
            return FacilityStatus.LOST.getDisplayString();
        }
        FacilityStatus fs = FacilityStatus.getByDisplayString(status);
        if (fs == FacilityStatus.LOST || fs == FacilityStatus.REVOKED || fs == FacilityStatus.RESERVED) {
            log.debug("stauts not changed: " + status);
            return status;
        }
        log.debug("stauts changed: " + FacilityStatus.LOST.getDisplayString());
        return FacilityStatus.LOST.getDisplayString();
    }

    @Override
    public String getRegularName(String name) {
        return name;
    }

    @Override
    public String getRegularVendorName(String name) {
        return name;
    }

    @Override
    public String getRegularModelName(String name) {
        return name;
    }

    private Logger log() {
        return LoggerFactory.getLogger(VlanConfigDiffPolicy.class);
    }

    @Override
    public String getDefaultNodePipePoolName() {
        return null;
    }

    @Override
    public String getDefaultNodePurpose(Device device) {
        return null;
    }

    @Override
    public String getPortSwitchPortMode(LogicalEthernetPort port) {
        if (port.getDevice().getVendorName() == null) {
            return null;
        }
        String vendorName = port.getDevice().getVendorName();
        if (vendorName.equals(Constants.VENDOR_FUJITSU)) {
            return getFlashWaveSwitchPortMode(port);
        }
        SwitchPortMode mode = NetworkDiffUtil.getSwitchPortMode(port);
        if (mode == null) {
            return null;
        } else {
            return mode.name();
        }
    }

    private String getFlashWaveSwitchPortMode(LogicalEthernetPort port) {
        String swMode = (String) port.gainConfigurationExtInfo().get(FlashWaveExtInfoNames.CONFIG_ETH_PORT_MODE);
        if (swMode == null) {
            return null;
        } else if (swMode.equals(FlashWaveExtInfoNames.CONFIG_ETH_PORT_MODE_notSet)) {
            return null;
        } else if (swMode.equals(FlashWaveExtInfoNames.CONFIG_ETH_PORT_MODE_portVWAN)) {
            return SwitchPortMode.ACCESS.name();
        } else if (swMode.equals(FlashWaveExtInfoNames.CONFIG_ETH_PORT_MODE_tagVWAN)) {
            return SwitchPortMode.TRUNK.name();
        } else if (swMode.equals(FlashWaveExtInfoNames.CONFIG_ETH_PORT_MODE_tagVWANExt)) {
            return SwitchPortMode.TRUNK.name();
        } else if (swMode.equals(FlashWaveExtInfoNames.CONFIG_ETH_PORT_MODE_vportVWAN)) {
            return SwitchPortMode.TRUNK.name();
        }
        return null;
    }

    @Override
    public void setExtraAttributes(DiffOperationType opType, CommandBuilder builder, VlanModel network, NaefDto db) {
        if (opType == DiffOperationType.REMOVE) {
            return;
        }
        if (network instanceof Device && builder instanceof NodeCommandBuilder) {
            NodeCommandBuilder builder_ = (NodeCommandBuilder) builder;
            Device device = (Device) network;
            builder_.setValue(MPLSNMS_ATTR.PURPOSE, getDefaultNodePurpose(device));
        }
    }

    public boolean isSwitchPortCapableBuilder(CommandBuilder builder) {
        if (builder == null) {
            return false;
        } else if (builder instanceof PhysicalPortCommandBuilder) {
            return true;
        } else if (builder instanceof EthernetEPSCommandBuilder) {
            return true;
        } else if (builder instanceof EthernetLAGCommandBuilder) {
            return true;
        }
        return false;
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
        return IpAddressModel.SUBNET;
    }

    @Override
    public String getVpnName(VlanModel model) {
        return null;
    }
}