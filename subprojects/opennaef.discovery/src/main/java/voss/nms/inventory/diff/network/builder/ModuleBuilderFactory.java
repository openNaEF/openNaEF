package voss.nms.inventory.diff.network.builder;

import naef.dto.ModuleDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.ModuleCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.ModuleRenderer;

import static voss.nms.inventory.diff.network.analyzer.ModuleRenderer.TargetAttribute.MODULE_TYPE;

public class ModuleBuilderFactory implements BuilderFactory {
    private final ModuleDto target;
    private final ModuleRenderer renderer;
    private final String editorName;

    public ModuleBuilderFactory(ModuleDto module, ModuleRenderer renderer, String editorName) {
        if (Util.isAllNull(module, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = module;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    @Override
    public CommandBuilder getBuilder() {
        ModuleCommandBuilder builder = null;
        if (target == null) {
            builder = new ModuleCommandBuilder(this.renderer.getParentAbsoluteName(), editorName);
            String moduleType = renderer.getValue(MODULE_TYPE.getAttributeName());
            builder.setModelTypeName(moduleType);
            builder.setSource(DiffCategory.DISCOVERY.name());
        } else {
            builder = new ModuleCommandBuilder(this.target, editorName);
        }
        return builder;
    }

}