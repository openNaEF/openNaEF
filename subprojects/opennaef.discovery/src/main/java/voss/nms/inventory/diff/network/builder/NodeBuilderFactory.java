package voss.nms.inventory.diff.network.builder;

import naef.dto.NodeDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.NodeCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.DeviceRenderer;

import java.util.List;

public class NodeBuilderFactory implements BuilderFactory {
    private final NodeDto node;
    private final DeviceRenderer renderer;
    private final String editorName;

    public NodeBuilderFactory(NodeDto node, DeviceRenderer renderer, String editorName) {
        if (Util.isAllNull(node, renderer)) {
            throw new IllegalArgumentException();
        }
        this.node = node;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    @Override
    public CommandBuilder getBuilder() {
        NodeCommandBuilder builder = null;
        if (node == null) {
            builder = new NodeCommandBuilder(editorName);
            builder.setVendor(renderer.getValue(DeviceRenderer.Attr.VENDOR_NAME));
            builder.setNodeName(renderer.getValue(DeviceRenderer.Attr.GUEST_DEVICE_NAME));
            builder.setCreateChassis(false);
            builder.setSource(DiffCategory.DISCOVERY.name());
            builder.setLocation(renderer.getValue(DeviceRenderer.Attr.LOCATION));
        } else {
            builder = new NodeCommandBuilder(node, editorName);
        }
        if (renderer != null) {
            boolean virtual = renderer.isVirtual();
            builder.setVirtualNode(virtual);
            builder.setVendor(renderer.getValue(DeviceRenderer.Attr.VENDOR_NAME));
            String nodeType = renderer.getValue(DeviceRenderer.Attr.DEVICE_TYPE);
            builder.setNodeType(nodeType);
            String osType = renderer.getValue(DeviceRenderer.Attr.OS_TYPE);
            builder.setOsType(osType);
            String osVersion = renderer.getValue(DeviceRenderer.Attr.OS_VERSION);
            builder.setOsVersion(osVersion);
            List<String> zones = renderer.getZones();
            builder.setZoneList(zones);
        }
        return builder;
    }
}