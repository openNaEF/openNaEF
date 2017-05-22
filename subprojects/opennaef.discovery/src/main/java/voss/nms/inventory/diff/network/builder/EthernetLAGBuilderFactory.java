package voss.nms.inventory.diff.network.builder;

import naef.dto.eth.EthLagIfDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.EthernetLAGCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.EthernetLAGRenderer;

import java.util.List;

public class EthernetLAGBuilderFactory extends AbstractPortBuilderFactory {
    private final EthLagIfDto target;
    private final EthernetLAGRenderer renderer;
    private final String editorName;

    public EthernetLAGBuilderFactory(EthLagIfDto lag, EthernetLAGRenderer renderer, String editorName) {
        if (Util.isAllNull(lag, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = lag;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    public EthernetLAGBuilderFactory(EthernetLAGRenderer renderer, String editorName) {
        this(null, renderer, editorName);
    }

    @Override
    public CommandBuilder getBuilder() {
        EthernetLAGCommandBuilder builder = null;
        if (this.target == null) {
            builder = new EthernetLAGCommandBuilder(this.renderer.getParentAbsoluteName(),
                    this.renderer.getValue(EthernetLAGRenderer.Attr.IFNAME), editorName);
            builder.setSource(DiffCategory.DISCOVERY.name());
        } else {
            builder = new EthernetLAGCommandBuilder(this.target, editorName);
        }
        builder.setPreCheckEnable(false);
        if (this.renderer != null) {
            EthernetLAGRenderer renderer = (EthernetLAGRenderer) this.renderer;
            String ifName = renderer.getValue(EthernetLAGRenderer.Attr.IFNAME);
            builder.setIfName(ifName);
            String configName = renderer.getValue(EthernetLAGRenderer.Attr.CONFIGNAME);
            if (configName != null) {
                builder.setConfigName(configName);
            }
            String ifType = renderer.getValue(EthernetLAGRenderer.Attr.PORT_TYPE);
            builder.setPortType(ifType);
            String portModeValue = renderer.getValue(EthernetLAGRenderer.Attr.PORT_MODE);
            if (portModeValue == null) {
                builder.setSwitchPortMode(null);
                builder.setPortMode(null);
            } else {
                builder.setPortMode(portModeValue);
                builder.setSwitchPortMode(renderer.getValue(EthernetLAGRenderer.Attr.SWITCH_PORT_MODE));
            }
            String ipIfName = renderer.getValue(EthernetLAGRenderer.Attr.IPIFNAME);
            builder.setIpIfName(ipIfName);
            builder.setIndependentIP(ipIfName != null);
            String ip = renderer.getValue(EthernetLAGRenderer.Attr.IP_ADDRESS);
            String mask = renderer.getValue(EthernetLAGRenderer.Attr.SUBNETMASK);
            setIpAddress(builder, renderer.getModel(), ip, mask);
            String description = renderer.getValue(EthernetLAGRenderer.Attr.DESCRIPTION);
            builder.setPortDescription(description);
            String adminStatus = renderer.getValue(EthernetLAGRenderer.Attr.STATUS_ADMIN);
            builder.setAdminStatus(adminStatus);
            builder.setOspfAreaID(renderer.getValue(EthernetLAGRenderer.Attr.OSPF_AREA_ID));
            Integer cost = Integer.valueOf(renderer.getValue(EthernetLAGRenderer.Attr.IGP_COST));
            builder.setIgpCost(cost);
            builder.setMemberPorts(renderer.getMemberPorts());
            String level = renderer.getValue(EthernetLAGRenderer.Attr.STORMCONTROL_BROADCAST_LEVEL);
            builder.setStormControlBroadcastLevel(level);
            List<String> actions = renderer.getValues(EthernetLAGRenderer.Attr.STORMCONTROL_ACTION);
            builder.setStormControlActions(actions);
        }
        return builder;
    }
}