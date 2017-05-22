package tef.skelton.dto;

import lib38k.xml.Xml;
import tef.TefService;
import tef.skelton.ConfigurationException;
import tef.skelton.KnownRuntimeException;
import tef.skelton.Model;
import tef.skelton.SkeltonUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MvoDtoMapping {

    private static void log(String str) {
        TefService.instance().logMessage(str);
    }

    /**
     * デフォルト設定値をオーバーライドする定義ができなければならないため, 
     * 重複検査は設定ファイル内での重複のみを確認.
     */
    public static void setupByConfigurationFile(MvoDtoMapping instance) {
        log("[mvo/dto mapping]initializing by configuration file...");
        File mappingDefinitionFile = new File(TefService.instance().getConfigsDirectory(), "MvoDtoMapping.xml");
        if (! mappingDefinitionFile.isFile()) {
            log("[mvo/dto mapping]no configuration file.");
            return;
        }

        Xml.Elem root = new Xml(mappingDefinitionFile).getRoot();
        if (! root.getName().equals("mvo-dto-mapping")) {
            throw new ConfigurationException("mvo dto mapping の root 名が不正です.");
        }

        Map<Class<? extends Model>, Class<? extends EntityDto>> mapping
            = new HashMap<Class<? extends Model>, Class<? extends EntityDto>>();
        for (Xml.Elem entry : root.getSubElems("map")) {
            Class<? extends Model> mvoClass = SkeltonUtils.classForName(
                ERROR_MESSAGE_PREFIX,
                Model.class,
                SkeltonUtils.getXmlAttribute(ERROR_MESSAGE_PREFIX, entry, "mvo"));
            Class<? extends EntityDto> dtoClass = SkeltonUtils.classForName(
                ERROR_MESSAGE_PREFIX,
                EntityDto.class,
                SkeltonUtils.getXmlAttribute(ERROR_MESSAGE_PREFIX, entry, "dto"));

            if (mapping.containsKey(mvoClass)) {
                throw new IllegalStateException(
                    generateDuplicationErrorMessage(mvoClass, mapping.get(mvoClass), dtoClass));
            };

            mapping.put(mvoClass, dtoClass);
        }

        log("[mvo/dto mapping]" + mapping.size() + " entries.");
        for (Class<? extends Model> mvoClass : mapping.keySet()) {
            Class<? extends EntityDto> dtoClass = mapping.get(mvoClass);
            instance.mapWithoutDuplicationCheck(mvoClass, dtoClass);

            log("[mvo/dto mapping]" + mvoClass.getName() + " -> " + dtoClass.getName());
        }
    }

    private static String generateDuplicationErrorMessage(
        Class<? extends Model> mvoClass,
        Class<? extends EntityDto> duplication1,
        Class<? extends EntityDto> duplication2)
    {
        return "duplicated mvo-dto mapping definition found: "
            + mvoClass.getName()
            + " -> {"
            + duplication1.getName()
            + ", "
            + duplication2.getName()
            + "}";
    }

    public static class NoMappingDefinedException extends KnownRuntimeException {

        public NoMappingDefinedException(String message) {
            super(message);
        }
    }

    private static final String ERROR_MESSAGE_PREFIX = "mvo-dto-mapping";

    private Map<Class<? extends Model>, Class<? extends EntityDto>> mvoToDtoMap_;
    private Map<Class<? extends EntityDto>, Class<? extends Model>> dtoToMvoMap_;

    public MvoDtoMapping() {
    }

    public synchronized void initialize() {
        if (mvoToDtoMap_ != null || dtoToMvoMap_ != null) {
            throw new IllegalStateException();
        }

        mvoToDtoMap_ = new HashMap<Class<? extends Model>, Class<? extends EntityDto>>();
        dtoToMvoMap_ = new HashMap<Class<? extends EntityDto>, Class<? extends Model>>();
    }

    public synchronized void map(Class<? extends Model> mvoClass, Class<? extends EntityDto> dtoClass) {
        if (mvoToDtoMap_.get(mvoClass) != null) {
            throw new IllegalStateException(
                generateDuplicationErrorMessage(mvoClass, mvoToDtoMap_.get(mvoClass), dtoClass));
        }

        mapWithoutDuplicationCheck(mvoClass, dtoClass);
    }

    public synchronized void mapWithoutDuplicationCheck(
        Class<? extends Model> mvoClass, Class<? extends EntityDto> dtoClass)
    {
        mvoToDtoMap_.put(mvoClass, (Class<? extends EntityDto>) dtoClass);
        dtoToMvoMap_.put((Class<? extends EntityDto>) dtoClass, mvoClass);
    }

    public synchronized boolean hasDtoMapping(Class<? extends Model> mvoClass) {
        return mvoToDtoMap_.get(mvoClass) != null
            || selectMostSuitableDtoClass(mvoClass) != null;
    }

    public synchronized Class<? extends EntityDto> getDtoClass(Class<?> klass) {
        if (! Model.class.isAssignableFrom(klass)) {
            return null;
        }

        Class<? extends Model> mvoClass = klass.asSubclass(Model.class);

        Class<? extends EntityDto> result = mvoToDtoMap_.get(mvoClass);
        if (result == null) {
            result = selectMostSuitableDtoClass(mvoClass);
            if (result == null) {
                throw new NoMappingDefinedException("no mvo class to dto class mapping: " + mvoClass.getName());
            } else {
                TefService.instance().logMessage("[mvo/dto map]" + mvoClass.getName() + "/" + result.getName());
                mvoToDtoMap_.put(mvoClass, result);
            }
        }
        return result;
    }

    private synchronized Class<? extends EntityDto> selectMostSuitableDtoClass(Class<? extends Model> mvoClass) {
        Class<? extends Model> mostSuitableMvoClass = null;
        for (Class<? extends Model> mappedMvoClass : mvoToDtoMap_.keySet()) {
            if (mappedMvoClass.isAssignableFrom(mvoClass)
                && (mostSuitableMvoClass == null || mostSuitableMvoClass.isAssignableFrom(mappedMvoClass)))
            {
                mostSuitableMvoClass = mappedMvoClass;
            }
        }
        return mvoToDtoMap_.get(mostSuitableMvoClass);
    }

    public synchronized Class<? extends Model> getMvoClass(Class<? extends EntityDto> dtoClass) {
        Class<? extends Model> result = dtoToMvoMap_.get(dtoClass);
        if (result == null) {
            result = selectMostSuitableMvoClass(dtoClass);
            if (result == null) {
                throw new IllegalStateException("no dto class to mvo class mapping: " + dtoClass.getName());
            } else {
                dtoToMvoMap_.put(dtoClass, result);
            }
        }
        return result;
    }

    private synchronized Class<? extends Model> selectMostSuitableMvoClass(Class<? extends EntityDto> dtoClass) {
        Class<? extends EntityDto> mostSuitableDtoClass = null;
        for (Class<? extends EntityDto> mappedDtoClass : dtoToMvoMap_.keySet()) {
            if (mappedDtoClass.isAssignableFrom(dtoClass)
                && (mostSuitableDtoClass == null || mappedDtoClass.isAssignableFrom(mostSuitableDtoClass)))
            {
                mostSuitableDtoClass = mappedDtoClass;
            }
        }
        return dtoToMvoMap_.get(mostSuitableDtoClass);
    }
}
