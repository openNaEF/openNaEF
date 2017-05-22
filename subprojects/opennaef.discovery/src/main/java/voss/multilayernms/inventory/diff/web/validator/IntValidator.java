package voss.multilayernms.inventory.diff.web.validator;

public class IntValidator extends ParameterValidator<Integer> implements Validator<Integer> {

    public IntValidator(String name, String description) {
        super(name, Integer.class, description);
    }

    public IntValidator(String name) {
        super(name, Integer.class, name);
    }

    public Integer get(String value, ValidationContext validationContext) {
        int n;
        try {
            n = Integer.parseInt(value.toLowerCase().trim());
        } catch (NumberFormatException e) {
            validationContext.addError(this, "The value of " + description + " is not correct.");
            return null;
        }
        return n;
    }


}