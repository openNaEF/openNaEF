package opennaef.notifier.util;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.NamingStyle;

import java.lang.reflect.Type;

/**
 * JSON 変換
 */
public class JsonUtils {
    public static JSON json;

    static {
        JSON json_ = new JSON() {
            @Override
            protected <T> T postparse(Context context, Object value, Class<? extends T> c, Type t) throws Exception {
                return super.postparse(context, value, c, t);
            }
        };
        json_.setPropertyStyle(NamingStyle.LOWER_UNDERSCORE);
//        json_.setPrettyPrint(true);
        json = json_;
    }

    public static String toJson(Object obj) {
        return json.format(obj);
    }
}
