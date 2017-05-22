package voss.multilayernms.inventory.diff.web.validator;


public class StringValidator extends ParameterValidator<String> implements Validator<String> {

    public StringValidator(String name, String description) {
        super(name, String.class, description);
    }

    public StringValidator(String name) {
        super(name, String.class, name);
    }

    @Override
    protected String get(String src, ValidationContext validationContext) {
        return src;
    }
}