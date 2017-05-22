package opennaef.rest.api.spawner.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 *
 */
public class Converts {
    private static final Logger log = LoggerFactory.getLogger(Converts.class);
    private static final Converts _instance = new Converts();
    private final List<ValueConverter<?>> _converters = new ArrayList<>();

    private Converts() {
        init();
    }

    private static Converts instance() {
        return Converts._instance;
    }

    /**
     * このパッケージ以下の @Converter を読み込む
     */
    private void init() {
        log.debug("converter init start");
        List<ValueConverter<?>> converters = find().stream()
                .filter(clazz -> (
                        clazz.getDeclaredAnnotation(Converter.class) != null
                                && ValueConverter.class.isAssignableFrom(clazz)))
                .map(clazz -> {
                    try {
                        log.debug("install " + clazz.getName());
                        return (ValueConverter<?>) clazz.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .filter(instance -> instance != null)
                .collect(Collectors.toList());
        converters
                .sort(Comparator.comparing(instance -> {
                    Converter anno = instance.getClass().getDeclaredAnnotation(Converter.class);
                    return anno.priority();
                }).reversed());
        _converters.addAll(converters);
    }

    /**
     * value を変換するConverterを返す
     *
     * @param value
     * @return ValueConverter
     * @throws ClassNotFoundException Converterが見つからなかった
     */
    public static ValueConverter<?> getConverter(final Object value) throws ClassNotFoundException {
        Optional<ValueConverter<?>> converter = instance()._converters.stream()
                .filter(conv -> conv.accept(value))
                .findFirst();
        if (converter.isPresent()) {
            return converter.get();
        } else {
            throw new ClassNotFoundException(value.getClass().getSimpleName() + "に対応するコンバーターが存在しません 。");
        }
    }

    /**
     * opennaef.rest.api.spawner.converter 配下のクラスを取得する
     */
    private static List<Class<?>> find() {
        log.debug("find: opennaef.rest.api.spawner.converter");
        return find("opennaef.rest.api.spawner.converter");
    }

    private final static String CLASS_SUFFIX = ".class";

    private static List<Class<?>> find(final String scannedPackage) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String scannedPath = toResourceName(scannedPackage);
        Enumeration<URL> resources;
        try {
            resources = classLoader.getResources(scannedPath);
        } catch (IOException e) {
            throw new IllegalArgumentException("package 指定ミス", e);
        }

        return Collections.list(resources).stream()
                .map(res -> {
                    log.debug("protocol: " + res.getProtocol());
                    log.debug("directory: " + res.getPath());
                    if ("jar".equals(res.getProtocol())) {
                        return findJar(res, scannedPackage);
                    }
                    return findFile(new File(res.getFile()), scannedPackage);
                })
                .collect(ArrayList::new, List::addAll, List::addAll);
    }

    private static List<Class<?>> findJar(final URL url, final String scannedPackage) {
        log.debug("find from jar");
        String scannedPath = toResourceName(scannedPackage);
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            JarURLConnection jarConn = (JarURLConnection) url.openConnection();
            JarFile jar = jarConn.getJarFile();
            Enumeration<JarEntry> entries = jar.entries();
            return Collections.list(entries).stream()
                    .map(entry -> {
                        String name = entry.getName();
                        log.debug("finding: " + name);
                        if (name.startsWith(scannedPath) && isClass(name)) {
                            try {
                                return classLoader.loadClass(toClassName(name));
                            } catch (ClassNotFoundException e) {
                                log.warn("converter 読み込み失敗. " + entry.getName());
                            }
                        }
                        return null;
                    })
                    .filter(clazz -> clazz != null)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("converter 読み込み失敗. ", e);
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private static List<Class<?>> findFile(final File file, final String scannedPackage) {
        log.debug("find from file");
        List<Class<?>> classes = new LinkedList<>();
        String fileName = file.getName();
        log.trace("finding: " + fileName);
        if (file.isDirectory()) {
            for (File nestedFile : file.listFiles()) {
                classes.addAll(findFile(nestedFile, scannedPackage));
            }
        } else if (isClass(fileName) && !fileName.contains("$")) {
            // 無名クラスは無視する
            String className = trimClassSuffix(fileName);
            try {
                classes.add(Class.forName(scannedPackage + "." + className));
            } catch (ClassNotFoundException ignore) {
            }
        }
        return classes;
    }

    private static boolean isClass(String fileName) {
        return fileName.endsWith(CLASS_SUFFIX);
    }

    private static String trimClassSuffix(String name) {
        if (name.contains(CLASS_SUFFIX)) {
            int endIndex = name.lastIndexOf(CLASS_SUFFIX);
            return name.substring(0, endIndex);
        }
        return name;
    }

    private static String toClassName(String resourceName) {
        return trimClassSuffix(resourceName).replace('/', '.');
    }

    private static String toResourceName(String className) {
        return trimClassSuffix(className).replace('.', '/');
    }
}