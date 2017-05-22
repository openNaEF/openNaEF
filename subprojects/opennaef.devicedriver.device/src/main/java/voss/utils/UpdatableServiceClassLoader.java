package voss.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class UpdatableServiceClassLoader extends ClassLoader {
    private static final String LOADER_LOG = "LoaderLog";
    private final Logger log = LoggerFactory.getLogger(UpdatableServiceClassLoader.class);
    private final Logger loaderLog = LoggerFactory.getLogger(LOADER_LOG);
    private final String libraryFileName;
    private final Map<String, byte[]> bytecodes = new HashMap<String, byte[]>();

    public UpdatableServiceClassLoader(String fileName) {
        this.libraryFileName = fileName;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytecode = bytecodes.get(name);
        if (bytecode == null) {
            throw new ClassNotFoundException();
        }
        Class<?> clazz = defineClass(name, bytecode, 0, bytecode.length);
        if (clazz != null) {
            return clazz;
        }
        throw new ClassNotFoundException();
    }

    public void initialize() {
        File file = new File(libraryFileName);
        if (file.exists()) {
            loadBytecode(file);
        }
    }

    private void loadBytecode(File file) {
        try {
            ZipFile zip = new ZipFile(file);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName().replace('/', '.');
                if (!name.contains(".class")) {
                    continue;
                }
                name = name.replace(".class", "");
                log.debug("loading class: " + name);
                InputStream zis = zip.getInputStream(entry);
                try {
                    byte[] bytecode = loadByteCode(zis, name);
                    bytecodes.put(name, bytecode);
                    log.debug("loaded class: " + name);
                    loaderLog.info("class " + name + " loaded from " + file.getAbsolutePath() + ".");
                } catch (IOException e) {
                    log.error("failed to load class: " + name + " (" + file.getAbsolutePath() + ")", e);
                    loaderLog.info("class " + name + " failed to load from " + file.getAbsolutePath() + ".");
                }
            }
            zip.close();
        } catch (ZipException e) {
            log.debug("A zip error occured.", e);
            loaderLog.error("cannot load library (zip error).");
        } catch (IOException e) {
            log.debug("An I/O error occured.", e);
            loaderLog.error("cannot load library (i/o error).");
        }
    }

    private byte[] loadByteCode(InputStream st, String className) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int b = 0;
        while ((b = st.read()) != -1) {
            bos.write(b & 0xff);
        }
        bos.close();
        return bos.toByteArray();
    }

    public static void main(String[] args) {
        UpdatableServiceClassLoader loader = new UpdatableServiceClassLoader("test/test1.zip");
        loader.initialize();

        try {
            Class<?> clazz = loader.findClass("voss.jettytest.p0.Test1");
            Field f = clazz.getDeclaredField("KEY");
            System.out.println(f.get(clazz));

            ClassLoader cl = clazz.getClassLoader();
            System.out.println(cl.getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        loader = new UpdatableServiceClassLoader("test/test2.zip");
        loader.initialize();
        try {
            Class<?> clazz = loader.findClass("voss.jettytest.p0.Test1");
            Field f = clazz.getDeclaredField("KEY");
            System.out.println(f.get(clazz));

            ClassLoader cl = clazz.getClassLoader();
            System.out.println(cl.getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}