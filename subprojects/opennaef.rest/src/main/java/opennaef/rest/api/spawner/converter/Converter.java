package opennaef.rest.api.spawner.converter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * このアノテーションが付与されているValueConverterは自動で読み込まれる
 *
 * @see Converts#init()  Values
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Converter {
    /**
     * 指定する値が大きいほど優先度は高くなる
     * この値を指定しない、または、0以下を指定した場合は優先度 0 として扱う
     *
     * @return 優先度
     */
    int priority() default 0;
}
