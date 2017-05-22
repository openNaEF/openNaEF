package voss.multilayernms.inventory.nmscore.csv.importer.setter.util;

import voss.multilayernms.inventory.nmscore.csv.importer.setter.CsvBuilderSetter;
import voss.nms.inventory.builder.AttributeUpdateCommandBuilder;

public class CsvGeneralBuilderSetterUtil {
    public static void setBuilderAttribute(AttributeUpdateCommandBuilder builder, String field, String value) {

        if (field.equals("Note")) {
            CsvBuilderSetter.setNote(builder, value);
            return;
        } else if (field.equals("Notices")) {
            CsvBuilderSetter.setNotices(builder, value);
            return;
        } else if (field.equals("Type") || field.equals("Purpose")) {
            CsvBuilderSetter.setPurpose(builder, value);
            return;
        }
    }
}