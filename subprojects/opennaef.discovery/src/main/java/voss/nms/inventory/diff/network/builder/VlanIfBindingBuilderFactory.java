package voss.nms.inventory.diff.network.builder;

import naef.dto.vlan.VlanIfDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.VlanIfBindingCommandBuilder;
import voss.nms.inventory.diff.network.analyzer.VlanIfBindingRenderer;

import java.util.List;

public class VlanIfBindingBuilderFactory extends AbstractPortBuilderFactory {
    private final VlanIfDto target;
    private final VlanIfBindingRenderer renderer;
    private final String editorName;

    public VlanIfBindingBuilderFactory(VlanIfDto port, VlanIfBindingRenderer renderer, String editorName) {
        if (Util.isAllNull(port, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = port;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    public VlanIfBindingBuilderFactory(VlanIfBindingRenderer renderer, String editorName) {
        this(null, renderer, editorName);
    }

    @Override
    public CommandBuilder getBuilder() {
        VlanIfBindingCommandBuilder builder;
        if (this.target == null) {
            String vlanName = this.renderer.getValue(VlanIfBindingRenderer.Attr.PORT_ID);
            builder = new VlanIfBindingCommandBuilder(this.renderer.getParentAbsoluteName(), vlanName, editorName);
        } else {
            builder = new VlanIfBindingCommandBuilder(this.target, editorName);
        }
        builder.setPreCheckEnable(false);
        if (renderer != null) {
            List<String> taggedPorts = this.renderer.getTaggedPorts();
            List<String> taggedPortIfNames = this.renderer.getTaggedPortIfNames();
            builder.setTaggedPorts(taggedPorts, taggedPortIfNames);
            List<String> untaggedPorts = this.renderer.getUntaggedPorts();
            List<String> untaggedPortIfNames = this.renderer.getUntaggedPortIfNames();
            builder.setUntaggedPorts(untaggedPorts, untaggedPortIfNames);
        }
        return builder;
    }

}