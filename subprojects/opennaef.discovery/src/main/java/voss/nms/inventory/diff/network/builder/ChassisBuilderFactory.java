package voss.nms.inventory.diff.network.builder;

import naef.dto.ChassisDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.ChassisCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.ChassisRenderer;

public class ChassisBuilderFactory implements BuilderFactory {
    private final ChassisDto target;
    private final ChassisRenderer renderer;
    private final String editorName;

    public ChassisBuilderFactory(ChassisDto chassis, ChassisRenderer renderer, String editorName) {
        if (Util.isAllNull(chassis, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = chassis;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    @Override
    public CommandBuilder getBuilder() {
        ChassisCommandBuilder builder;
        if (this.target == null) {
            builder = new ChassisCommandBuilder(this.renderer.getParentAbsoluteName(), editorName);
            builder.setSource(DiffCategory.DISCOVERY.name());
        } else {
            builder = new ChassisCommandBuilder(this.target, editorName);
        }
        return builder;
    }

}