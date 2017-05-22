package voss.multilayernms.inventory.web.location;

import naef.dto.LocationDto;
import org.apache.wicket.markup.html.form.ChoiceRenderer;

public class LocationChoiceRenderer extends ChoiceRenderer<LocationDto> {
    private static final long serialVersionUID = 1L;

    @Override
    public Object getDisplayValue(LocationDto arg0) {
        return LocationUtil.getCaption(arg0);
    }

}