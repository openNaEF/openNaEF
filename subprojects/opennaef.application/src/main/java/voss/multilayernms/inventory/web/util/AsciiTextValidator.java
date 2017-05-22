package voss.multilayernms.inventory.web.util;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;

import java.util.Map;

public class AsciiTextValidator extends AbstractValidator<String> {
    private static final long serialVersionUID = 1L;
    private char invalidChar = ' ';

    public AsciiTextValidator() {
        super();
    }

    @Override
    protected void onValidate(IValidatable<String> arg0) {
        String range = arg0.getValue();
        for (int i = 0; i < range.length(); i++) {
            char c = range.charAt(i);
            if (0x20 > c) {
                invalidChar = c;
                error(arg0);
            } else if (0xFF < c) {
                invalidChar = c;
                error(arg0);
            }
        }
    }

    @Override
    protected String resourceKey() {
        return "AsciiTextValidator";
    }

    @Override
    protected Map<String, Object> variablesMap(IValidatable<String> validatable) {
        Map<String, Object> map = super.variablesMap(validatable);
        map.put("asciiText", validatable.getValue());
        map.put("invalidChars", String.valueOf(this.invalidChar));
        return map;
    }

}