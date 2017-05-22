package voss.nms.inventory.diff.network.builder;

import naef.dto.atm.AtmPvcIfDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.AtmVcCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.AtmPvcIfRenderer;

public class AtmPvcIfBuilderFactory extends AbstractPortBuilderFactory {
    private final AtmPvcIfDto target;
    private final AtmPvcIfRenderer renderer;
    private final String editorName;

    public AtmPvcIfBuilderFactory(AtmPvcIfDto port, AtmPvcIfRenderer renderer, String editorName) {
        if (Util.isAllNull(port, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = port;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    public AtmPvcIfBuilderFactory(AtmPvcIfRenderer renderer, String editorName) {
        this(null, renderer, editorName);
    }

    @Override
    public CommandBuilder getBuilder() {
        AtmVcCommandBuilder builder;
        if (this.target == null) {
            builder = new AtmVcCommandBuilder(this.renderer.getParentAbsoluteName(), editorName);
            builder.setSource(DiffCategory.DISCOVERY.name());
        } else {
            builder = new AtmVcCommandBuilder(this.target, editorName);
        }
        builder.setPreCheckEnable(false);
        if (renderer != null) {
            builder.setIfName(renderer.getValue(AtmPvcIfRenderer.Attr.IFNAME));
            String configName = renderer.getValue(AtmPvcIfRenderer.Attr.CONFIGNAME);
            if (configName != null) {
                builder.setConfigName(configName);
            }
            builder.setVci(renderer.getValue(AtmPvcIfRenderer.Attr.VCI));
            String value = renderer.getValue(AtmPvcIfRenderer.Attr.BANDWIDTH);
            if (value != null) {
                Long bandwidth = Long.parseLong(value);
                builder.setBandwidth(bandwidth);
            }
            String ip = renderer.getValue(AtmPvcIfRenderer.Attr.IP_ADDRESS);
            String mask = renderer.getValue(AtmPvcIfRenderer.Attr.SUBNETMASK);
            setIpAddress(builder, renderer.getModel(), ip, mask);
            String description = renderer.getValue(AtmPvcIfRenderer.Attr.DESCRIPTION);
            builder.setPortDescription(description);
            builder.setOspfAreaID(renderer.getValue(AtmPvcIfRenderer.Attr.OSPF_AREA_ID));
            Integer cost = Integer.valueOf(renderer.getValue(AtmPvcIfRenderer.Attr.IGP_COST));
            builder.setIgpCost(cost);
        }
        return builder;
    }

}