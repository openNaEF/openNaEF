package opennaef.rest.api.config.api;

import voss.core.server.builder.CommandBuilder;

import java.util.Collections;
import java.util.List;

/**
 * Builderの設定
 */
public class BuilderConfig {
    public final Class<? extends CommandBuilder> builder;
    public final ConstructorConfig createConstructor;
    public final ConstructorConfig updateConstructor;
    public final ConstructorConfig deleteConstructor;
    public final List<MethodConfig> methods;

    public BuilderConfig(
            Class<? extends CommandBuilder> builder,
            ConstructorConfig createConstructor,
            ConstructorConfig updateConstructor,
            ConstructorConfig deleteConstructor,
            List<MethodConfig> methods) {
        this.builder = builder;
        this.createConstructor = createConstructor;
        this.updateConstructor = updateConstructor;
        this.deleteConstructor = deleteConstructor;
        this.methods = Collections.unmodifiableList(methods);
    }
}
