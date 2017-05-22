package voss.nms.inventory.builder;

import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TextBasedIpLinkCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private String id;
    private String ipName1;
    private String ifName1;
    private String ipName2;
    private String ifName2;
    private int maxPorts = 2;

    public TextBasedIpLinkCommandBuilder(String editorName) {
        super(IpSubnetDto.class, null, editorName);
        setConstraint(IpSubnetDto.class);
    }

    public void setPort1Name(String ipName, String ifName) {
        if (ipName == null || ifName == null) {
            throw new IllegalArgumentException();
        }
        this.ipName1 = ipName;
        this.ifName1 = ifName;
        recordChange("Port1", null, ipName);
    }

    public void setPort2Name(String ipName, String ifName) {
        if (ipName == null || ifName == null) {
            throw new IllegalArgumentException();
        }
        this.ipName2 = ipName;
        this.ifName2 = ifName;
        recordChange("Port2", null, ipName);
    }

    public void setFoundOnNetwork(Boolean value) {
        if (value == null) {
            return;
        }
        setValue(MPLSNMS_ATTR.LINK_FOUND_ON_NETWORK, value.toString());
    }

    public void setMaxPorts(int ports) {
        setValue(ATTR.LINK_MAX_PORTS, String.valueOf(maxPorts));
    }

    public void setFoundOnExternalInventoryDB(Boolean value) {
        if (value == null) {
            return;
        }
        setValue(MPLSNMS_ATTR.LINK_FOUND_ON_EXTERNAL_INVENTORY_DB, value.toString());
    }

    public void setApproved(Boolean value) {
        if (value == null) {
            return;
        }
        setValue(MPLSNMS_ATTR.LINK_APPROVED, value.toString());
    }

    public void setSource(String source) {
        setValue(MPLSNMS_ATTR.SOURCE, source);
    }

    public void setSRLGValue(String sRLG) {
        setValue(MPLSNMS_ATTR.LINK_SRLG_VALUE, sRLG);
    }

    public void setCableName(String cableName) {
        setValue(MPLSNMS_ATTR.LINK_CABLE_NAME, cableName);
    }

    public void setLinkType(String type) {
        setValue(MPLSNMS_ATTR.LINK_TYPE, type);
    }

    public void setLinkAccommodationLimit(String capacity) {
        setValue(MPLSNMS_ATTR.LINK_ACCOMMODATION_LIMIT, capacity);
    }

    public void setLinkId(String id) {
        this.id = id;
    }

    public String getLinkId() {
        if (this.id != null) {
            return this.id;
        }
        List<String> list = new ArrayList<String>();
        list.add(ifName1);
        list.add(ifName2);
        Collections.sort(list);
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            sb.append(s);
        }
        return sb.toString();
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        checkBuilt();
        String registerDate = InventoryBuilder.getInventoryDateString(new Date());
        setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
        String id = getLinkId();
        InventoryConnector conn = InventoryConnector.getInstance();
        try {
            IpSubnetNamespaceDto pool = conn.getActiveRootIpSubnetNamespace();
            InventoryBuilder.buildNetworkIDCreationCommand(cmd, ATTR.NETWORK_TYPE_IPSUBNET,
                    ATTR.ATTR_IPSUBNET_ID, id,
                    ATTR.ATTR_IPSUBNET_POOL, pool.getName());
        } catch (ExternalServiceException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
        InventoryBuilder.buildBindPortToNetworkCommands(cmd, ipName1);
        InventoryBuilder.buildBindPortToNetworkCommands(cmd, ipName2);
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        throw new IllegalStateException();
    }

    public String getObjectType() {
        return DiffObjectType.L3_LINK.getCaption();
    }

}