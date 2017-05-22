package voss.multilayernms.inventory.diff.web.validator;

public interface Validator<T> {

    public T get(ValidationContext validationContext);

    public String getFieldName();

    public String getParameterName();

    public boolean isNull(ValidationContext validationContext);

    public String getDescription();

}