package voss.multilayernms.inventory.nmscore.csv.importer.setter;

import voss.multilayernms.inventory.constants.CustomerConstants;
import voss.nms.inventory.builder.AttributeUpdateCommandBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;

public class CsvBuilderSetter {
    public static void setPurpose(AttributeUpdateCommandBuilder builder, String purpose) {
        builder.setValue(MPLSNMS_ATTR.PURPOSE, purpose);
    }

    public static void setNote(AttributeUpdateCommandBuilder builder, String note) {
        builder.setValue(MPLSNMS_ATTR.NOTE, note);
    }

    public static void setNotices(AttributeUpdateCommandBuilder builder, String notices) {
        builder.setValue(CustomerConstants.NOTICES, notices);
    }

    public static void setEndUser(AttributeUpdateCommandBuilder builder, String userName) {
        builder.setValue(MPLSNMS_ATTR.END_USER, userName);
    }

    public static void setAreaCode(AttributeUpdateCommandBuilder builder, String areaCode) {
        builder.setValue(CustomerConstants.AREA_CODE, areaCode);
    }

    public static void setUserCode(AttributeUpdateCommandBuilder builder, String userCode) {
        builder.setValue(CustomerConstants.USER_CODE, userCode);
    }
}