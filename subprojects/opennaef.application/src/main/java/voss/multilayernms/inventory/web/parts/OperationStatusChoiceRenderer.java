package voss.multilayernms.inventory.web.parts;

import org.apache.wicket.markup.html.form.ChoiceRenderer;

public class OperationStatusChoiceRenderer extends ChoiceRenderer<String> {
    private static final long serialVersionUID = 1L;

    @Override
    public Object getDisplayValue(String str) {
        if (str == null || str.length() == 0) {
            return "N/A";
        }
        return super.getDisplayValue(str);
    }


}