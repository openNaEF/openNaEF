package voss.core.common.diff;

public class StringValueResolver implements ValueResolver<String> {

    @Override
    public String getKey(String object) {
        if (object == null) {
            return null;
        }
        return object;
    }

    @Override
    public String getValue(String object) {
        if (object == null) {
            return null;
        }
        return object;
    }

}