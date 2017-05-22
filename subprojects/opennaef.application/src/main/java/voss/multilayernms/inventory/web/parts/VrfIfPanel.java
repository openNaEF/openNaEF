package voss.multilayernms.inventory.web.parts;

import naef.dto.vrf.VrfIfDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;

import java.util.List;

public class VrfIfPanel extends Panel {
    private static final long serialVersionUID = 1L;

    public VrfIfPanel(String id, final WebPage parentPage, final AbstractReadOnlyModel<List<VrfIfDto>> vrfIfsModel, final String editorName) {
        super(id);

        final ListView<VrfIfDto> vrfIfsListView = new ListView<VrfIfDto>("vrfIfs", vrfIfsModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(ListItem<VrfIfDto> item) {
                VrfIfDto port = item.getModelObject();
                if (port == null) {
                    ColumnHeaderRowPanel row = new ColumnHeaderRowPanel("vrfPanel");
                    item.add(row);
                } else {
                    port.renew();
                    item.setOutputMarkupId(true);
                    VrfIfRowPanel row = new VrfIfRowPanel("vrfPanel", parentPage, port, editorName);
                    item.add(row);
                }
            }
        };
        add(vrfIfsListView);
    }
}