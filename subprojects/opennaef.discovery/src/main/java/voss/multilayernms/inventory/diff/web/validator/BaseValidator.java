package voss.multilayernms.inventory.diff.web.validator;


public abstract class BaseValidator<DESTTYPE, SRCTYPE> implements Validator<DESTTYPE> {

    protected final String description;
    private final String fieldName;
    private Class<DESTTYPE> type;
    private boolean nullOkFlag = false;

    public BaseValidator(String fieldName, Class<DESTTYPE> type, String description) {
        this.fieldName = fieldName;
        this.type = type;
        this.description = description;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getParameterName() {
        return fieldName;
    }

    public Class<DESTTYPE> getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    abstract protected DESTTYPE get(SRCTYPE src, ValidationContext validationContext);

    abstract protected SRCTYPE getSource(ValidationContext validationContext);

    public boolean isNull(ValidationContext validationContext) {
        SRCTYPE value = getSource(validationContext);
        return (value == null || value.toString().trim().length() == 0);
    }

    public Validator<DESTTYPE> setNullOk() {
        nullOkFlag = true;
        return this;
    }

    public final DESTTYPE get(ValidationContext validationContext) {
        SRCTYPE value = getSource(validationContext);
        if (isNull(validationContext)) {
            if (!nullOkFlag) {
                validationContext.addError(this, getEmptyMessage());
            }
            return null;
        }
        return get(value, validationContext);
    }

    protected String getEmptyMessage() {
        return description + " is null.";
    }

}