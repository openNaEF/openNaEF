package opennaef.rest.api.config.api;

import opennaef.rest.Classes;

/**
 * Builder の Constructor, Method の引数の設定
 */
public class ArgConfig {
    public final Class<?> paramClass;
    public final String parameterizedType;  // TODO String ではなく、Type を持つべき
    public final String attrName;

    public ArgConfig(String parameterizedType, String attrName) throws ClassNotFoundException {
        this.parameterizedType = parameterizedType;

        String paramClassType = parameterizedType;
        if (paramClassType.contains("<")) {
            paramClassType = paramClassType.substring(0, paramClassType.indexOf("<"));
        }

        this.paramClass = Classes.forName(paramClassType);
        this.attrName = attrName;
    }
}
