package voss.nms.inventory.diff.network.builder;

import naef.dto.eth.EthLagIfDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.EthernetEPSCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.EthernetEPSRenderer;

public class EthernetEPSBuilderFactory extends AbstractPortBuilderFactory {
    private final EthLagIfDto target;
    private final EthernetEPSRenderer renderer;
    private final String editorName;

    public EthernetEPSBuilderFactory(EthLagIfDto lag, EthernetEPSRenderer renderer, String editorName) {
        if (Util.isAllNull(lag, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = lag;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    public EthernetEPSBuilderFactory(EthernetEPSRenderer renderer, String editorName) {
        this(null, renderer, editorName);
    }

    @Override
    public CommandBuilder getBuilder() {
        EthernetEPSCommandBuilder builder = null;
        if (this.target == null) {
            builder = new EthernetEPSCommandBuilder(this.renderer.getParentAbsoluteName(),
                    this.renderer.getValue(EthernetEPSRenderer.Attr.IFNAME), editorName);
            builder.setSource(DiffCategory.DISCOVERY.name());
        } else {
            builder = new EthernetEPSCommandBuilder(this.target, editorName);
        }
        builder.setPreCheckEnable(false);
        if (this.renderer != null) {
            EthernetEPSRenderer renderer = (EthernetEPSRenderer) this.renderer;
            String ifName = renderer.getValue(EthernetEPSRenderer.Attr.IFNAME);
            builder.setIfName(ifName);
            String configName = renderer.getValue(EthernetEPSRenderer.Attr.CONFIGNAME);
            if (configName != null) {
                builder.setConfigName(configName);
            }
            String ifType = renderer.getValue(EthernetEPSRenderer.Attr.PORT_TYPE);
            builder.setPortType(ifType);
            String portModeValue = renderer.getValue(EthernetEPSRenderer.Attr.PORT_MODE);
            if (portModeValue == null) {
                builder.setSwitchPortMode(null);
                builder.setPortMode(null);
            } else {
                builder.setPortMode(portModeValue);
                builder.setSwitchPortMode(renderer.getValue(EthernetEPSRenderer.Attr.SWITCH_PORT_MODE));
            }
            String ipIfName = renderer.getValue(EthernetEPSRenderer.Attr.IPIFNAME);
            builder.setIpIfName(ipIfName);
            builder.setIndependentIP(ipIfName != null);
            String ip = renderer.getValue(EthernetEPSRenderer.Attr.IP_ADDRESS);
            String mask = renderer.getValue(EthernetEPSRenderer.Attr.SUBNETMASK);
            setIpAddress(builder, renderer.getModel(), ip, mask);
            String description = renderer.getValue(EthernetEPSRenderer.Attr.DESCRIPTION);
            builder.setPortDescription(description);
            String adminStatus = renderer.getValue(EthernetEPSRenderer.Attr.STATUS_ADMIN);
            builder.setAdminStatus(adminStatus);
            builder.setOspfAreaID(renderer.getValue(EthernetEPSRenderer.Attr.OSPF_AREA_ID));
            Integer cost = Integer.valueOf(renderer.getValue(EthernetEPSRenderer.Attr.IGP_COST));
            builder.setIgpCost(cost);
            builder.setMemberPorts(renderer.getMemberPorts());
        }
        return builder;
    }
}