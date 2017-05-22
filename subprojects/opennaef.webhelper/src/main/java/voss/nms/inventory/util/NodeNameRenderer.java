package voss.nms.inventory.util;

import naef.dto.NodeDto;
import org.apache.wicket.markup.html.form.ChoiceRenderer;

public class NodeNameRenderer extends ChoiceRenderer<NodeDto> {
    private static final long serialVersionUID = 1L;

    @Override
    public Object getDisplayValue(NodeDto node) {
        if (node == null) {
            return null;
        }
        return node.getName();
    }
}