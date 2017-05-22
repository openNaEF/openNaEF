package tef.skelton;

import lib38k.xml.Xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SkeltonUtils {

    private SkeltonUtils() {
    }

    public static List<String> splitToken(String str, String delimiter) {
        List<String> result = new ArrayList<String>();
        int delimiterIndex = str.indexOf(delimiter);
        if (delimiterIndex < 0) {
            result.add(str);
        } else {
            result.add(str.substring(0, delimiterIndex));
            result.addAll(splitToken(str.substring(delimiterIndex + delimiter.length()), delimiter));
        }
        return result;
    }

    public static String getXmlAttribute(String errorMessagePrefix, Xml.Elem e, String attrName) {
        String result = e.getAttr(attrName);
        if (result == null) {
            throw new ConfigurationException(
                errorMessagePrefix + ", " + e.getName() + " には " + attrName + " が必須です.");
        }
        return result;
    }

    public static <T> Class<? extends T> classForName(String errorMessagePrefix, Class<T> klass, String className) {
        Class<?> result;
        try {
            result = Class.forName(className);
        } catch (ClassNotFoundException ce) {
            throw new ConfigurationException(errorMessagePrefix + ", " + className + "が見つかりません.");
        }
        if (! klass.isAssignableFrom(result)) {
            throw new ConfigurationException(errorMessagePrefix + ", " + className + " は指定できません.");
        }
        return result.asSubclass(klass);
    }

    public static String fqnEscape(String rawName) {
        String delimiter1 = SkeltonTefService.instance().getFqnPrimaryDelimiter();
        String delimiter2 = SkeltonTefService.instance().getFqnSecondaryDelimiter();
        String delimiter3 = SkeltonTefService.instance().getFqnTertiaryDelimiter();
        return rawName
            .replace("\\", "\\\\")
            .replace(delimiter1, "\\" + delimiter1)
            .replace(delimiter2, "\\" + delimiter2)
            .replace(delimiter3, "\\" + delimiter3);
    }

    public static String shellCommandlineArgEscape(String commandlineArg) {
        return commandlineArg
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    public static <T> T asSingle(Collection<T> values) {
        if (values.size() == 0) {
            return null;
        } else if (values.size() == 1) {
            return values.iterator().next();
        }
        throw new IllegalStateException();
    }
}
