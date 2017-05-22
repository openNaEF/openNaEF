package voss.nms.inventory.diff.network.builder;

import naef.dto.atm.AtmPvpIfDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.AtmVpCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.AtmVpRenderer;

public class AtmVpBuilderFactory implements BuilderFactory {
    private final AtmPvpIfDto target;
    private final AtmVpRenderer renderer;
    private final String editorName;

    public AtmVpBuilderFactory(AtmPvpIfDto port, AtmVpRenderer renderer, String editorName) {
        if (Util.isAllNull(port, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = port;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    public AtmVpBuilderFactory(AtmVpRenderer renderer, String editorName) {
        this(null, renderer, editorName);
    }

    @Override
    public CommandBuilder getBuilder() {
        AtmVpCommandBuilder builder;
        if (this.target == null) {
            builder = new AtmVpCommandBuilder(this.renderer.getParentAbsoluteName(), editorName);
            builder.setSource(DiffCategory.DISCOVERY.name());
        } else {
            builder = new AtmVpCommandBuilder(this.target, editorName);
        }
        builder.setPreCheckEnable(false);
        if (renderer != null) {
            builder.setIfName(renderer.getValue(AtmVpRenderer.Attr.IFNAME));
            String configName = renderer.getValue(AtmVpRenderer.Attr.CONFIGNAME);
            if (configName != null) {
                builder.setConfigName(configName);
            }
            builder.setVpi(renderer.getValue(AtmVpRenderer.Attr.VPI));
            String description = renderer.getValue(AtmVpRenderer.Attr.DESCRIPTION);
            builder.setPortDescription(description);
            builder.setImplicitValue(renderer.getValue(AtmVpRenderer.Attr.IMPLICIT));
        }
        return builder;
    }

}