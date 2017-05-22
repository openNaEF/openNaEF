package opennaef.rest;

public class Classes {
    public static final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

    public static Class<?> forName(String className) throws ClassNotFoundException {
        // TODO primitive型のClass取得
        if (className.equals("boolean")) {
            return boolean.class;
        } else {
            return Class.forName(className);
        }
    }

    /**
     * classString で指定したクラスが assignable と同じ型、もしくは派生している型であれば Class を返す.
     * そうでなければ ClassNotFoundException を返す.
     *
     * @param classString
     * @param assignable
     * @param <C>
     * @return
     * @throws ClassNotFoundException
     */
    public static <C> Class<? extends C> getClass(String classString, Class<C> assignable) throws ClassNotFoundException {
        if (assignable == null) throw new IllegalArgumentException("assignable class is null.");
        Class<?> clazz = systemClassLoader.loadClass(classString);
        if (assignable.isAssignableFrom(clazz)) {
            return (Class<? extends C>) clazz;
        } else {
            throw new ClassNotFoundException("type mismatch. " + classString + " <> " + assignable.getName());
        }
    }
}
