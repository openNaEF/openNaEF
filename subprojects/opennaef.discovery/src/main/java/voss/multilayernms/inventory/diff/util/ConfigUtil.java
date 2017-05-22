package voss.multilayernms.inventory.diff.util;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.config.MplsNmsDiffConfiguration;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConfigUtil {

    private static final Logger log = LoggerFactory.getLogger(ConfigUtil.class);

    public static final String KEY_DIFFSET_STORE_DIR = "diffset-store-directory";
    public static final String KEY_DEBUG_STORE_DIR = "debug-store-directory";
    public static final String KEY_DEBUG_FLAG = "debug-flag";

    public static final String ATTR_DISPLAY_NAME = ".display-name";
    public static final String ATTR_AUTO_APPLY = ".auto-apply";
    public static final String ATTR_CONNECTION_STRING = ".connection-string";
    public static final String ATTR_REGULAR_EXECUTION = ".regular-execution";
    public static final String ATTR_START_TIME = ".start-time";
    public static final String ATTR_INTERVAL = ".interval";

    public static final String KEY_DISCOVERY_BASE = "discovery";
    public static final String KEY_DISCOVERY_AUTO_APPLY = KEY_DISCOVERY_BASE + ATTR_AUTO_APPLY;
    public static final String KEY_DISCOVERY_REGULAR_EXECUTION_TIME = KEY_DISCOVERY_BASE + ATTR_REGULAR_EXECUTION + ATTR_START_TIME;
    public static final String KEY_DISCOVERY_REGULAR_EXECUTION_INTERVAL = KEY_DISCOVERY_BASE + ATTR_REGULAR_EXECUTION + ATTR_INTERVAL;

    private static final String DATE_STRING_FORMAT = "yyyy/MM/dd HH:mm";

    private XMLConfiguration conf = new XMLConfiguration();
    private final List<ConfigFormatValidator> validateList;
    private static ConfigUtil instance = null;

    public static ConfigUtil getInstance() {
        if (instance == null) instance = new ConfigUtil();
        return instance;
    }

    private ConfigUtil() {
        validateList = new ArrayList<ConfigFormatValidator>();
        validateList.add(new DateTimeValidator(ConfigUtil.KEY_DISCOVERY_REGULAR_EXECUTION_TIME).isNullOk());
        validateList.add(new IntegerGEValidator(ConfigUtil.KEY_DISCOVERY_REGULAR_EXECUTION_INTERVAL, 1).isNullOk());
        validateList.add(new StringValidator(ConfigUtil.KEY_DIFFSET_STORE_DIR));
        validateList.add(new StringValidator(ConfigUtil.KEY_DEBUG_STORE_DIR));
    }

    public Date getDate(String key) {
        try {
            DateFormat format = new SimpleDateFormat(DATE_STRING_FORMAT);
            return format.parse(conf.getString(key));
        } catch (ParseException e) {
        }
        return null;
    }

    public int getPlusInt(String key) {
        return conf.getInt(key, -1);
    }

    public boolean reload() throws ConfigurationException, IOException {
        return reload(MplsNmsDiffConfiguration.getInstance().getXMLConfiguration());
    }

    private boolean reload(XMLConfiguration xmlConfig) throws ConfigurationException {
        log.debug("reloadPropertyFile");
        if (isValueCheckOk(xmlConfig)) {
            conf = xmlConfig;
            return true;
        } else {
            return false;
        }
    }

    private boolean isValueCheckOk(XMLConfiguration xmlConfig) throws ConfigurationException {
        for (ConfigFormatValidator validater : validateList) {
            if (!validater.validate(xmlConfig)) {
                log.debug("property[" + validater.paramName + "] value[" + validater.value + "] NG.");
                return false;
            }
        }
        return true;
    }

    public boolean isDiscoveryAutoApply() {
        return conf.getBoolean(KEY_DISCOVERY_AUTO_APPLY, false);
    }

    public String getProperty(String key) {
        return conf.getString(key);
    }

    public void setProperty(String key, String value) {
        conf.setProperty(key, value);
    }

    public XMLConfiguration getPropertiesConfiguration() {
        return conf;
    }

    public String getDifferenceSetDir() {
        String result = conf.getString(KEY_DIFFSET_STORE_DIR);
        if (!result.endsWith("/")) {
            result += "/";
        }
        return result;
    }

    public boolean isDebugFlagOn() {
        return conf.getBoolean(KEY_DEBUG_FLAG, false);
    }

    public String getDebugDumpDir() {
        String result = conf.getString(KEY_DEBUG_STORE_DIR);
        if (!result.endsWith("/")) {
            result += "/";
        }
        return result;
    }

    private abstract class ConfigFormatValidator {
        protected final String paramName;
        protected String value;
        protected boolean isNullOk = false;

        ConfigFormatValidator(String paramName) {
            this.paramName = paramName;
        }

        public abstract boolean validate(XMLConfiguration conf);

        protected void validateFalse() {
            log.debug("param[" + paramName + "] value[" + value + "]");
        }

        public ConfigFormatValidator isNullOk() {
            isNullOk = true;
            return this;
        }
    }

    private class DateTimeValidator extends ConfigFormatValidator {

        DateTimeValidator(String paramName) {
            super(paramName);
        }

        @Override
        public boolean validate(XMLConfiguration conf) {
            value = conf.getString(paramName);
            if (value == null) {
                if (isNullOk) return true;
                return false;
            }
            try {
                DateFormat format = new SimpleDateFormat(DATE_STRING_FORMAT);
                format.parse(value);
                return true;
            } catch (ParseException e) {
                log.debug("", e);
                validateFalse();
            }
            return false;
        }

    }

    @SuppressWarnings("unused")
    private class BooleanValidator extends ConfigFormatValidator {

        BooleanValidator(String paramName) {
            super(paramName);
        }

        @Override
        public boolean validate(XMLConfiguration conf) {
            value = conf.getString(paramName);
            if (value == null) {
                if (isNullOk) return true;
                return false;
            }
            try {
                if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))
                    return true;
            } catch (Exception e) {
                log.debug("", e);
                validateFalse();
            }
            return false;
        }

    }

    @SuppressWarnings("unused")
    private class IntegerValidator extends ConfigFormatValidator {

        IntegerValidator(String paramName) {
            super(paramName);
        }

        @Override
        public boolean validate(XMLConfiguration conf) {
            value = conf.getString(paramName);
            if (value == null) {
                if (isNullOk) return true;
                return false;
            }
            try {
                Integer.parseInt(value);
                return true;
            } catch (Exception e) {
                log.debug("", e);
                validateFalse();
            }
            return false;
        }

    }

    private class IntegerGEValidator extends ConfigFormatValidator {
        private final int border;

        IntegerGEValidator(String paramName, int border) {
            super(paramName);
            this.border = border;
        }

        @Override
        public boolean validate(XMLConfiguration conf) {
            value = conf.getString(paramName);
            if (value == null) {
                if (isNullOk) return true;
                return false;
            }
            try {
                int i = Integer.parseInt(value);
                return i >= border;
            } catch (Exception e) {
                log.debug("", e);
                validateFalse();
            }
            return false;
        }

    }

    private class StringValidator extends ConfigFormatValidator {

        StringValidator(String paramName) {
            super(paramName);
        }

        @Override
        public boolean validate(XMLConfiguration conf) {
            value = conf.getString(paramName);
            if (value == null) {
                if (isNullOk) return true;
                return false;
            }
            if ("".endsWith(value)) {
                if (isNullOk) return true;
                return false;
            }
            return true;
        }

    }

}