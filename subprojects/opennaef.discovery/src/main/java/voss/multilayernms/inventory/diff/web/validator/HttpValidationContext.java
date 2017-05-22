package voss.multilayernms.inventory.diff.web.validator;

import voss.multilayernms.inventory.diff.web.flow.FlowContext;

import java.util.List;
import java.util.Map;


public class HttpValidationContext extends BaseValidationContext {

    private final FlowContext flowContext;

    public HttpValidationContext(FlowContext flowContext) {
        this.flowContext = flowContext;
    }

    public Object getParameter(Validator<? extends Object> validator) {
        return flowContext.getParameter(getParameterName(validator));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getParameter(Class<T> clazz, Validator<? extends Object> validator) {
        if (clazz.equals(String.class)) {
            return (T) getParameter(validator);
        } else if (clazz.equals(String[].class)) {
            return (T) flowContext.getParameterMap().get(getParameterName(validator));
        }
        throw new IllegalArgumentException();
    }

    public FlowContext getFlowContext() {
        return flowContext;
    }

    public void registerErrorMessagesToAttribute() {
        registerErrorMessages();
        registerErrorMessageMap();
    }

    @SuppressWarnings("unchecked")
    private void registerErrorMessageMap() {
        Map<String, List<String>> attribute = (Map<String, List<String>>) flowContext.getHttpServletRequest().getAttribute("errorMessageMap");
        if (attribute == null) {
            attribute = (Map<String, List<String>>) errorMessageMap.clone();
        } else {
            attribute.putAll(errorMessageMap);
        }
        flowContext.getHttpServletRequest().setAttribute("errorMessageMap", attribute);
    }

    @SuppressWarnings("unchecked")
    private void registerErrorMessages() {
        List<String> attribute = (List<String>) flowContext.getHttpServletRequest().getAttribute("errorMessages");
        if (attribute == null) {
            attribute = (List<String>) errorMessages.clone();
        } else {
            attribute.addAll(errorMessages);
        }
        flowContext.getHttpServletRequest().setAttribute("errorMessages", attribute);
    }

}