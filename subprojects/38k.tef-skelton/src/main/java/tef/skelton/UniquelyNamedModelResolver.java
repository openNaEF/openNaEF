package tef.skelton;

import tef.MVO;

public class UniquelyNamedModelResolver<T extends MVO & NamedModel>
    extends Resolver.SingleNameResolver<T, Model>
{
    private final UniquelyNamedModelHome<T> home_;

    public UniquelyNamedModelResolver(UniquelyNamedModelHome<T> home) {
        super(home.getType(), null);

        home_ = home;
    }

    @Override protected T resolveImpl(Model context, String arg) {
        return home_.getByName(arg);
    }

    @Override public String getName(T obj) {
        return obj.getName();
    }

    public UniquelyNamedModelHome<T> getHome() {
        return home_;
    }
}
