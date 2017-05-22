package voss.nms.inventory.builder;

import naef.dto.NodeDto;
import naef.dto.PathHopDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.dto.mpls.RsvpLspHopSeriesIdPoolDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.RsvpLspUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PathCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final RsvpLspHopSeriesDto path;
    private final RsvpLspHopSeriesIdPoolDto pool;
    private String name;
    private String ingressNodeName;
    private List<String> sourcePorts = new ArrayList<String>();
    private boolean pathChanged = false;

    public static final String PATH_HOP = "hop";

    public PathCommandBuilder(String poolName, String editorName) {
        super(RsvpLspHopSeriesDto.class, null, editorName);
        setConstraint(RsvpLspHopSeriesDto.class);
        this.path = null;
        this.pool = RsvpLspUtil.getHopSeriesIdPool(poolName);
        if (this.pool == null) {
            throw new IllegalStateException("illegal pool-name: " + poolName);
        }
        this.name = null;
    }

    public PathCommandBuilder(RsvpLspHopSeriesDto target, String editorName) {
        super(RsvpLspHopSeriesDto.class, target, editorName);
        setConstraint(RsvpLspHopSeriesDto.class);
        if (RsvpLspUtil.getIngressNode(target) == null) {
            throw new IllegalArgumentException("no ingress node. Please select ingress node, first.");
        }
        this.path = target;
        this.pool = target.getIdPool();
        this.name = DtoUtil.getStringOrNull(target, MPLSNMS_ATTR.PATH_NAME);
        this.ingressNodeName = RsvpLspUtil.getIngressNode(target).getName();
        for (PathHopDto hop : this.path.getHops()) {
            IpIfDto src = (IpIfDto) hop.getSrcPort();
            this.sourcePorts.add(src.getAbsoluteName());
        }
    }

    public PathCommandBuilder(RsvpLspHopSeriesDto target, NodeDto ingress, String editorName) {
        super(RsvpLspHopSeriesDto.class, target, editorName);
        setConstraint(RsvpLspHopSeriesDto.class);
        if (ingress == null) {
            throw new IllegalArgumentException("No ingress. Please select ingress node, first");
        }
        this.path = target;
        this.pool = target.getIdPool();
        this.name = DtoUtil.getStringOrNull(target, MPLSNMS_ATTR.PATH_NAME);
        this.ingressNodeName = ingress.getName();
        for (PathHopDto hop : this.path.getHops()) {
            IpIfDto src = (IpIfDto) hop.getSrcPort();
            this.sourcePorts.add(src.getAbsoluteName());
        }
    }

    public PathCommandBuilder(RsvpLspHopSeriesDto target, String ingressNodeName, String editorName) {
        super(RsvpLspHopSeriesDto.class, target, editorName);
        setConstraint(RsvpLspHopSeriesDto.class);
        if (ingressNodeName == null) {
            throw new IllegalArgumentException("No ingress. Please select ingress node, first");
        }
        this.path = target;
        this.pool = target.getIdPool();
        this.name = DtoUtil.getStringOrNull(target, MPLSNMS_ATTR.PATH_NAME);
        this.ingressNodeName = ingressNodeName;
        for (PathHopDto hop : this.path.getHops()) {
            IpIfDto src = (IpIfDto) hop.getSrcPort();
            this.sourcePorts.add(src.getAbsoluteName());
        }
    }

    public PathCommandBuilder(RsvpLspHopSeriesIdPoolDto pool, NodeDto ingress, String editorName) {
        super(RsvpLspHopSeriesDto.class, null, editorName);
        setConstraint(RsvpLspHopSeriesDto.class);
        if (ingress == null) {
            throw new IllegalArgumentException("No ingress. Please select ingress node, first");
        }
        this.path = null;
        this.pool = pool;
        this.ingressNodeName = ingress.getName();
    }

    public void setPathName(String name) {
        if (this.path != null) {
            if (DtoUtil.hasStringValue(path, MPLSNMS_ATTR.PATH_NAME, name)) {
                return;
            }
            setValue(MPLSNMS_ATTR.PATH_NAME, name);
        }
        this.name = name;
    }

    public void setIngressNodeName(NodeDto ingress) {
        setIngressNodeName(ingress.getName());
    }

    public void setIngressNodeName(String ingressNodeName) {
        if (this.ingressNodeName == null && ingressNodeName == null) {
            return;
        } else if (this.ingressNodeName != null && this.ingressNodeName.equals(ingressNodeName)) {
            return;
        }
        recordChange(ATTR.ATTR_PATH_INGRESS, this.ingressNodeName, ingressNodeName);
        this.ingressNodeName = ingressNodeName;
    }

    public void setBandwidth(Long bandwidth) {
        String value = null;
        if (bandwidth != null) {
            value = bandwidth.toString();
        }
        setValue(MPLSNMS_ATTR.BANDWIDTH, value);
    }

    public void setOperationStatus(String status) {
        setValue(MPLSNMS_ATTR.OPER_STATUS, status);
    }

    public void setNote(String note) {
        setValue(MPLSNMS_ATTR.NOTE, note);
    }

    public void setPathHops(List<PortDto> newSourcePorts) {
        if (!isPathChanged(newSourcePorts)) {
            return;
        }
        if (newSourcePorts == null || newSourcePorts.size() == 0) {
            recordChange(PATH_HOP, this.sourcePorts.toString(), null);
            this.sourcePorts.clear();
            this.pathChanged = true;
            return;
        }
        int length = Math.max(this.sourcePorts.size(), newSourcePorts.size());
        for (int i = 0; i < length; i++) {
            String newHop = null;
            if (newSourcePorts.size() > i) {
                PortDto newHop_ = newSourcePorts.get(i);
                newHop = newHop_.getAbsoluteName();
            }
            String oldHop = null;
            if (this.sourcePorts.size() > i) {
                oldHop = this.sourcePorts.get(i);
            }
            recordChange(PATH_HOP + " [" + i + "]", oldHop, newHop);
        }
        this.pathChanged = true;
        this.sourcePorts.clear();
        for (PortDto newSourcePort : newSourcePorts) {
            this.sourcePorts.add(newSourcePort.getAbsoluteName());
        }
    }

    private boolean isPathChanged(List<PortDto> ports) {
        if (ports == null) {
            if (this.sourcePorts.size() > 0) {
                return true;
            } else {
                return false;
            }
        }
        List<PathHopDto> currentHops = this.path.getHops();
        if (currentHops.size() != ports.size()) {
            return true;
        }
        for (int i = 0; i < currentHops.size(); i++) {
            PortDto p1 = ports.get(i);
            PathHopDto hop = currentHops.get(i);
            PortDto p2 = hop.getSrcPort();
            if (!DtoUtil.mvoEquals(p1, p2)) {
                return true;
            }
        }
        return false;
    }

    public void setPathHopsByString(List<String> newSourcePorts) {
        if (!isPathChanged2(newSourcePorts)) {
            return;
        }
        if (newSourcePorts == null || newSourcePorts.size() == 0) {
            recordChange(PATH_HOP, this.sourcePorts.toString(), null);
            this.sourcePorts.clear();
            this.pathChanged = true;
            return;
        }
        int length = Math.max(this.sourcePorts.size(), newSourcePorts.size());
        for (int i = 0; i < length; i++) {
            String newHop = null;
            if (newSourcePorts.size() > i) {
                newHop = newSourcePorts.get(i);
            }
            String oldHop = null;
            if (this.sourcePorts.size() > i) {
                oldHop = this.sourcePorts.get(i);
            }
            recordChange(PATH_HOP + " [" + i + "]", oldHop, newHop);
        }
        this.sourcePorts.clear();
        for (String newSourcePort : newSourcePorts) {
            this.sourcePorts.add(newSourcePort);
        }
        this.pathChanged = true;
    }

    private boolean isPathChanged2(List<String> newHops) {
        if (newHops == null) {
            if (this.sourcePorts.size() > 0) {
                return true;
            } else {
                return false;
            }
        } else if (newHops.size() != this.sourcePorts.size()) {
            return true;
        }
        int length = newHops.size();
        for (int i = 0; i < length; i++) {
            String newHop = newHops.get(i);
            String oldHop = this.sourcePorts.get(i);
            if (!newHop.equals(oldHop)) {
                return true;
            }
        }
        return false;
    }

    public String getAbsolutePathName() {
        return this.pool.getAbsoluteName() + ATTR.NAME_DELIMITER_PRIMARY +
                ATTR.TYPE_ID + ATTR.NAME_DELIMITER_SECONDARY + getPathID();
    }

    public String getPathID() {
        if (this.ingressNodeName != null) {
            return getPathID(this.ingressNodeName, this.name);
        }
        throw new IllegalStateException("no ingress.");
    }

    public static String getPathID(String ingressNodeName, String pathName) {
        return ingressNodeName + ":" + pathName;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        if (this.path != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }

        if (this.path == null) {
            InventoryBuilder.changeContext(cmd, pool);
            InventoryBuilder.buildNetworkIDCreationCommand(cmd,
                    ATTR.NETWORK_TYPE_RSVPLSP_HOP_SERIES,
                    ATTR.ATTR_PATH_ID, getPathID(),
                    ATTR.ATTR_PATH_POOL, pool.getAbsoluteName());
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.ATTR_PATH_INGRESS, ingressNodeName);
            setValue(MPLSNMS_ATTR.PATH_NAME, name);
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
        } else {
            InventoryBuilder.changeContext(cmd, path);
            cmd.addVersionCheckTarget(path);
        }
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, this.attributes);
        if (this.pathChanged) {
            if (this.path != null) {
                InventoryBuilder.buildRemoveHopCommand(cmd);
            }
            if (isChanged(ATTR.ATTR_PATH_INGRESS)) {
                if (this.ingressNodeName == null && this.sourcePorts.size() > 0) {
                    throw new IllegalStateException("ingress node name must be not null.");
                }
                InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.ATTR_PATH_INGRESS, this.ingressNodeName);
            }
            for (String sourcePort : this.sourcePorts) {
                InventoryBuilder.buildAddHopCommand(cmd,
                        ATTR.NETWORK_TYPE_IPSUBNET, sourcePort);
            }
        }
        if (this.path != null && isChanged(MPLSNMS_ATTR.PATH_NAME)) {
            InventoryBuilder.buildNetworkIDRenameCommands(cmd, ATTR.ATTR_PATH_ID, getPathID());
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.PATH_NAME, this.name);
        }
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        if (this.path == null) {
            return BuildResult.FAIL;
        }
        recordChange("Path", path.getName(), "");
        InventoryBuilder.changeContext(cmd, path);
        InventoryBuilder.buildRemoveHopCommand(cmd);
        InventoryBuilder.buildNetworkIDReleaseCommand(cmd, ATTR.ATTR_PATH_ID, ATTR.ATTR_PATH_POOL);
        return BuildResult.SUCCESS;
    }

    public String getObjectType() {
        return DiffObjectType.RSVPLSP_PATH.getCaption();
    }

}