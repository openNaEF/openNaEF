package voss.nms.inventory.diff.network.builder;

import naef.dto.atm.AtmApsIfDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.AtmAPSCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.ATMAPSRenderer;

public class ATMAPSBuilderFactory extends AbstractPortBuilderFactory {
    private final AtmApsIfDto target;
    private final ATMAPSRenderer renderer;
    private final String editorName;

    public ATMAPSBuilderFactory(AtmApsIfDto lag, ATMAPSRenderer renderer, String editorName) {
        if (Util.isAllNull(lag, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = lag;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    public ATMAPSBuilderFactory(ATMAPSRenderer renderer, String editorName) {
        this(null, renderer, editorName);
    }

    @Override
    public CommandBuilder getBuilder() {
        AtmAPSCommandBuilder builder = null;
        if (this.target == null) {
            builder = new AtmAPSCommandBuilder(this.renderer.getParentAbsoluteName(),
                    this.renderer.getValue(ATMAPSRenderer.Attr.IFNAME), editorName);
            builder.setSource(DiffCategory.DISCOVERY.name());
        } else {
            builder = new AtmAPSCommandBuilder(this.target, editorName);
        }
        builder.setPreCheckEnable(false);
        if (this.renderer != null) {
            ATMAPSRenderer renderer = (ATMAPSRenderer) this.renderer;
            String ifName = renderer.getValue(ATMAPSRenderer.Attr.IFNAME);
            builder.setIfName(ifName);
            String configName = renderer.getValue(ATMAPSRenderer.Attr.CONFIGNAME);
            if (configName != null) {
                builder.setConfigName(configName);
            }
            String ipIfName = renderer.getValue(ATMAPSRenderer.Attr.IPIFNAME);
            builder.setIpIfName(ipIfName);
            builder.setIndependentIP(ipIfName != null);
            String ip = renderer.getValue(ATMAPSRenderer.Attr.IP_ADDRESS);
            String mask = renderer.getValue(ATMAPSRenderer.Attr.SUBNETMASK);
            setIpAddress(builder, renderer.getModel(), ip, mask);
            String description = renderer.getValue(ATMAPSRenderer.Attr.DESCRIPTION);
            builder.setPortDescription(description);
            builder.setMemberPorts(renderer.getMemberPorts());
            builder.setOspfAreaID(renderer.getValue(ATMAPSRenderer.Attr.OSPF_AREA_ID));
            Integer cost = Integer.valueOf(renderer.getValue(ATMAPSRenderer.Attr.IGP_COST));
            builder.setIgpCost(cost);
        }
        return builder;
    }
}