package tef.logic;

import lib38k.plugin.PluginUtils;
import tef.TefFileUtils;
import tef.TefService;
import tef.TefServiceConfig;
import tef.TransactionContext;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.zip.ZipFile;

/*
 * たとえば以下の型定義が合ったとする:
 * - interface A extends BusinessLogic
 * - interface B extends A
 * - class X implements A
 * - class Y extends X implements B
 *
 * そして
 * - jar file に A, B, X, Y が含まれる
 * - required classes は { A } で BusinessLogicPool を構成する (B は必須ではない)
 * とする。
 *
 * この時 mapping (pool) は以下のように構成される。
 * - A -> Y
 * - B -> Y
 *
 * つまり、BusinessLogicPool.get() を A.class および B.class どちらで呼び出しても Y が返される。
 */
public class BusinessLogicPool {

    private final TefService tefService_;
    private List<File> logicJarFilePaths_;
    private Map<Class<?>, BusinessLogic> pool_;
    private Set<Class<?>> requiredLogicClasses_;

    public BusinessLogicPool(TefService tefService, Class<?>[] requiredLogicClasses) {
        tefService_ = tefService;

        TefServiceConfig.BusinessLogicConfig config
                = tefService_.getTefServiceConfig().businessLogicConfig;

        requiredLogicClasses_ = new HashSet<Class<?>>();
        for (Class<?> logicClass : requiredLogicClasses) {
            if (!BusinessLogic.class.isAssignableFrom(logicClass)) {
                throw new IllegalArgumentException
                        (logicClass.getName() + " is not a logic class.");
            }

            requiredLogicClasses_.add(logicClass);
        }

        List<File> files = new ArrayList<File>();
        for (String filename : config.logicJarFileName.split(";")) {
            files.add
                    (TefFileUtils.isAbsolutePath(filename)
                            ? new File(filename)
                            : new File(tefService_.getWorkingDirectory(), filename));
        }
        logicJarFilePaths_ = files;
    }

    public void init() {
        updatePool();
    }

    public void updatePool() {
        TefService.RunningMode runningMode = tefService_.getRunningMode();
        if (runningMode == TefService.RunningMode.MASTER) {
            TransactionContext.beginWriteTransaction(null);
        } else {
            TransactionContext.beginReadTransaction(null);
        }

        try {
            synchronized (this) {
                StringBuffer versionsStr = new StringBuffer();
                List<ZipFile> logicJarFiles = getLogicJarFiles();
                for (ZipFile jarFile : logicJarFiles) {
                    versionsStr.append(versionsStr.length() > 0 ? "," : "");
                    versionsStr.append
                            (jarFile.getName() + ":" + PluginUtils.getVersion(jarFile));
                }
                List<Class<?>> classes;
                try {
                    classes = PluginUtils
                            .loadClasses(BusinessLogic.class.getClassLoader(), logicJarFiles);
                } finally {
                    for (ZipFile logicsJar : logicJarFiles) {
                        try {
                            logicsJar.close();
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                }

                Map<Class<?>, BusinessLogic> newPool;
                try {
                    newPool = getNewLogicPool(classes);
                } catch (Exception e) {
                    throw new Error(e);
                }

                BusinessLogic[] newLogics
                        = (BusinessLogic[]) newPool.values().toArray(new BusinessLogic[0]);
                initializeLogicObjects(newLogics);

                if (pool_ != null) {
                    BusinessLogic[] oldLogics = pool_.values().toArray(new BusinessLogic[0]);
                    disposeLogicObjects(oldLogics);
                }

                pool_ = newPool;

                logNewPool(versionsStr.toString());
            }
        } finally {
            TransactionContext.close();
        }
    }

    private List<ZipFile> getLogicJarFiles() {
        List<ZipFile> result = new ArrayList<ZipFile>();
        for (File file : logicJarFilePaths_) {
            if (!file.exists()) {
                throw new RuntimeException("no logic file: '" + file.getAbsolutePath() + "'");
            }

            ZipFile zipfile;
            try {
                result.add(new ZipFile(file));
            } catch (IOException ioe) {
                throw new RuntimeException("zip file error: " + file.getAbsolutePath(), ioe);
            }
        }
        return result;
    }

    private Map<Class<?>, BusinessLogic> getNewLogicPool(List<Class<?>> classes)
            throws InstantiationException, IllegalAccessException {
        Map<Class<?>, Class<?>> logicTypeToLogicImplementorClasses
                = new HashMap<Class<?>, Class<?>>();
        for (Class<?> clazz : classes) {
            if (Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            Set<Class<?>> implementingLogicTypes = getImplementingLogicTypes(clazz);
            if (implementingLogicTypes.size() == 0) {
                continue;
            }

            for (Class<?> logicType : implementingLogicTypes) {
                Class<?> currentLogicImplementorClass
                        = logicTypeToLogicImplementorClasses.get(logicType);
                if (currentLogicImplementorClass == null) {
                    logicTypeToLogicImplementorClasses.put(logicType, clazz);
                } else {
                    Class<?> moreSpecificType
                            = selectMoreSpecificType(clazz, currentLogicImplementorClass);
                    if (moreSpecificType == null) {
                        throw new IllegalStateException
                                ("duplicated logic objects: "
                                        + logicType.getName());
                    }

                    logicTypeToLogicImplementorClasses.put(logicType, moreSpecificType);
                }
            }
        }

        Map<Class<?>, BusinessLogic> newPool = new HashMap<Class<?>, BusinessLogic>();
        for (Class<?> logicType : logicTypeToLogicImplementorClasses.keySet()) {
            Class<?> implementorClass = logicTypeToLogicImplementorClasses.get(logicType);
            newPool.put(logicType, (BusinessLogic) implementorClass.newInstance());
        }

        checkLogicSet(newPool);

        return newPool;
    }

    private Class<?> selectMoreSpecificType(Class<?> clazz1, Class<?> clazz2) {
        if (clazz1.isAssignableFrom(clazz2)) {
            return clazz2;
        } else if (clazz2.isAssignableFrom(clazz1)) {
            return clazz1;
        } else {
            return null;
        }
    }

    private Set<Class<?>> getImplementingLogicTypes(Class<?> logicClassImpl) {
        Set<Class<?>> result = new HashSet<Class<?>>();

        for (Class<?> implementingInterface : logicClassImpl.getInterfaces()) {
            if (implementingInterface != BusinessLogic.class
                    && BusinessLogic.class.isAssignableFrom(implementingInterface)) {
                result.add(implementingInterface);
                result.addAll(getLogicSuperinterfaces(implementingInterface));
            }
        }

        if (logicClassImpl.getSuperclass() != null) {
            result.addAll(getImplementingLogicTypes(logicClassImpl.getSuperclass()));
        }

        return result;
    }

    private Set<Class<?>> getLogicSuperinterfaces(Class<?> interfaceType) {
        Set<Class<?>> result = new HashSet<Class<?>>();
        for (Class<?> superInterface : interfaceType.getInterfaces()) {
            if (superInterface != BusinessLogic.class
                    && BusinessLogic.class.isAssignableFrom(superInterface)) {
                result.add(superInterface);
                result.addAll(getLogicSuperinterfaces(superInterface));
            }
        }
        return result;
    }

    private void checkLogicSet(Map<Class<?>, BusinessLogic> newPool) {
        for (Class<?> requiredLogicClass : requiredLogicClasses_) {
            if (newPool.get(requiredLogicClass) == null) {
                throw new RuntimeException
                        ("no logic object found: " + requiredLogicClass.getName());
            }
        }
    }

    private void initializeLogicObjects(BusinessLogic[] logicObjects) {
        Throwable error = null;
        for (int i = 0; i < logicObjects.length; i++) {
            try {
                logicObjects[i].initialize();
            } catch (Throwable t) {
                error = t;

                logErr
                        ("failed to initialize logic object: "
                                        + logicObjects[i].getClass().getName(),
                                t);

                BusinessLogic[] initializedLogicObjects
                        = new BusinessLogic[i];
                System.arraycopy
                        (logicObjects, 0, initializedLogicObjects, 0,
                                initializedLogicObjects.length);
                disposeLogicObjects(initializedLogicObjects);
            }
        }

        if (error != null) {
            throw new Error(error);
        }
    }

    private Throwable disposeLogicObjects(BusinessLogic[] logicObjects) {
        Throwable error = null;
        for (int i = 0; i < logicObjects.length; i++) {
            try {
                logicObjects[i].dispose();
            } catch (Throwable t) {
                error = t;
                logErr
                        ("failed to dispose logic object: "
                                        + logicObjects[i].getClass().getName(),
                                t);
            }
        }

        return error;
    }

    private synchronized void logNewPool(String version) {
        SortedSet<Class<?>> poolKeys = new TreeSet<Class<?>>(new Comparator<Class<?>>() {

            public int compare(Class<?> o1, Class<?> o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        poolKeys.addAll(pool_.keySet());

        List<String> logicObjectNames = new ArrayList<String>();
        for (Class<?> logicType : poolKeys) {
            BusinessLogic logicObject = pool_.get(logicType);
            logicObjectNames.add(logicType.getName() + "\t" + logicObject.getClass().getName());
        }
        logMessage
                ("updated:" + version + ", " + pool_.keySet().size() + " logics.",
                        logicObjectNames);
    }

    private synchronized void logMessage(String message, List<String> extraMessages) {
        tefService_.logMessage("[logic]" + message, extraMessages);
    }

    private synchronized void logErr(String message, Throwable t) {
        tefService_.logError("[logic]" + message, t);
    }

    public synchronized <T extends BusinessLogic> T getLogic(Class<T> logicClass) {
        if (!logicClass.isInterface()) {
            throw new IllegalArgumentException("argument type is not an interface.");
        }

        return (T) pool_.get(logicClass);
    }
}
