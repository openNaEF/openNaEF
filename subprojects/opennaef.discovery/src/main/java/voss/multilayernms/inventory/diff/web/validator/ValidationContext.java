package voss.multilayernms.inventory.diff.web.validator;

import java.util.List;

public interface ValidationContext {

    public void addError(String paramName, String message);

    public void addError(Validator<? extends Object> validator, String message);

    public void addError(String message);

    public List<String> getErrorMessages();

    public void setMessageHeader(String messageHeader);

    public boolean containsError();

    public boolean containsError(Validator<?> validator);

    public void setParameterPrefix(String parameterPrefix);

    public Object getParameter(Validator<? extends Object> validator);

    public <T> T getParameter(Class<T> clazz, Validator<? extends Object> validator);

}