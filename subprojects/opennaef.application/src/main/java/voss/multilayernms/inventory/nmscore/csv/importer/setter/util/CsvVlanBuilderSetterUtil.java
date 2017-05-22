package voss.multilayernms.inventory.nmscore.csv.importer.setter.util;

import voss.multilayernms.inventory.nmscore.csv.importer.setter.CsvBuilderSetter;
import voss.nms.inventory.builder.AttributeUpdateCommandBuilder;

public class CsvVlanBuilderSetterUtil {
    public static void setBuilderAttribute(AttributeUpdateCommandBuilder builder, String field, String value) {
        if (field.equals("AreaCode")) {
            CsvBuilderSetter.setAreaCode(builder, value);
            return;
        } else if (field.equals("UserCode")) {
            CsvBuilderSetter.setUserCode(builder, value);
            return;
        } else if (field.equals("EndUser")) {
            return;
        } else {
            CsvGeneralBuilderSetterUtil.setBuilderAttribute(builder, field, value);
        }

    }
}