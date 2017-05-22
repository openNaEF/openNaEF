package opennaef.rest.api.config;

import opennaef.rest.Classes;
import opennaef.rest.api.config.api.*;
import opennaef.rest.api.helper.BuildHelper;
import opennaef.rest.api.helper.DefaultBuildHelper;
import org.yaml.snakeyaml.Yaml;
import tef.skelton.Model;
import tef.skelton.dto.Dto;
import voss.core.server.builder.CommandBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Naef command builder Configuration
 */
public class CmdBuilderMapping extends Config<Map<String, ApiConfig>> {
    private static CmdBuilderMapping _instance = new CmdBuilderMapping();
    public static final String CONFIG_FILE = "cmd-builder-mapping.yaml";

    public CmdBuilderMapping() {
        super(CONFIG_FILE, (Class<Map<String, ApiConfig>>) (Class<?>) Map.class);
    }

    public static CmdBuilderMapping instance() {
        return CmdBuilderMapping._instance;
    }

    @Override
    protected synchronized Map<String, ApiConfig> loadInner(Yaml yaml, InputStream is) throws ConfigurationException {
        Map<String, ?> flesh = yaml.loadAs(is, Map.class);

        Map<String, ApiConfig> confs = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : flesh.entrySet()) {
            String apiName = entry.getKey();
            Object raw = entry.getValue();
            ApiConfig conf = createConfig(apiName, raw);
            confs.put(apiName, conf);
        }
        return confs;
    }

    public ApiConfig content(String apiContext) throws ConfigurationException {
        load();
        return config().get(apiContext);
    }


    public List<ApiConfig> contents(Class<?> clazz) {
        try {
            load();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        List<ApiConfig> configs = new ArrayList<>();
        for (Map.Entry<String, ApiConfig> entry : config().entrySet()) {
            ApiConfig value = entry.getValue();
            if (value.mvoClasses.contains(clazz) || value.dtoClasses.contains(clazz)) {
                configs.add(value);
            }
        }
        return configs;
    }

    public static void main(String[] args) throws Exception {
        CmdBuilderMapping._instance.load();
    }

    private static ApiConfig createConfig(String apiName, Object raw) throws ConfigurationException {
        if (raw instanceof String) {
            // 別ファイルを読み込む
            Path path = Paths.get(CONFIG_DIR, (String) raw);
            if (!Files.isRegularFile(path))
                throw new ConfigurationException(CONFIG_FILE + "ロード失敗. ファイル指定が不正です. " + apiName);

            try (InputStream is = Files.newInputStream(path)) {
                return createConfig(apiName, new Yaml().loadAs(is, Map.class));
            } catch (IOException ioe) {
                throw new ConfigurationException("設定ファイルの読み込みに失敗しました. file: " + path.getFileName(), ioe);
            }
        }
        if (!(raw instanceof Map)) {
            throw new ConfigurationException(CONFIG_FILE + "ロード失敗. 形式が不正です. " + apiName);
        }

        Map<String, Object> attrs = (Map<String, Object>) raw;
        try {
            return new ApiConfig(
                    apiName,
                    getTargetMvos(attrs),
                    getTargetDtos(attrs),
                    getHelper(attrs),
                    getBuilderConfig(attrs));
        } catch (ClassCastException | ClassNotFoundException | NoSuchMethodException e) {
            throw new ConfigurationException(e);
        }
    }


    private static Set<Class<? extends Model>> getTargetMvos(Map<String, Object> attr) throws ClassCastException, ClassNotFoundException {
        Set<Class<? extends Model>> mvoClasses = new HashSet<>();
        Collection<String> mvoClassStrings = (Collection<String>) attr.get("mvos");
        for (String classString : mvoClassStrings) {
            mvoClasses.add(Classes.getClass(classString, Model.class));
        }
        return mvoClasses;
    }

    private static Set<Class<? extends Dto>> getTargetDtos(Map<String, Object> attr) throws ClassCastException, ClassNotFoundException {
        Set<Class<? extends Dto>> dtoClasses = new HashSet<>();
        Collection<String> dtoClassString = (Collection<String>) attr.get("dtos");
        for (String classString : dtoClassString) {
            dtoClasses.add(Classes.getClass(classString, Dto.class));
        }
        return dtoClasses;

    }

    private static BuildHelper getHelper(Map<String, Object> attr) throws ClassNotFoundException, ConfigurationException {
        String helperClassName = (String) attr.get("helper");
        if (helperClassName == null) return DefaultBuildHelper.INSTANCE;

        Class<?> helperClass = Class.forName(helperClassName);
        if (BuildHelper.class.isAssignableFrom(helperClass)) {
            try {
                return (BuildHelper) helperClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new ConfigurationException(CONFIG_FILE + "ロード失敗. helper指定が不正です. " + helperClassName);
            }
        }
        return DefaultBuildHelper.INSTANCE;
    }

    private static BuilderConfig getBuilderConfig(Map<String, Object> attr) throws ClassNotFoundException, NoSuchMethodException {
        Map<String, Object> builderAttr = (Map<String, Object>) attr.get("builder");
        if (builderAttr == null) return null;

        String builderClassName = (String) builderAttr.get("class-name");
        Map<String, Map<String, Object>> constructorsAttr = (Map<String, Map<String, Object>>) builderAttr.get("constructors");
        Map<String, Map<String, Object>> methodsAttr = (Map<String, Map<String, Object>>) builderAttr.get("methods");

        Class<? extends CommandBuilder> builderClass = Classes.getClass(builderClassName, CommandBuilder.class);
        ConstructorConfig createConstructor = getConstructorConfig(builderClass, constructorsAttr, ConstructorConfig.Type.create);
        ConstructorConfig updateConstructor = getConstructorConfig(builderClass, constructorsAttr, ConstructorConfig.Type.update);
        ConstructorConfig deleteConstructor = getConstructorConfig(builderClass, constructorsAttr, ConstructorConfig.Type.delete);

        List<MethodConfig> methods = getMethodConfigs(builderClass, methodsAttr);

        return new BuilderConfig(builderClass, createConstructor, updateConstructor, deleteConstructor, methods);
    }


    private static ConstructorConfig getConstructorConfig(
            Class<? extends CommandBuilder> builderClass,
            Map<String, Map<String, Object>> constructorsAttr,
            ConstructorConfig.Type type
    ) throws ClassNotFoundException, NoSuchMethodException {
        if (builderClass == null) return null;
        if (constructorsAttr == null) return null;

        Map<String, Object> constructorAttr = constructorsAttr.get(type.name());

        String description = (String) constructorAttr.get("description");
        List<Map<String, String>> rawArgs = (List<Map<String, String>>) constructorAttr.get("args");

        List<ArgConfig> args = new ArrayList<>();
        for (Map<String, String> ra : rawArgs) {
            ArgConfig arg = new ArgConfig(ra.get("java-class"), ra.get("attribute-name"));
            args.add(arg);
        }
        return new ConstructorConfig(builderClass, type, args, description);
    }

    private static List<MethodConfig> getMethodConfigs(
            Class<? extends CommandBuilder> builderClass,
            Map<String, Map<String, Object>> methodsAttr
    ) throws ClassNotFoundException, NoSuchMethodException {
        List<MethodConfig> configs = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : methodsAttr.entrySet()) {
            String methodName = entry.getKey();
            Map<String, Object> methodArgs = entry.getValue();
            String description = (String) methodArgs.get("description");
            List<Map<String, String>> rawArgs = (List<Map<String, String>>) methodArgs.get("args");

            List<ArgConfig> args = new ArrayList<>();
            for (Map<String, String> ra : rawArgs) {
                ArgConfig arg = new ArgConfig(ra.get("java-class"), ra.get("attribute-name"));
                args.add(arg);
            }
            configs.add(new MethodConfig(builderClass, methodName, args, description));
        }
        return configs;
    }
}
