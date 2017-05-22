package opennaef.rest.api.config.api;

import voss.core.server.builder.CommandBuilder;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Builder の Method の設定
 */
public class MethodConfig {
    public final Class<? extends CommandBuilder> builderClass;
    public final String methodName;
    public final Method method;
    public final String description;
    public final List<ArgConfig> args;    // 順序が守られていなければならない


    public MethodConfig(
            Class<? extends CommandBuilder> builderClass,
            String methodName,
            List<ArgConfig> args,
            String description
    ) throws NoSuchMethodException {
        this.methodName = methodName;
        this.builderClass = builderClass;
        this.description = description;
        this.args = Collections.unmodifiableList(args);

        Class<?>[] argTypes = new Class[args.size()];
        for (int i = 0; i < args.size(); i++) {
            argTypes[i] = args.get(i).paramClass;
        }
        method = builderClass.getMethod(methodName, argTypes);
    }
}
