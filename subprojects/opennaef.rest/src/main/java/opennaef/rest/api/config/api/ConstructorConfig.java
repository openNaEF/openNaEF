package opennaef.rest.api.config.api;

import voss.core.server.builder.CommandBuilder;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

/**
 * Builder の Constructor の設定
 */
public class ConstructorConfig {
    public final Class<? extends CommandBuilder> builderClass;
    public final Type type;
    public final Constructor<? extends CommandBuilder> constructor;
    public final String description;
    public final List<ArgConfig> args;    // 順序が守られていなければならない

    public ConstructorConfig(
            Class<? extends CommandBuilder> builderClass,
            Type type,
            List<ArgConfig> args,
            String description
    ) throws NoSuchMethodException {
        this.builderClass = builderClass;
        this.type = type;
        this.description = description;
        this.args = Collections.unmodifiableList(args);

        Class<?>[] argTypes = new Class[args.size()];
        for (int i = 0; i < args.size(); i++) {
            argTypes[i] = args.get(i).paramClass;
        }
        constructor = builderClass.getConstructor(argTypes);
    }

    public enum Type {
        create, update, delete;
    }
}
