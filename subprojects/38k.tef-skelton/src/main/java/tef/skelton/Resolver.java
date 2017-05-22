package tef.skelton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Resolver<T extends Model> {

    static class Instances {

        private final Map<String, Resolver<?>> resolversByName_ = new HashMap<String, Resolver<?>>();
        private final Map<Class<?>, Resolver<?>> resolversByType_ = new HashMap<Class<?>, Resolver<?>>();

        public synchronized Set<Resolver<?>> getResolvers() {
            return new HashSet<Resolver<?>>(resolversByName_.values());
        }

        public synchronized Resolver<?> getResolver(String name) {
            return resolversByName_.get(name);
        }

        public synchronized Resolver<?> getResolver(Class<?> type) {
            return resolversByType_.get(type);
        }

        public void installMainResolver(Resolver<?> resolver) {
            installResolver(resolver, true);
        }

        public void installSubResolver(Resolver<?> resolver) {
            installResolver(resolver, false);
        }

        private synchronized void installResolver(Resolver<?> resolver, boolean isToRegisterType) {
            if (getResolver(resolver.getName()) != null) {
                throw new IllegalArgumentException("既に同一の名前のリゾルバが登録されています: " + resolver.getName());
            }
            resolversByName_.put(resolver.getName(), resolver);

            if (isToRegisterType) {
                Class<?> type = resolver.getType();
                if (getResolver(type) != null) {
                    throw new IllegalArgumentException(
                        "型 " + SkeltonTefService.instance().uiTypeNames().getName(type)
                        + " に対するメイン リゾルバは登録済です: " + resolver.getName());
                }
                resolversByType_.put(type, resolver);
            }
        }

        public void removeResolver(Resolver<?> resolver) {
            resolversByName_.remove(resolver.getName());
            resolversByType_.remove(resolver.getType());
        }
    }

    public static abstract class SingleNameResolver<T extends Model, S extends Model>
        extends Resolver<T>
    {
        private final Class<? extends S> requiredContextType_;

        protected SingleNameResolver(Class<? extends T> type, Class<? extends S> requiredContextType) {
            this(type, resolveTypeName(type), requiredContextType);
        }

        protected SingleNameResolver(Class<? extends T> type, String name, Class<? extends S> requiredContextType) {
            super(type, name);

            requiredContextType_ = requiredContextType;
        }

        public final Class<? extends S> getRequiredContextType() {
            return requiredContextType_;
        }

        public final T resolve(Model context, String arg) throws ResolveException {
            if (requiredContextType_ != null && ! requiredContextType_.isInstance(context)) {
                throw new ResolveException(
                    "コンテキストに " + SkeltonTefService.instance().uiTypeNames().getName(requiredContextType_)
                    + " を設定してください.");
            }

            return resolveImpl((S) context, arg);
        }

        abstract protected T resolveImpl(S context, String arg) throws ResolveException;
    }

    public static abstract class ClusteringResolver<T extends Model, S extends Model>
        extends Resolver<T>
    {
        private final Class<? extends S> elementType_;

        protected ClusteringResolver(Class<? extends T> type, String name, Class<? extends S> elementType) {
            super(type, name);

            elementType_ = elementType;
        }

        public final Class<? extends S> getElementType() {
            return elementType_;
        }

        public final T resolve(List<Object> elements) throws ResolveException {
            List<S> args = new ArrayList<S>();
            for (Object element : elements) {
                if (elementType_ != null && ! elementType_.isInstance(element)) {
                    throw new ResolveException("メンバーの型が不適合です.");
                }

                args.add((S) element);
            }

            return resolveImpl(args);
        }

        abstract protected T resolveImpl(List<S> elements) throws ResolveException;
    }

    private static String resolveTypeName(Class<?> type) {
        UiTypeName typename = SkeltonTefService.instance().uiTypeNames().getByType(type);
        if (typename == null) {
            throw new IllegalArgumentException("ui type name が未登録です: " + type.getName());
        }

        return typename.name();
    }

    private final Class<? extends T> type_;
    private final String name_;

    protected Resolver(Class<? extends T> type) {
        this(type, resolveTypeName(type));
    }

    protected Resolver(Class<? extends T> type, String name) {
        if (type == null || name == null) {
            throw new IllegalArgumentException();
        }

        type_ = type;
        name_ = name;
    }

    public final Class<? extends T> getType() {
        return type_;
    }

    public final String getName() {
        return name_;
    }

    abstract public String getName(T obj);
}
