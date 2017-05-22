package voss.nms.inventory.diff.network.builder;

import naef.dto.NodeDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.NodeUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.VirtualNodeCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.DeviceRenderer;

import java.util.List;

public class VirtualNodeBuilderFactory implements BuilderFactory {
    private final NodeDto node;
    private final NodeDto host;
    private final DeviceRenderer renderer;
    private final String key;
    private final String editorName;

    public VirtualNodeBuilderFactory(NodeDto node, DeviceRenderer renderer, String editorName) {
        if (Util.isAllNull(node, renderer)) {
            throw new IllegalArgumentException();
        }
        this.node = node;
        NodeDto _host = null;
        if (this.node != null) {
            _host = node.getVirtualizationHostNode();
        }
        if (_host == null) {
            try {
                _host = NodeUtil.getNode(renderer.getHostNodeName());
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
        this.host = _host;
        this.renderer = renderer;
        this.key = (this.node != null ? node.getName() : renderer.getID());
        this.editorName = editorName;
    }

    @Override
    public CommandBuilder getBuilder() {
        if (this.host == null) {
            throw new IllegalArgumentException("no virtual host device: " + this.key);
        }
        VirtualNodeCommandBuilder builder = null;
        if (node == null) {
            builder = new VirtualNodeCommandBuilder(editorName);
            builder.setVendor(renderer.getValue(DeviceRenderer.Attr.VENDOR_NAME));
            builder.setGuestNodeName(renderer.getValue(DeviceRenderer.Attr.GUEST_DEVICE_NAME));
            builder.setSource(DiffCategory.DISCOVERY.name());
        } else {
            builder = new VirtualNodeCommandBuilder(node, editorName);
        }
        builder.setHyperVisor(host);
        if (renderer != null) {
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