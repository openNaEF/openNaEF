package voss.multilayernms.inventory.web.parts;

import naef.dto.PortDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import voss.nms.inventory.model.PortsModel;

public class PortPanel extends Panel {
    private static final long serialVersionUID = 1L;

    public PortPanel(String id, final WebPage parentPage, final PortsModel portsModel, final String editorName) {
        super(id);

        final ListView<PortDto> interfaceListView = new ListView<PortDto>("interfaces", portsModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(ListItem<PortDto> item) {
                PortDto port = item.getModelObject();
                if (port == null) {
                    ColumnHeaderRowPanel row = new ColumnHeaderRowPanel("panel");
                    item.add(row);
                } else {
                    port.renew();
                    item.setOutputMarkupId(true);
                    PortRowPanel row = new PortRowPanel("panel", parentPage, port, editorName);
                    item.add(row);
                }
            }
        };
        add(interfaceListView);
    }
}