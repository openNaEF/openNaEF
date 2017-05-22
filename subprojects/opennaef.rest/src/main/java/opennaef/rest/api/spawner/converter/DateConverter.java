package opennaef.rest.api.spawner.converter;

import opennaef.rest.DateFormat;
import tef.DateTime;

import java.util.Date;

/**
 * primitive型をStringへ変換する
 * 形式: "yyyy-MM-dd'T'HH:mm:ss.SSS"
 */
@Converter
public class DateConverter implements ValueConverter<String> {
    @Override
    public boolean accept(Object value) {
        return value instanceof Date
                || value instanceof DateTime;
    }

    @Override
    public String convert(Object value) {
        Date date;
        if (value instanceof Date) {
            date = (Date) value;
        } else if (value instanceof DateTime) {
            date = ((DateTime) value).toJavaDate();
        } else {
            throw new IllegalArgumentException("");
        }
        return DateFormat.format(date);
    }
}
