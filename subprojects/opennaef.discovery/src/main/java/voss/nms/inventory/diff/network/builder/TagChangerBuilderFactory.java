package voss.nms.inventory.diff.network.builder;

import naef.dto.vlan.VlanSegmentGatewayIfDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.VlanSegmentGatewayIfCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.TagChangerRenderer;

public class TagChangerBuilderFactory extends AbstractPortBuilderFactory {
    private final VlanSegmentGatewayIfDto target;
    private final TagChangerRenderer renderer;
    private final String editorName;

    public TagChangerBuilderFactory(VlanSegmentGatewayIfDto port, TagChangerRenderer renderer, String editorName) {
        if (Util.isAllNull(port, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = port;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    public TagChangerBuilderFactory(TagChangerRenderer renderer, String editorName) {
        this(null, renderer, editorName);
    }

    @Override
    public CommandBuilder getBuilder() {
        VlanSegmentGatewayIfCommandBuilder builder;
        if (this.target == null) {
            builder = new VlanSegmentGatewayIfCommandBuilder(this.renderer.getParentAbsoluteName(), editorName);
            builder.setSource(DiffCategory.DISCOVERY.name());
        } else {
            if (!(this.target instanceof VlanSegmentGatewayIfDto)) {
                throw new IllegalStateException("illegal target: " + this.target.getAbsoluteName());
            }
            builder = new VlanSegmentGatewayIfCommandBuilder(this.target, editorName);
        }
        builder.setPreCheckEnable(false);
        if (renderer != null) {
            Integer vlanID = Integer.valueOf(this.renderer.getValue(TagChangerRenderer.Attr.INNER_VLAN_ID));
            builder.setInnerVlanId(vlanID);
            String outerVlanID = this.renderer.getValue(TagChangerRenderer.Attr.OUTER_VLAN_ID);
            builder.setOuterVlanId(outerVlanID);
            try {
                Long bandwidth = Long.valueOf(this.renderer.getValue(TagChangerRenderer.Attr.BANDWIDTH));
                builder.setBandwidth(bandwidth);
            } catch (Exception e) {
            }
            builder.setIfName(renderer.getValue(TagChangerRenderer.Attr.IFNAME));
            String configName = renderer.getValue(TagChangerRenderer.Attr.CONFIGNAME);
            if (configName != null) {
                builder.setConfigName(configName);
            }
            String ip = renderer.getValue(TagChangerRenderer.Attr.IP_ADDRESS);
            String mask = renderer.getValue(TagChangerRenderer.Attr.SUBNETMASK);
            setIpAddress(builder, renderer.getModel(), ip, mask);
            String description = renderer.getValue(TagChangerRenderer.Attr.DESCRIPTION);
            builder.setPortDescription(description);
            builder.setOspfAreaID(renderer.getValue(TagChangerRenderer.Attr.OSPF_AREA_ID));
            Integer cost = Integer.valueOf(renderer.getValue(TagChangerRenderer.Attr.IGP_COST));
            builder.setIgpCost(cost);
        }
        return builder;
    }

}