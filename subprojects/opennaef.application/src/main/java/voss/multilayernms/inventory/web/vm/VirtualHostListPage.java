package voss.multilayernms.inventory.web.vm;

import naef.dto.NodeDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.web.node.SimpleNodeDetailPage;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;

public class VirtualHostListPage extends WebPage {
    public static final String KEY_NODE_NAME_FILTER = "filter";
    public static final String OPERATION_NAME = "VirtualHostList";
    private final VirtualizationHostModel model;

    public VirtualHostListPage() {
        this(new PageParameters());
    }

    public VirtualHostListPage(PageParameters params) {
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            ExternalLink searchLink = UrlUtil.getLink("search", "search");
            add(searchLink);
            BookmarkablePageLink<Void> refresh = new BookmarkablePageLink<Void>("refresh", VirtualHostListPage.class);
            add(refresh);
            this.model = new VirtualizationHostModel();
            this.model.renew();
            ListView<NodeDto> nodeList = new ListView<NodeDto>("nodes", this.model) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<NodeDto> item) {
                    final NodeDto node = item.getModelObject();
                    Link<Void> link = getLink(node);
                    item.add(link);
                    Label label = new Label("nodeName", NodeRenderer.getNodeName(node));
                    link.add(label);
                    item.add(new Label("purpose", NodeRenderer.getPurpose(node)));
                    item.add(new Label("nodeType", NodeRenderer.getNodeType(node)));
                    item.add(new Label("vendorName", NodeRenderer.getVendorName(node)));
                    item.add(new Label("osType", NodeRenderer.getOsType(node)));
                    item.add(new Label("osVersion", NodeRenderer.getOsVersion(node)));
                    item.add(new Label("managementIpAddress", NodeRenderer.getManagementIpAddress(node)));
                    item.add(getVmAddLink(node));
                    item.add(getVmListLink(node));
                }

                private BookmarkablePageLink<Void> getLink(final NodeDto node) {
                    if (node == null) {
                        throw new IllegalStateException("node is null.");
                    }
                    PageParameters param = NodeUtil.getParameters(node);
                    BookmarkablePageLink<Void> link = new BookmarkablePageLink<Void>("nodeNameLink", SimpleNodeDetailPage.class, param);
                    return link;
                }

                private Link<Void> getVmAddLink(final NodeDto node) {
                    Link<Void> addNodeLink = new Link<Void>("addVM") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            WebPage page = new VirtualNodeEditPage(VirtualHostListPage.this, null, node);
                            setResponsePage(page);
                        }
                    };
                    return addNodeLink;
                }

                private Link<Void> getVmListLink(final NodeDto node) {
                    if (node == null) {
                        throw new IllegalStateException("node is null.");
                    }
                    Link<Void> link = new Link<Void>("listVM") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            WebPage page = new VirtualNodeListPage(VirtualHostListPage.this, node);
                            setResponsePage(page);
                        }
                    };
                    return link;
                }
            };
            add(nodeList);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private static class VirtualizationHostModel extends RenewableAbstractReadOnlyModel<List<NodeDto>> {
        private static final long serialVersionUID = 1L;
        private final List<NodeDto> nodes = new ArrayList<NodeDto>();

        public VirtualizationHostModel() {
        }

        @Override
        public List<NodeDto> getObject() {
            return nodes;
        }

        @Override
        public void renew() {
            this.nodes.clear();
            try {
                InventoryConnector conn = InventoryConnector.getInstance();
                for (NodeDto node : conn.getActiveNodes()) {
                    if (DtoUtil.getBoolean(node, ATTR.ATTR_VIRTUALIZATION_HOSTING_ENABLED)) {
                        this.nodes.add(node);
                    }
                }
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }

    }
}