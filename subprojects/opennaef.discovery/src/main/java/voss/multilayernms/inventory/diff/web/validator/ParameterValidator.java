package voss.multilayernms.inventory.diff.web.validator;

abstract public class ParameterValidator<T> extends BaseValidator<T, String> {

    public ParameterValidator(String fieldName, Class<T> type, String description) {
        super(fieldName, type, description);
    }

    @Override
    protected String getSource(ValidationContext validationContext) {
        return (String) validationContext.getParameter(this);
    }

}