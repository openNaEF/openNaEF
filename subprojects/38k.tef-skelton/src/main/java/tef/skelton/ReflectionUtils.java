package tef.skelton;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class ReflectionUtils {

    private ReflectionUtils() {
    }

    public static List<Object> initializeStaticFinalFields(Class<?> klass) {
        List<Object> result = new ArrayList<Object>();
        for (Field f : klass.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())) {
                try {
                    f.setAccessible(true);
                    result.add(f.get(null));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }
}
