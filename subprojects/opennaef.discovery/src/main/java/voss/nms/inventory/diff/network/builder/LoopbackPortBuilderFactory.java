package voss.nms.inventory.diff.network.builder;

import naef.dto.ip.IpIfDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.LoopbackPortCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.LoopbackPortRenderer;

public class LoopbackPortBuilderFactory extends AbstractPortBuilderFactory {
    private final IpIfDto target;
    private final LoopbackPortRenderer renderer;
    private final String editorName;

    public LoopbackPortBuilderFactory(IpIfDto loopback, LoopbackPortRenderer renderer, String editorName) {
        if (Util.isAllNull(loopback, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = loopback;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    public LoopbackPortBuilderFactory(LoopbackPortRenderer renderer, String editorName) {
        this(null, renderer, editorName);
    }

    @Override
    public CommandBuilder getBuilder() {
        LoopbackPortCommandBuilder builder;
        if (this.target == null) {
            builder = new LoopbackPortCommandBuilder(this.renderer.getParentAbsoluteName(), editorName);
            builder.setSource(DiffCategory.DISCOVERY.name());
        } else {
            builder = new LoopbackPortCommandBuilder(this.target, editorName);
        }
        if (renderer != null) {
            builder.setIfName(renderer.getValue(LoopbackPortRenderer.Attr.IFNAME));
            String configName = renderer.getValue(LoopbackPortRenderer.Attr.CONFIGNAME);
            if (configName != null) {
                builder.setConfigName(configName);
            }
            String ip = renderer.getValue(LoopbackPortRenderer.Attr.IP_ADDRESS);
            String mask = renderer.getValue(LoopbackPortRenderer.Attr.SUBNETMASK);
            setIpAddress(builder, renderer.getModel(), ip, mask);
            String description = renderer.getValue(LoopbackPortRenderer.Attr.DESCRIPTION);
            builder.setPortDescription(description);
            builder.setOspfAreaID(renderer.getValue(LoopbackPortRenderer.Attr.OSPF_AREA_ID));
            Integer cost = Integer.valueOf(renderer.getValue(LoopbackPortRenderer.Attr.IGP_COST));
            builder.setIgpCost(cost);
        }
        return builder;
    }

}