package voss.nms.inventory.diff.network.builder;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.NodeUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.AliasPortCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.AliasPortRenderer;

public class AliasPortBuilderFactory extends AbstractPortBuilderFactory {
    private final PortDto target;
    private final AliasPortRenderer renderer;
    private final String editorName;

    public AliasPortBuilderFactory(PortDto port, AliasPortRenderer renderer, String editorName) {
        if (Util.isAllNull(port, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = port;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    @Override
    public CommandBuilder getBuilder() {
        AliasPortCommandBuilder builder;
        if (this.target == null) {
            String parentNodeName = getParentNodeName();
            NodeDto vm = getNode(parentNodeName);
            if (vm == null) {
                LoggerFactory.getLogger(getClass()).info("no virtual node found: " + parentNodeName);
                return null;
            }
            builder = new AliasPortCommandBuilder(vm, editorName);
            AliasPortRenderer renderer = (AliasPortRenderer) this.renderer;
            String ifName = renderer.getValue(AliasPortRenderer.Attr.IFNAME);
            builder.setIfName(ifName);
            String aliasSourceNode = renderer.getValue(AliasPortRenderer.Attr.ALIAS_SOURCE_NODE);
            String aliasSourceIfName = renderer.getValue(AliasPortRenderer.Attr.ALIAS_SOURCE_IFNAME);
            builder.setAliasSourceAbsoluteName(aliasSourceNode, aliasSourceIfName);
        } else {
            builder = new AliasPortCommandBuilder(this.target, editorName);
        }
        builder.setPreCheckEnable(false);
        if (this.renderer != null) {
            String configName = renderer.getValue(AliasPortRenderer.Attr.CONFIGNAME);
            if (configName != null) {
                builder.setConfigName(configName);
            }
            String description = renderer.getValue(AliasPortRenderer.Attr.DESCRIPTION);
            builder.setPortDescription(description);
            if (this.target == null) {
                builder.setSource(DiffCategory.DISCOVERY.name());
            }
        }
        return builder;
    }

    private String getParentNodeName() {
        String nodeName = this.renderer.getParentAbsoluteName();
        int idx = nodeName.indexOf(';');
        if (idx != -1) {
            nodeName = nodeName.substring(idx + 1);
        }
        return nodeName;
    }

    private NodeDto getNode(String nodeName) {
        try {
            NodeDto vm = NodeUtil.getNode(nodeName);
            return vm;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }
}