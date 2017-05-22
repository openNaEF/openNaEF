package opennaef.rest.api.config;

import org.yaml.snakeyaml.Yaml;
import tef.MVO;
import tef.skelton.dto.EntityDto;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MVO, DTOの属性名をrestful apiで使いやすいように、変換、統一する
 * <p>
 * config/attribute-name-mapping.yamlを読み込む
 * attr-nameの重複があった場合はExceptionを投げる
 */
public class AttrNameMapping extends Config<Map<String, AttrNameMapping.Mapping>> {
    private static AttrNameMapping _instance = new AttrNameMapping();
    public static final String CONFIG_FILE = "attribute-name-mapping.yaml";

    public AttrNameMapping() {
        // FIXME 型の渡し方
        //noinspection unchecked
        super(CONFIG_FILE, (Class<Map<String, Mapping>>) (Class<?>) Map.class);
    }

    public static AttrNameMapping instance() {
        return AttrNameMapping._instance;
    }

    @Override
    protected synchronized Map<String, Mapping> loadInner(Yaml yaml, InputStream is) throws ConfigurationException {
        Map<String, Map<String, Object>> flesh = yaml.loadAs(is, Map.class);
        Map<String, Mapping> config = new HashMap<>();

        for (Map.Entry<String, Map<String, Object>> entry : flesh.entrySet()) {
            Map<String, Object> rawMapping = entry.getValue();

            String contextName = entry.getKey();
            String mvoClassStr = (String) rawMapping.get("mvo");
            String dtoClassStr = (String) rawMapping.get("dto");
            Map<String, Map<String, String>> attrs = (Map<String, Map<String, String>>) rawMapping.get("attrs");
            try {
                Mapping mapping = new Mapping(contextName, mvoClassStr, dtoClassStr, attrs);
                config.put(contextName, mapping);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return Collections.unmodifiableMap(config);
    }

    public Mapping mapping(String contextName) {
        try {
            load();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        return config().get(contextName);
    }

    public Mapping mapping(Class<? extends EntityDto> dtoClass) {
        try {
            load();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        for (Map.Entry<String, Mapping> entry : config().entrySet()) {
            Mapping map = entry.getValue();
            Class<? extends EntityDto> dto = map.dto();
            if (dto != null && dto == dtoClass) {
                return map;
            }
        }
        return null;
    }

    public static class Mapping {
        private final String _contextName;
        private final Class<? extends MVO> _mvo;
        private final Class<? extends EntityDto> _dto;
        private final Map<String, Attr> _attrs;

        @SuppressWarnings("unchecked")
        Mapping(
                String contextName,
                String mvoClassStr,
                String dtoClassStr,
                Map<String, Map<String, String>> rawAttrs
        ) throws ClassNotFoundException {
            _contextName = contextName;

            if (mvoClassStr != null) {
                Class<?> clazz = Class.forName(mvoClassStr);
                if (MVO.class.isAssignableFrom(clazz)) {
                    _mvo = (Class<? extends MVO>) clazz;
                } else {
                    throw new IllegalArgumentException(clazz.getSimpleName() + " is not MVO.");
                }
            } else {
                _mvo = null;
            }

            if (dtoClassStr != null) {
                Class<?> clazz = Class.forName(dtoClassStr);
                if (EntityDto.class.isAssignableFrom(clazz)) {
                    _dto = (Class<? extends EntityDto>) clazz;
                } else {
                    throw new IllegalArgumentException(clazz.getSimpleName() + " is not DTO.");
                }
            } else {
                _dto = null;
            }

            Map<String, Attr> attrs = new LinkedHashMap<>();
            if (rawAttrs != null) {
                for (Map.Entry<String, Map<String, String>> entry : rawAttrs.entrySet()) {
                    Map<String, String> rawAttr = entry.getValue();

                    String name = entry.getKey();
                    String summary = rawAttr.get("summary");
                    String description = rawAttr.get("description");
                    String dtoName = rawAttr.get("dto-name");

                    Attr attr = new Attr(name, summary, description, dtoName);
                    attrs.put(name, attr);
                }
            }
            _attrs = Collections.unmodifiableMap(attrs);
        }

        public String context() {
            return _contextName;
        }

        public Class<? extends MVO> mvo() {
            return _mvo;
        }

        public Class<? extends EntityDto> dto() {
            return _dto;
        }

        public Map<String, Attr> attrs() {
            return _attrs;
        }

        public Attr attr(String attrName) {
            return _attrs.get(attrName);
        }
    }

    public static class Attr {
        private final String _name;
        private final String _summary;
        private final String _description;
        private final String _dtoName;

        public Attr(String name, String summary, String description, String dtoName) {
            _name = name;
            _summary = summary;
            _description = description;
            _dtoName = dtoName;
        }

        public String name() {
            return _name;
        }

        public String summary() {
            return _summary;
        }

        public String description() {
            return _description;
        }

        public String dtoName() {
            return _dtoName;
        }
    }
}
