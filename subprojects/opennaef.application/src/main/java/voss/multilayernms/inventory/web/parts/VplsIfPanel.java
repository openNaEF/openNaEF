package voss.multilayernms.inventory.web.parts;

import naef.dto.vpls.VplsIfDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;

public class VplsIfPanel extends Panel {
    private static final long serialVersionUID = 1L;

    public VplsIfPanel(String id, final WebPage parentPage, final AbstractReadOnlyModel vplsIfsModel, final String editorName) {
        super(id);

        final ListView<VplsIfDto> vplsIfsListView = new ListView<VplsIfDto>("vplsIfs", vplsIfsModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(ListItem<VplsIfDto> item) {
                VplsIfDto port = item.getModelObject();
                if (port == null) {
                    ColumnHeaderRowPanel row = new ColumnHeaderRowPanel("vplsPanel");
                    item.add(row);
                } else {
                    port.renew();
                    item.setOutputMarkupId(true);
                    VplsIfRowPanel row = new VplsIfRowPanel("vplsPanel", parentPage, port, editorName);
                    item.add(row);
                }
            }
        };
        add(vplsIfsListView);
    }
}