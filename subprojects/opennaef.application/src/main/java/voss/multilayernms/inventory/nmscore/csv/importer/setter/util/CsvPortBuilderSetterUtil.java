package voss.multilayernms.inventory.nmscore.csv.importer.setter.util;

import voss.multilayernms.inventory.nmscore.csv.importer.setter.CsvBuilderSetter;
import voss.nms.inventory.builder.AttributeUpdateCommandBuilder;

public class CsvPortBuilderSetterUtil {
    public static void setBuilderAttribute(AttributeUpdateCommandBuilder builder, String field, String value) {
        if (field.equals("EndUser")) {
            CsvBuilderSetter.setEndUser(builder, value);
            return;
        } else if (field.equals("Notices")) {
            CsvBuilderSetter.setNotices(builder, value);
            return;
        } else {
            CsvGeneralBuilderSetterUtil.setBuilderAttribute(builder, field, value);
        }

    }
}