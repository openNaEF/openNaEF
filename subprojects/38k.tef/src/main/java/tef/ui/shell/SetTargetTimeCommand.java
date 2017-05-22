package tef.ui.shell;

import tef.DateTimeFormat;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class SetTargetTimeCommand extends ShellCommand {

    private enum OffsetUnit {

        DAY(Calendar.DATE), MONTH(Calendar.MONTH), YEAR(Calendar.YEAR);

        final int field;

        OffsetUnit(int field) {
            this.field = field;
        }
    }

    public String getArgumentDescription() {
        return "[target time]";
    }

    @Override
    public void process(Commandline commandline) throws ShellCommandException {
        checkArgsSize(commandline, 0, 2);

        Long value;

        switch (commandline.argsSize()) {
            case 0:
                value = defaultValue();
                break;
            case 1:
                String timeStr = commandline.arg(0);
                value = parseAsValue(timeStr);
                break;
            case 2:
                String valueStr = commandline.arg(0);
                String offsetUnitStr = commandline.arg(1);

                value = asOffset(valueStr, offsetUnitStr);
                break;
            default:
                throw new RuntimeException();
        }

        if (value == null) {
            throw new ShellCommandException("date-time format error.");
        }

        getSession().setTargetTime(value);
    }

    /**
     * 引数が指定されなかった場合の値を決定します. デフォルトでは now を返しますが,
     * 異なるデフォルト値を定義するアプリケーションではこのメソッドをオーバーライドしてください.
     */
    protected Long defaultValue() {
        return now();
    }

    protected Long now() {
        return new Long(System.currentTimeMillis());
    }

    private Long parseAsValue(String timeStr) {
        if (timeStr.equals("now")) {
            return now();
        }

        try {
            return Long.parseLong(timeStr);
        } catch (NumberFormatException nfe) {
        }

        try {
            return asTime
                    (new SimpleDateFormat("HH:mm:ss.SSS"), DateTimeFormat.YMDHMSS_DOT, timeStr);
        } catch (ParseException pe) {
        }

        try {
            return asTime
                    (new SimpleDateFormat("HH:mm:ss"), DateTimeFormat.YMDHMS_DOT, timeStr);
        } catch (ParseException pe) {
        }

        try {
            return DateTimeFormat.parse(timeStr).getTime();
        } catch (ParseException pe) {
        }

        return null;
    }

    private Long asTime(DateFormat format1, DateTimeFormat format2, String timeStr)
            throws ParseException {
        format1.parse(timeStr);

        String ymdhmssStr
                = DateTimeFormat.YMD_DOT.format(getSession().getTargetTime())
                + "-"
                + timeStr;
        return new Long(format2.parse(ymdhmssStr).getTime());
    }

    private Long asOffset(String offsetValueStr, String offsetUnitStr)
            throws ShellCommandException {
        int offsetValue;
        try {
            DecimalFormat formatter = new DecimalFormat();
            formatter.setPositivePrefix("+");
            formatter.setNegativePrefix("-");
            offsetValue = formatter.parse(offsetValueStr).intValue();
        } catch (ParseException pe) {
            throw new ShellCommandException("offset value format error: " + offsetValueStr);
        }

        OffsetUnit offsetUnit;
        try {
            offsetUnit = OffsetUnit.valueOf(offsetUnitStr.toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new ShellCommandException("unsupported offset unit: " + offsetUnitStr);
        }

        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date(getSession().getTargetTime()));
        calendar.add(offsetUnit.field, offsetValue);

        return new Long(calendar.getTime().getTime());
    }
}
