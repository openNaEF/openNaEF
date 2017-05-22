package voss.multilayernms.inventory.diff.web.validator;


public class LongValidator extends ParameterValidator<Long> implements Validator<Long> {

    public LongValidator(String name, String description) {
        super(name, Long.class, description);
    }

    public LongValidator(String name) {
        super(name, Long.class, name);
    }

    public Long get(String value, ValidationContext validationContext) {
        long n;
        try {
            n = Long.parseLong(value.toLowerCase().trim());
        } catch (NumberFormatException e) {
            validationContext.addError(this, "The value of " + description + " is not correct.");
            return null;
        }
        return n;
    }

}