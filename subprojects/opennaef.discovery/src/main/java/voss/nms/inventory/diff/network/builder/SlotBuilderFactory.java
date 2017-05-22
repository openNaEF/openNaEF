package voss.nms.inventory.diff.network.builder;

import naef.dto.SlotDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.SlotCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.SlotRenderer;

public class SlotBuilderFactory implements BuilderFactory {
    private final SlotDto target;
    private final SlotRenderer renderer;
    private final String editorName;

    public SlotBuilderFactory(SlotDto slot, SlotRenderer renderer, String editorName) {
        if (Util.isAllNull(slot, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = slot;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    @Override
    public CommandBuilder getBuilder() {
        SlotCommandBuilder builder = null;
        if (target == null) {
            builder = new SlotCommandBuilder(this.renderer.getParentAbsoluteName(), editorName);
            builder.setName(renderer.getValue(SlotRenderer.Attr.NAME));
            builder.setSource(DiffCategory.DISCOVERY.name());
        } else {
            builder = new SlotCommandBuilder(target, editorName);
            builder.setName(target.getName());
        }
        return builder;
    }

}