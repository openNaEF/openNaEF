package tef.ui;

public class ClassNameMapper {

    public static interface ClassNameResolver {

        public Class<?> resolveClass(String name) throws ClassNotFoundException;
    }

    public static interface ClassNameRenderer {
        public String getRenderedClassName(Class<?> targetClass);
    }

    private static ClassNameResolver resolver__;
    private static ClassNameRenderer renderer__;

    private ClassNameMapper() {
    }

    public static void clear() {
        resolver__ = null;
        renderer__ = null;
    }

    public static void setClassNameResolver(ClassNameResolver resolver) {
        resolver__ = resolver;
    }

    public static Class<?> resolveClass(String name) {
        try {
            if (resolver__ != null) {
                Class<?> resolvedClass = resolver__.resolveClass(name);
                if (resolvedClass != null) {
                    return resolvedClass;
                }
            }

            return Class.forName(name);
        } catch (ClassNotFoundException cnfe) {
            return null;
        }
    }

    public static void setClassNameRenderer(ClassNameRenderer renderer) {
        renderer__ = renderer;
    }

    public static String getRenderedClassName(Class<?> targetClass) {
        if (renderer__ != null) {
            String renderedClassName = renderer__.getRenderedClassName(targetClass);
            if (renderedClassName != null) {
                return renderedClassName;
            }
        }

        return targetClass.getName();
    }
}
