package voss.multilayernms.inventory.nmscore.csv.importer.setter.util;

import voss.nms.inventory.builder.AttributeUpdateCommandBuilder;

public class CsvNodeBuilderSetterUtil {
    public static void setBuilderAttribute(AttributeUpdateCommandBuilder builder, String field, String value) {
        CsvGeneralBuilderSetterUtil.setBuilderAttribute(builder, field, value);
    }
}