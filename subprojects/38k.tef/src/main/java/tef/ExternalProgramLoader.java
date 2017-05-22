package tef;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ExternalProgramLoader {

    private static class ExternalProgramClassLoader extends ClassLoader {

        ExternalProgramClassLoader(ClassLoader parentClassLoader) {
            super(parentClassLoader);
        }

        public Class findClass(String name) throws ClassNotFoundException {
            File classDefFile
                    = new File
                    (TefService.instance().getWorkingDirectory(),
                            name.replace('.', '\\') + ".class");
            if (!classDefFile.exists() || !classDefFile.isFile()) {
                throw new ClassNotFoundException(name);
            }

            byte[] classDef;
            try {
                classDef = lib38k.io.IoUtils.readFile(classDefFile);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }

            return defineClass(name, classDef, 0, classDef.length);
        }
    }

    private String externalProgramClassName_;
    private String methodName_;

    private Object externalProgramObject_;
    private Method method_;

    public ExternalProgramLoader
            (String externalProgramClassName, String methodName)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        externalProgramClassName_ = externalProgramClassName;
        methodName_ = methodName;

        load();
    }

    private void load()
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        ClassLoader classLoader = new ExternalProgramClassLoader(getClass().getClassLoader());
        Class externalProgramClass = classLoader.loadClass(externalProgramClassName_);

        if (externalProgramClass == null) {
            throw new ClassNotFoundException(externalProgramClassName_);
        }

        Constructor externalProgramConstructor
                = externalProgramClass.getDeclaredConstructor(new Class[0]);
        externalProgramConstructor.setAccessible(true);
        externalProgramObject_
                = externalProgramConstructor.newInstance(new Object[0]);
        method_ = externalProgramClass.getDeclaredMethod(methodName_, new Class[0]);
        method_.setAccessible(true);
    }

    public Object getExternalProgramObject() {
        return externalProgramObject_;
    }

    public Method getMethod() {
        return method_;
    }
}
