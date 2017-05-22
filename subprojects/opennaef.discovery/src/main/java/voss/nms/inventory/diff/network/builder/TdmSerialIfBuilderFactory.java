package voss.nms.inventory.diff.network.builder;

import naef.dto.serial.TdmSerialIfDto;
import voss.core.server.builder.CommandBuilder;
import voss.nms.inventory.builder.ChannelPortCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.TdmSerialIfRenderer;

public class TdmSerialIfBuilderFactory extends AbstractPortBuilderFactory {
    private final TdmSerialIfDto target;
    private final TdmSerialIfRenderer renderer;
    private final String editorName;

    public TdmSerialIfBuilderFactory(TdmSerialIfDto port, TdmSerialIfRenderer renderer, String editorName) {
        this.target = port;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    public TdmSerialIfBuilderFactory(TdmSerialIfRenderer renderer, String editorName) {
        this(null, renderer, editorName);
    }

    @Override
    public CommandBuilder getBuilder() {
        ChannelPortCommandBuilder builder;
        if (this.target == null) {
            builder = new ChannelPortCommandBuilder(this.renderer.getParentAbsoluteName(), editorName);
            builder.setSource(DiffCategory.DISCOVERY.name());
        } else {
            builder = new ChannelPortCommandBuilder(this.target, editorName);
        }
        builder.setPreCheckEnable(false);
        if (renderer != null) {
            try {
                Long bandwidth = Long.valueOf(this.renderer.getValue(TdmSerialIfRenderer.Attr.BANDWIDTH));
                builder.setBandwidth(bandwidth);
            } catch (Exception e) {
            }
            builder.setIfName(renderer.getValue(TdmSerialIfRenderer.Attr.IFNAME));
            String configName = renderer.getValue(TdmSerialIfRenderer.Attr.CONFIGNAME);
            if (configName != null) {
                builder.setConfigName(configName);
            }
            builder.setTimeSlot(renderer.getValue(TdmSerialIfRenderer.Attr.TIMESLOT));
            builder.setChannelGroup(renderer.getValue(TdmSerialIfRenderer.Attr.CHANNEL_GROUP));
            String ip = renderer.getValue(TdmSerialIfRenderer.Attr.IP_ADDRESS);
            String mask = renderer.getValue(TdmSerialIfRenderer.Attr.SUBNETMASK);
            setIpAddress(builder, renderer.getModel(), ip, mask);
            String description = renderer.getValue(TdmSerialIfRenderer.Attr.DESCRIPTION);
            builder.setPortDescription(description);
        }
        return builder;
    }

}