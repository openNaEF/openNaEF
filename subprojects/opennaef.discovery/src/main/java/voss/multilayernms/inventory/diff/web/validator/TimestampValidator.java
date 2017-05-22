package voss.multilayernms.inventory.diff.web.validator;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class TimestampValidator extends ParameterValidator<Timestamp> implements Validator<Timestamp> {

    private String dateFormat;

    public TimestampValidator(String fieldName, String description, String dateFormat) {
        super(fieldName, Timestamp.class, description);
        this.dateFormat = dateFormat;

    }

    @Override
    protected Timestamp get(String src, ValidationContext context) {
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        format.setLenient(false);
        Timestamp result;
        try {
            result = new Timestamp(format.parse(src.trim()).getTime());
        } catch (ParseException e) {
            context.addError(this, description + "The date and time are incorrect. Please enter in the format as " + dateFormat + ".");
            return null;
        }

        return result;
    }

}