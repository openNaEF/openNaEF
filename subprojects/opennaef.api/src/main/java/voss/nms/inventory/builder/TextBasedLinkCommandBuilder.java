package voss.nms.inventory.builder;

import naef.dto.ip.IpSubnetDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TextBasedLinkCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private String id;
    private String nodeName1;
    private String ifName1;
    private String nodeName2;
    private String ifName2;
    private String linkType;

    public TextBasedLinkCommandBuilder(String editorName) {
        super(IpSubnetDto.class, null, editorName);
        setConstraint(IpSubnetDto.class);
    }

    public void setPort1Name(String nodeName, String ifName) {
        if (nodeName == null || ifName == null) {
            throw new IllegalArgumentException();
        }
        this.nodeName1 = nodeName;
        this.ifName1 = ifName;
        recordChange("Port1", null, nodeName + ":" + ifName);
    }

    public void setPort2Name(String nodeName, String ifName) {
        if (nodeName == null || ifName == null) {
            throw new IllegalArgumentException();
        }
        this.nodeName2 = nodeName;
        this.ifName2 = ifName;
        recordChange("Port2", null, nodeName + ":" + ifName);
    }

    public void setFoundOnNetwork(Boolean value) {
        if (value == null) {
            return;
        }
        setValue(MPLSNMS_ATTR.LINK_FOUND_ON_NETWORK, value.toString());
    }

    public void setSource(String source) {
        setValue(MPLSNMS_ATTR.SOURCE, source);
    }

    public void setLinkType(String type) {
        this.linkType = type;
    }

    public void setCustomLinkType(String type) {
        setValue(MPLSNMS_ATTR.LINK_TYPE, type);
    }

    public void setLinkId(String id) {
        this.id = id;
    }

    public String getLinkId() {
        if (this.id != null) {
            return this.id;
        }
        List<String> list = new ArrayList<String>();
        list.add(nodeName1 + ":" + ifName1);
        list.add(nodeName2 + ":" + ifName2);
        Collections.sort(list);
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) {
                sb.append("-");
            }
            sb.append(s);
        }
        return sb.toString();
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        if (this.linkType == null) {
            throw new IllegalStateException("no link-type.");
        }
        String registerDate = InventoryBuilder.getInventoryDateString(new Date());
        setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
        InventoryBuilder.translate(this.cmd, CMD.LINK_CONNECT,
                CMD.ARG_TYPE, this.linkType,
                CMD.ARG_FQN1, AbsoluteNameFactory.getIfNameAbsoluteName(nodeName1, ifName1),
                CMD.ARG_FQN2, AbsoluteNameFactory.getIfNameAbsoluteName(nodeName2, ifName2));
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes, false);
        return BuildResult.SUCCESS;
    }

    @Override
    public BuildResult buildDeleteCommandInner() throws IOException {
        throw new IllegalStateException();
    }

    public String getObjectType() {
        return DiffObjectType.L3_LINK.getCaption();
    }

}