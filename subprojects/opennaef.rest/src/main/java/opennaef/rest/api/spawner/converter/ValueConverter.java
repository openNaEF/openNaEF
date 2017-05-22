package opennaef.rest.api.spawner.converter;

/**
 * ある値からJSONで返すべき値へ変換する
 */
public interface ValueConverter<R> {
    /**
     * value がこのクラスで処理すべきオブジェクトであれば true
     *
     * @param value
     * @return このクラスで処理すべきオブジェクトであれば true
     */
    boolean accept(Object value);

    /**
     * value からJSONで返すべき値へ変換する
     *
     * @param value
     * @return JSONで返すべき値
     */
    R convert(Object value);
}
