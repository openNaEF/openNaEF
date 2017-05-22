package voss.multilayernms.inventory.web.mpls;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.web.mpls.PseudoWireEditPage.AcWrapper;
import voss.multilayernms.inventory.web.node.SimpleNodeDetailPage;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.util.NodeUtil;

import java.util.ArrayList;
import java.util.List;

public class PseudoWireAcLinkCellPanel extends Panel {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
    private static final transient Logger log = LoggerFactory.getLogger(PseudoWireAcLinkCellPanel.class);
    private final AcWrapper wrapper;
    private final RenewableAbstractReadOnlyModel<List<NodeDto>> nodesModel;
    private final ListView<NodeDto> listView;

    public PseudoWireAcLinkCellPanel(final String id, final AcWrapper pw) {
        super(id);
        this.wrapper = pw;

        this.nodesModel = new RenewableAbstractReadOnlyModel<List<NodeDto>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<NodeDto> getObject() {
                List<NodeDto> result = new ArrayList<NodeDto>();
                PortDto acPort = wrapper.getPort();
                if (acPort != null) {
                    acPort.renew();
                    result.add(acPort.getNode());
                }
                return result;
            }

            @Override
            public void renew() {
                if (wrapper.getPort() != null) {
                    wrapper.getPort().renew();
                }
            }
        };

        this.listView = new ListView<NodeDto>("nodes", nodesModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(ListItem<NodeDto> item) {
                final NodeDto node = item.getModelObject();
                PageParameters param = new PageParameters();
                String nodeName = "";
                if (node != null) {
                    param.add(NodeUtil.KEY_NODE, node.getName());
                    nodeName = node.getName();
                }
                Link<Void> nodeLink = new BookmarkablePageLink<Void>("node", SimpleNodeDetailPage.class, param);
                nodeLink.setEnabled(node != null);
                nodeLink.setVisible(node != null);
                item.add(nodeLink);

                Label nodeNameLabel = new Label("nodeName", Model.of(nodeName));
                nodeLink.add(nodeNameLabel);
            }
        };
        add(listView);
    }

    protected void onModelChanged() {
        this.listView.modelChanged();
        super.onModelChanged();
    }
}