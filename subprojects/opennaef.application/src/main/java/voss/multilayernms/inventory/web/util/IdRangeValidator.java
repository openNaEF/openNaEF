package voss.multilayernms.inventory.web.util;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.InventoryUtil;
import voss.core.server.util.Util;

import java.util.Map;

public class IdRangeValidator extends AbstractValidator<String> {
    private static final long serialVersionUID = 1L;
    private final boolean allowZero;
    private String errorMessage = null;

    public IdRangeValidator() {
        super();
        this.allowZero = false;
    }

    public IdRangeValidator(boolean allowZero) {
        super();
        this.allowZero = allowZero;
    }

    @Override
    protected void onValidate(IValidatable<String> arg0) {
        String range = arg0.getValue();
        try {
            if (range != null) {
                range = Util.formatRange(range);
            }
            InventoryUtil.checkRange(range, this.allowZero);
        } catch (InventoryException e) {
            this.errorMessage = e.getMessage();
            error(arg0);
        }
    }

    @Override
    protected String resourceKey() {
        return "IdRangeValidator";
    }

    @Override
    protected Map<String, Object> variablesMap(IValidatable<String> validatable) {
        Map<String, Object> map = super.variablesMap(validatable);
        map.put("IdRange", validatable.getValue());
        map.put("Cause", this.errorMessage);
        return map;
    }

}