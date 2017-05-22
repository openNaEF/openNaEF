package voss.nms.inventory.diff.network.diffunitbuilder;

import naef.dto.NodeDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.dto.mpls.RsvpLspHopSeriesIdPoolDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.builder.CommandUtil;
import voss.core.server.builder.LastUpdateCommandBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.RsvpLspUtil;
import voss.core.server.util.Util;
import voss.discovery.agent.alcatel.AlcatelExtInfoNames;
import voss.model.LabelSwitchedPathEndPoint;
import voss.model.MplsTunnel;
import voss.nms.inventory.builder.PathCommandBuilder;
import voss.nms.inventory.builder.RsvpLspCommandBuilder;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.DiffOperationType;
import voss.nms.inventory.diff.DiffSet;
import voss.nms.inventory.diff.DiffUnit;
import voss.nms.inventory.diff.network.DiffConstants;
import voss.nms.inventory.diff.network.DiffPolicy;
import voss.nms.inventory.diff.network.IpAddressDB;
import voss.nms.inventory.diff.network.NetworkDiffUtil;

import java.io.IOException;
import java.util.*;

public class LspDiffUnitBuilder extends DiffUnitBuilderImpl<MplsTunnel, RsvpLspDto> {
    private static final Logger log = LoggerFactory.getLogger(LspDiffUnitBuilder.class);
    private final DiffPolicy policy;
    private final IpAddressDB ipDB;
    private final String userName;
    private final Map<String, PathCommandBuilder> appliedPathBuilder = new HashMap<String, PathCommandBuilder>();
    private final Map<String, RsvpLspHopSeriesDto> knownPath = new HashMap<String, RsvpLspHopSeriesDto>();
    private boolean pathChanged = false;

    public LspDiffUnitBuilder(DiffSet set, DiffPolicy policy,
                              IpAddressDB ipDB,
                              String editorName)
            throws IOException, InventoryException, ExternalServiceException {
        super(set, DiffObjectType.RSVPLSP.getCaption(), DiffConstants.lspDepth, editorName);
        this.policy = policy;
        this.ipDB = ipDB;
        this.userName = editorName;
        RsvpLspHopSeriesIdPoolDto pool = InventoryConnector.getInstance().getRsvpLspHopSeriesIdPool(
                policy.getDefaultRsvpLspPathPoolName());
        for (RsvpLspHopSeriesDto user : pool.getUsers()) {
            this.knownPath.put(user.getName(), user);
        }
        this.ipDB.dumpLink();
    }

    @Override
    protected DiffUnit update(String inventoryID, MplsTunnel networkLsp, RsvpLspDto dbLsp) throws IOException,
            InventoryException, ExternalServiceException {
        this.pathChanged = false;
        boolean primaryExists = false;
        boolean secondaryExists = false;
        log.debug("processing LSP[" + networkLsp.getIfName() + "]...");
        DiffUnit unit = new DiffUnit(DtoUtil.getMvoId(dbLsp).toString(), inventoryID, DiffOperationType.UPDATE);
        RsvpLspCommandBuilder builder = new RsvpLspCommandBuilder(dbLsp, userName);
        builder.setVersionCheck(false);
        String stauts = DtoUtil.getStringOrNull(dbLsp, MPLSNMS_ATTR.FACILITY_STATUS);
        String newStatus = this.policy.getFacilityStatus(stauts);
        builder.setFacilityStatus(newStatus);
        builder.setTerm(NetworkDiffUtil.getLspTerm(networkLsp));
        Object sdpID = networkLsp.gainConfigurationExtInfo().get("SDP-ID");
        if (sdpID != null) {
            builder.setSdpId(sdpID.toString());
        }
        Object tunnelID = networkLsp.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_LSP_TUNNEL_ID);
        if (tunnelID != null) {
            builder.setTunnelId(tunnelID.toString());
        }
        if (networkLsp.getActiveHops() != null) {
            log.debug("- active-path: " + networkLsp.getActiveHops().getLspName());
        } else {
            log.debug("- active-path: N/A");
        }
        List<Integer> keys = new ArrayList<Integer>(networkLsp.getMemberLsps().keySet());
        String ingressNodeName = RsvpLspUtil.getIngressNode(dbLsp).getName();
        Collections.sort(keys);
        for (Integer key : keys) {
            LabelSwitchedPathEndPoint path = networkLsp.getMemberLsps().get(key);
            if (path == null) {
                continue;
            }
            RsvpLspHopSeriesDto dbPath = null;
            if (policy.isPrimaryPath(networkLsp, path)) {
                dbPath = dbLsp.getHopSeries1();
                primaryExists = true;
            } else if (policy.isSecondaryPath(networkLsp, path)) {
                dbPath = dbLsp.getHopSeries2();
                secondaryExists = true;
            } else {
                log.debug("ignored: path[" + path.getFullyQualifiedName() + "] is not primary nor secondary.");
                continue;
            }
            String currentPathName = DtoUtil.getStringOrNull(dbPath, MPLSNMS_ATTR.PATH_NAME);
            PathCommandBuilder pathBuilder = null;
            String pathName = getPathName(networkLsp, path);
            String pathID = PathCommandBuilder.getPathID(ingressNodeName, pathName);
            log.debug("processing path[" + pathID + "]...");
            if (this.appliedPathBuilder.get(pathID) != null) {
                log.debug("path[" + pathID + "] is already built.");
                pathBuilder = this.appliedPathBuilder.get(pathID);
            } else {
                if (Util.equals(currentPathName, pathName)) {
                    NodeDto ingress = RsvpLspUtil.getIngressNode(dbPath);
                    if (ingress == null) {
                        pathBuilder = new PathCommandBuilder(dbPath, ingressNodeName, userName);
                    } else {
                        pathBuilder = new PathCommandBuilder(dbPath, userName);
                    }
                } else {
                    dbPath = this.knownPath.get(pathID);
                    if (dbPath == null) {
                        pathBuilder = new PathCommandBuilder(policy.getDefaultRsvpLspPathPoolName(), userName);
                        pathBuilder.setIngressNodeName(ingressNodeName);
                        pathBuilder.setPathName(pathName);
                    } else {
                        NodeDto ingress = RsvpLspUtil.getIngressNode(dbPath);
                        if (ingress == null) {
                            pathBuilder = new PathCommandBuilder(dbPath, ingressNodeName, userName);
                        } else {
                            pathBuilder = new PathCommandBuilder(dbPath, userName);
                        }
                    }
                }
                setPathAttributes(path, unit, pathBuilder, dbPath);
                this.appliedPathBuilder.put(pathID, pathBuilder);
            }
            if (pathBuilder != null) {
                pathBuilder.setVersionCheck(false);
            }
            setPathToLsp(networkLsp, path, builder, pathBuilder);
        }
        if (!primaryExists) {
            builder.setPrimaryPathHopName(null);
            buildPathDeleteBuilder(unit, dbLsp, dbLsp.getHopSeries1());
        }
        if (!secondaryExists) {
            builder.setSecondaryPathHopName(null);
            buildPathDeleteBuilder(unit, dbLsp, dbLsp.getHopSeries2());
        }
        super.applyExtraAttributes(DiffOperationType.UPDATE, builder, networkLsp, dbLsp);
        BuildResult result = builder.buildCommand();
        if (result == BuildResult.NO_CHANGES) {
            if (!pathChanged) {
                return null;
            } else {
                LastUpdateCommandBuilder builder_ = new LastUpdateCommandBuilder(dbLsp, userName);
                builder_.setVersionCheck(false);
                builder_.buildCommand();
                unit.addBuilder(builder_);
            }
        } else {
            NetworkDiffUtil.setVersionCheckValues(builder);
            unit.addBuilder(builder);
            CommandUtil.logCommands(builder);
        }
        return unit;
    }

    private String getPathName(MplsTunnel networkLsp, LabelSwitchedPathEndPoint path) {
        String pathName = path.getLspName();
        if (pathName == null) {
            pathName = "hops-" + networkLsp.getIfName();
        }
        return pathName;
    }

    @Override
    protected DiffUnit create(String inventoryID, MplsTunnel networkLsp) throws IOException, InventoryException,
            ExternalServiceException {
        this.pathChanged = false;
        if (networkLsp.getIfName() == null) {
            log.warn("lsp has no name: " + networkLsp.getFullyQualifiedName());
            return null;
        }
        DiffUnit unit = new DiffUnit(inventoryID, DiffOperationType.ADD);
        List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
        RsvpLspCommandBuilder builder = new RsvpLspCommandBuilder(
                policy.getDefaultRsvpLspPoolName(),
                networkLsp.getIfName(),
                networkLsp.getDevice().getDeviceName(),
                userName);
        builder.setFacilityStatus(this.policy.getDefaultFacilityStatus());
        builder.setSource(DiffCategory.DISCOVERY.name());
        builder.setTerm(NetworkDiffUtil.getLspTerm(networkLsp));
        Object sdpID = networkLsp.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_LSP_SDP_ID);
        if (sdpID != null) {
            builder.setSdpId(sdpID.toString());
        }
        Object tunnelID = networkLsp.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_LSP_TUNNEL_ID);
        if (tunnelID != null) {
            builder.setTunnelId(tunnelID.toString());
        }
        if (networkLsp.getActiveHops() != null) {
            log.debug("- active-path: " + networkLsp.getActiveHops().getLspName());
        } else {
            log.debug("- active-path: N/A");
        }
        for (Map.Entry<Integer, LabelSwitchedPathEndPoint> entry : networkLsp.getMemberLsps().entrySet()) {
            LabelSwitchedPathEndPoint path = entry.getValue();
            if (path.getLspHops().size() == 0) {
                log.debug("no hops.");
                continue;
            } else if (path.getLspName() == null) {
                log.debug("no path name.");
                continue;
            }
            String ingressNodeName = path.getDevice().getDeviceName();
            String pathName = getPathName(networkLsp, path);
            String pathID = PathCommandBuilder.getPathID(ingressNodeName, pathName);
            log.debug("pathID: " + pathID);
            PathCommandBuilder pathBuilder = null;
            if (this.appliedPathBuilder.get(pathID) != null) {
                log.debug("path[" + pathID + "] is already known.");
                pathBuilder = this.appliedPathBuilder.get(pathID);
            } else {
                RsvpLspHopSeriesDto dbPath = this.knownPath.get(pathID);
                if (dbPath == null) {
                    pathBuilder = new PathCommandBuilder(policy.getDefaultRsvpLspPathPoolName(), userName);
                    pathBuilder.setIngressNodeName(ingressNodeName);
                    pathBuilder.setPathName(pathName);
                    pathBuilder.setSource(DiffCategory.DISCOVERY.name());
                } else {
                    pathBuilder = new PathCommandBuilder(dbPath, ingressNodeName, userName);
                }
                setPathAttributes(path, unit, pathBuilder, dbPath);
                this.appliedPathBuilder.put(pathID, pathBuilder);
            }
            setPathToLsp(networkLsp, path, builder, pathBuilder);
        }
        super.applyExtraAttributes(DiffOperationType.ADD, builder, networkLsp, null);
        BuildResult result = builder.buildCommand();
        if (result == BuildResult.SUCCESS) {
            builders.add(builder);
            CommandUtil.logCommands(builder);
            unit.addBuilder(builder);
        }
        return unit;
    }

    @Override
    protected DiffUnit delete(String inventoryID, RsvpLspDto dbLsp) throws IOException, InventoryException,
            ExternalServiceException {
        this.pathChanged = false;
        DiffUnit unit = new DiffUnit(DtoUtil.getMvoId(dbLsp).toString(), inventoryID, DiffOperationType.REMOVE);
        RsvpLspCommandBuilder builder = new RsvpLspCommandBuilder(dbLsp, userName);
        super.applyExtraAttributes(DiffOperationType.REMOVE, builder, null, dbLsp);
        builder.buildDeleteCommand();
        unit.addBuilder(builder);
        if (dbLsp.getHopSeries1() != null) {
            RsvpLspHopSeriesDto path = dbLsp.getHopSeries1();
            buildPathDeleteBuilder(unit, dbLsp, path);
        }
        if (dbLsp.getHopSeries2() != null) {
            RsvpLspHopSeriesDto path = dbLsp.getHopSeries2();
            buildPathDeleteBuilder(unit, dbLsp, path);
        }
        CommandUtil.logCommands(builder);
        return unit;
    }

    private void buildPathDeleteBuilder(DiffUnit unit, RsvpLspDto dbLsp, RsvpLspHopSeriesDto path) throws IOException,
            InventoryException, ExternalServiceException {
        if (path == null) {
            return;
        }
        if (RsvpLspUtil.isLastUser(path, dbLsp)) {
            PathCommandBuilder builder1 = new PathCommandBuilder(path, userName);
            builder1.buildDeleteCommand();
            unit.addBuilder(builder1);
        }
    }

    private void setPathToLsp(MplsTunnel networkLsp, LabelSwitchedPathEndPoint path,
                              RsvpLspCommandBuilder builder, PathCommandBuilder pathBuilder) {
        if (policy.isPrimaryPath(networkLsp, path)) {
            builder.setPrimaryPathHopName(pathBuilder.getAbsolutePathName());
        } else if (policy.isSecondaryPath(networkLsp, path)) {
            builder.setSecondaryPathHopName(pathBuilder.getAbsolutePathName());
        } else {
            log.warn("ignored: unexpected naming convention on path:"
                    + path.getFullyQualifiedName());
        }
    }

    private void setPathAttributes(LabelSwitchedPathEndPoint path,
                                   DiffUnit unit, PathCommandBuilder pathBuilder, RsvpLspHopSeriesDto dbPath) throws IOException,
            InventoryException, ExternalServiceException {
        List<String> hops = NetworkDiffUtil.getSubnetSeriesFromPathHops(this.ipDB, path);
        pathBuilder.setPathHopsByString(hops);
        pathBuilder.setBandwidth(path.getBandwidth());
        BuildResult result = pathBuilder.buildCommand();
        if (result == BuildResult.NO_CHANGES) {
            return;
        }
        this.pathChanged = true;
        if (dbPath != null) {
            NetworkDiffUtil.setVersionCheckValues(pathBuilder);
        }
        CommandUtil.logCommands(pathBuilder);
        unit.addBuilder(pathBuilder);
    }
}