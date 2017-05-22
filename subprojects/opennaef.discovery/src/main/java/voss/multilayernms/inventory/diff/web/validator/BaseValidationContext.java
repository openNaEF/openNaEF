package voss.multilayernms.inventory.diff.web.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

abstract public class BaseValidationContext implements ValidationContext {

    private final Logger log = LoggerFactory.getLogger(BaseValidationContext.class);

    protected final ArrayList<String> errorMessages = new ArrayList<String>();
    protected final HashMap<String, List<String>> errorMessageMap = new HashMap<String, List<String>>();
    protected final Set<Validator<?>> validators = new HashSet<Validator<?>>();
    protected String messageHeader;
    protected String parameterPrefix;


    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void addError(String paramName, String message) {
        String shownMessage = getShownMessage(message);
        log.debug("validationError " + shownMessage);
        errorMessages.add(shownMessage);
        addToErrorMessageMap(paramName, message);
    }


    public void addError(Validator<? extends Object> validator, String message) {
        validators.add(validator);
        String shownMessage = getShownMessage(message);
        log.debug("validationError " + shownMessage);
        errorMessages.add(shownMessage);
        addToErrorMessageMap(getParameterName(validator), shownMessage);
    }

    public void addError(String message) {
        String shownMessage = getShownMessage(message);
        log.debug("validationError " + shownMessage);
        errorMessages.add(shownMessage);
    }

    private String getShownMessage(String message) {
        if (messageHeader != null) {
            return messageHeader + message;
        } else {
            return message;
        }
    }

    private void addToErrorMessageMap(String paramName, String message) {
        List<String> elementErrorMessages = errorMessageMap.get(paramName);
        if (elementErrorMessages == null) {
            elementErrorMessages = new ArrayList<String>();
            errorMessageMap.put(paramName, elementErrorMessages);
        }
        elementErrorMessages.add(message);
    }


    public void setMessageHeader(String messageHeader) {
        this.messageHeader = messageHeader;
    }

    public boolean containsError() {
        return errorMessages.size() >= 1;
    }

    public boolean containsError(Validator<?> validator) {
        return validators.contains(validator);
    }

    public void setParameterPrefix(String parameterPrefix) {
        this.parameterPrefix = parameterPrefix;
    }

    public String getParameterName(Validator<? extends Object> validator) {
        return (parameterPrefix != null ? parameterPrefix : "") + validator.getParameterName();
    }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(Class<T> clazz, Validator<? extends Object> validator) {
        return (T) getParameter(validator);
    }

    abstract public Object getParameter(Validator<? extends Object> validator);

}