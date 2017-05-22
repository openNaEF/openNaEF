package voss.nms.inventory.builder;

import naef.dto.InterconnectionIfDto;
import naef.dto.PortDto;
import voss.core.server.builder.*;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.Comparators;

import java.io.IOException;
import java.util.*;

public class TextBasedNodePipeCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final InterconnectionIfDto target;
    private String nodeName = null;
    private String ifName = null;
    private String ac1AbsoluteName = null;
    private String ac1NodeIfName = null;
    private String ac2AbsoluteName = null;
    private String ac2NodeIfName = null;
    private String originalAc1AbsoluteName = null;
    private String originalAc2AbsoluteName = null;

    public TextBasedNodePipeCommandBuilder(InterconnectionIfDto target, String editorName) {
        super(InterconnectionIfDto.class, target, editorName);
        setConstraint(InterconnectionIfDto.class);
        this.target = target;
        if (target == null) {
            throw new IllegalStateException();
        }
        this.nodeName = target.getNode().getName();
        this.ifName = DtoUtil.getStringOrNull(this.target, MPLSNMS_ATTR.IFNAME);
        List<PortDto> ports = new ArrayList<PortDto>(target.getAttachedPorts());
        if (ports.size() != 2) {
            throw new IllegalArgumentException("illegal number of attachment port: " + ports.size());
        }
        Collections.sort(ports, Comparators.getIfNameBasedPortComparator());
        Iterator<PortDto> it = ports.iterator();
        PortDto port1 = it.next();
        this.ac1AbsoluteName = getAbsoluteName(port1);
        this.originalAc1AbsoluteName = this.ac1AbsoluteName;
        this.ac1NodeIfName = getNodeIfName(port1);
        PortDto port2 = it.next();
        this.ac2AbsoluteName = getAbsoluteName(port2);
        this.ac2NodeIfName = getNodeIfName(port2);
        this.originalAc2AbsoluteName = this.ac2AbsoluteName;
    }

    public TextBasedNodePipeCommandBuilder(String nodeName, String editorName) {
        super(InterconnectionIfDto.class, null, editorName);
        setConstraint(InterconnectionIfDto.class);
        this.target = null;
        this.nodeName = BuilderUtil.getNodeName(nodeName);
    }

    public void setPipeName(String name) {
        setValue(MPLSNMS_ATTR.IFNAME, name);
        this.ifName = name;
    }

    public void setServiceID(String serviceID) {
        setValue(MPLSNMS_ATTR.SERVICE_ID, serviceID);
    }

    public void setDescription(String descr) {
        setValue(MPLSNMS_ATTR.DESCRIPTION, descr);
    }

    private String getAbsoluteName(PortDto p) {
        if (p == null) {
            return null;
        }
        return p.getAbsoluteName();
    }

    private String getNodeIfName(PortDto p) {
        if (p == null) {
            return null;
        }
        return p.getNode().getName() + ":" + DtoUtil.getIfName(p);
    }

    public void updateAttachmentCircuitName(String acAName, String aNodeIfName, String acBName, String bNodeIfName) {
        boolean ac1Used = false;
        boolean ac2Used = false;
        List<String> newNames = new ArrayList<String>();
        String nodeIfName = null;
        String aName = getMatchedName(aNodeIfName, ac1NodeIfName, ac2NodeIfName);
        if (aName != null) {
            if (aName.equals(ac1NodeIfName)) {
                ac1Used = true;
            } else {
                ac2Used = true;
            }
        } else {
            newNames.add(aName);
            nodeIfName = aNodeIfName;
        }

        String bName = getMatchedName(bNodeIfName, ac1NodeIfName, ac2NodeIfName);
        if (bName != null) {
            if (bName.equals(ac1NodeIfName)) {
                ac1Used = true;
            } else {
                ac2Used = true;
            }
        } else {
            newNames.add(bName);
            nodeIfName = bNodeIfName;
        }
        if (ac1Used && ac2Used) {
            return;
        }

        if (newNames.size() == 2) {
            if (ac1Used || ac2Used) {
                throw new IllegalStateException();
            }
            setAttachmentCircuit1Name(acAName, aNodeIfName);
            setAttachmentCircuit2Name(acBName, bNodeIfName);
        } else if (newNames.size() == 1) {
            String acName = newNames.iterator().next();
            if (ac1Used && ac2Used) {
                throw new IllegalStateException("no vacant ac.");
            } else if (!ac1Used && !ac2Used) {
                throw new IllegalStateException("too many vacant ac.");
            } else if (!ac1Used) {
                setAttachmentCircuit1Name(acName, getIfNamePart(nodeIfName));
            } else if (!ac2Used) {
                setAttachmentCircuit2Name(acName, getIfNamePart(nodeIfName));
            }
        } else {
            throw new IllegalStateException("no new names.");
        }
    }

    private void setAttachmentCircuit1Name(String acName, String nodeIfName) {
        recordChange("Attachment Port 1", this.ac1NodeIfName, nodeIfName);
        recordChange(ATTR.PW_AC1, this.ac1AbsoluteName, acName, false);
        this.ac1AbsoluteName = acName;
    }

    private void setAttachmentCircuit2Name(String acName, String nodeIfName) {
        recordChange("Attachment Port 2", this.ac2NodeIfName, nodeIfName);
        recordChange(ATTR.PW_AC2, this.ac2AbsoluteName, acName, false);
        this.ac2AbsoluteName = acName;
    }

    private String getIfNamePart(String nodeIfName) {
        if (nodeIfName == null) {
            throw new IllegalArgumentException();
        } else if (nodeIfName.indexOf(':') == -1) {
            throw new IllegalArgumentException();
        }
        return nodeIfName.split(":")[1];
    }

    private String getMatchedName(String key, String value1, String value2) {
        if (key == null) {
            return null;
        } else if (key.equals(value1)) {
            return value1;
        } else if (key.equals(value2)) {
            return value2;
        }
        return null;
    }

    public void setPseudoWireType(String type) {
        setValue(MPLSNMS_ATTR.PSEUDOWIRE_TYPE, type);
    }

    public void setFacilityStatus(String status) {
        setValue(MPLSNMS_ATTR.FACILITY_STATUS, status);
    }

    public void setServiceType(String type) {
        setValue(MPLSNMS_ATTR.SERVICE_TYPE, type);
    }

    public void setActualServiceType(String type) {
        setValue(MPLSNMS_ATTR.ACCOMMODATION_SERVICE_TYPE, type);
    }

    public void setBandwidth(Long bandwidth) {
        String value;
        if (bandwidth == null) {
            value = null;
        } else {
            value = bandwidth.toString();
        }
        setValue(MPLSNMS_ATTR.CONTRACT_BANDWIDTH, value);
    }

    public String getPipeAbsoluteName() {
        return this.nodeName + ATTR.NAME_DELIMITER_PRIMARY +
                ATTR.TYPE_INTERCONNECTION_IF + ATTR.NAME_DELIMITER_SECONDARY +
                this.ifName;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        if (this.target != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (this.target == null) {
            InventoryBuilder.changeContext(cmd, ATTR.TYPE_NODE, this.nodeName);
            cmd.addCommand(InventoryBuilder.translate(CMD.NEW_PORT,
                    CMD.ARG_TYPE, ATTR.TYPE_INTERCONNECTION_IF,
                    CMD.ARG_NAME, this.ifName));
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
        } else {
            InventoryBuilder.changeContext(cmd, this.target);
        }
        this.cmd.addLastEditCommands();
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
        updateAc1();
        updateAc2();
        return BuildResult.SUCCESS;
    }

    private void updateAc1() {
        if (!isChanged(ATTR.PW_AC1)) {
            return;
        }
        updateAc(this.originalAc1AbsoluteName, this.ac1AbsoluteName);
    }

    private void updateAc2() {
        if (!isChanged(ATTR.PW_AC2)) {
            return;
        }
        updateAc(this.originalAc2AbsoluteName, this.ac2AbsoluteName);
    }

    private void updateAc(String originalAc, String acName) {
        if (originalAc == null && acName == null) {
            return;
        } else if (originalAc != null && acName == null) {
            InventoryBuilder.translate(cmd, CMD.DISCONNECT_NETWORK_INSTANCE_PORT,
                    CMD.ARG_INSTANCE, getPipeAbsoluteName(),
                    CMD.ARG_PORT, acName);
        } else if (originalAc == null && acName != null) {
            InventoryBuilder.translate(cmd, CMD.CONNECT_NETWORK_INSTANCE_PORT,
                    CMD.ARG_INSTANCE, getPipeAbsoluteName(),
                    CMD.ARG_PORT, acName);
        } else {
            InventoryBuilder.translate(cmd, CMD.DISCONNECT_NETWORK_INSTANCE_PORT,
                    CMD.ARG_INSTANCE, getPipeAbsoluteName(),
                    CMD.ARG_PORT, acName);
            InventoryBuilder.translate(cmd, CMD.CONNECT_NETWORK_INSTANCE_PORT,
                    CMD.ARG_INSTANCE, getPipeAbsoluteName(),
                    CMD.ARG_PORT, acName);
        }
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        checkBuilt();
        if (this.target == null) {
            return BuildResult.FAIL;
        }
        InventoryBuilder.changeContext(cmd, this.target);
        for (PortDto ac : this.target.getAttachedPorts()) {
            InventoryBuilder.translate(cmd, CMD.DISCONNECT_NETWORK_INSTANCE_PORT,
                    CMD.ARG_INSTANCE, this.target.getAbsoluteName(),
                    CMD.ARG_PORT, ac.getAbsoluteName());
        }
        cmd.addLastEditCommands();
        cmd.addCommand(CMD.CONTEXT_DOWN);
        InventoryBuilder.buildNodeElementDeletionCommands(cmd, target);
        recordChange("PIPE", DtoUtil.getIfName(target), null);
        return BuildResult.SUCCESS;
    }

    public String getObjectType() {
        return DiffObjectType.PSEUDOWIRE.getCaption();
    }

}