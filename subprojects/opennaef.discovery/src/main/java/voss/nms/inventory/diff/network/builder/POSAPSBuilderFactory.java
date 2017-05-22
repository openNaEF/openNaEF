package voss.nms.inventory.diff.network.builder;

import naef.dto.pos.PosApsIfDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.POSAPSCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.POSAPSRenderer;

public class POSAPSBuilderFactory extends AbstractPortBuilderFactory {
    private final PosApsIfDto target;
    private final POSAPSRenderer renderer;
    private final String editorName;

    public POSAPSBuilderFactory(PosApsIfDto lag, POSAPSRenderer renderer, String editorName) {
        if (Util.isAllNull(lag, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = lag;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    public POSAPSBuilderFactory(POSAPSRenderer renderer, String editorName) {
        this(null, renderer, editorName);
    }

    @Override
    public CommandBuilder getBuilder() {
        POSAPSCommandBuilder builder = null;
        if (this.target == null) {
            builder = new POSAPSCommandBuilder(this.renderer.getParentAbsoluteName(),
                    this.renderer.getValue(POSAPSRenderer.Attr.IFNAME), editorName);
            builder.setSource(DiffCategory.DISCOVERY.name());
        } else {
            builder = new POSAPSCommandBuilder(this.target, editorName);
        }
        builder.setPreCheckEnable(false);
        if (this.renderer != null) {
            POSAPSRenderer renderer = (POSAPSRenderer) this.renderer;
            String ifName = renderer.getValue(POSAPSRenderer.Attr.IFNAME);
            builder.setIfName(ifName);
            String configName = renderer.getValue(POSAPSRenderer.Attr.CONFIGNAME);
            if (configName != null) {
                builder.setConfigName(configName);
            }
            String ipIfName = renderer.getValue(POSAPSRenderer.Attr.IPIFNAME);
            builder.setIpIfName(ipIfName);
            builder.setIndependentIP(ipIfName != null);
            String ip = renderer.getValue(POSAPSRenderer.Attr.IP_ADDRESS);
            String mask = renderer.getValue(POSAPSRenderer.Attr.SUBNETMASK);
            setIpAddress(builder, renderer.getModel(), ip, mask);
            String description = renderer.getValue(POSAPSRenderer.Attr.DESCRIPTION);
            builder.setPortDescription(description);
            builder.setMemberPorts(renderer.getMemberPorts());
            builder.setOspfAreaID(renderer.getValue(POSAPSRenderer.Attr.OSPF_AREA_ID));
            Integer cost = Integer.valueOf(renderer.getValue(POSAPSRenderer.Attr.IGP_COST));
            builder.setIgpCost(cost);
        }
        return builder;
    }
}