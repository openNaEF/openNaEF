package voss.multilayernms.inventory.web.node;

import naef.dto.LocationDto;
import naef.dto.NodeDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.web.location.LocationUtil;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;

public class NodeListPage extends WebPage {
    public static final String KEY_NODE_NAME_FILTER = "filter";
    public static final String OPERATION_NAME = "NodeList";

    public NodeListPage() {
        this(new PageParameters());
    }

    public NodeListPage(PageParameters params) {
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            final MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            ExternalLink searchLink = UrlUtil.getLink("search", "search");
            add(searchLink);
            BookmarkablePageLink<Void> refresh = new BookmarkablePageLink<Void>("refresh", NodeListPage.class);
            add(refresh);
            Link<Void> addNodeLink = new Link<Void>("addNode") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    LocationDto top = LocationUtil.getRootLocation();
                    WebPage page = new NodeEditPage(NodeListPage.this, null, top);
                    setResponsePage(page);
                }
            };
            add(addNodeLink);
            List<NodeDto> nodes = new ArrayList<NodeDto>(conn.getActiveNodes());
            ListView<NodeDto> nodeList = new ListView<NodeDto>("nodes", nodes) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<NodeDto> item) {
                    NodeDto node = item.getModelObject();
                    Link<Void> link = getLink(node);
                    item.add(link);
                    Label label = new Label("nodeName", NodeRenderer.getNodeName(node));
                    link.add(label);
                    item.add(new Label("applianceType", NodeRenderer.getApplianceType(node)));
                    item.add(new Label("resourcePermission", NodeRenderer.getResourcePermission(node)));
                    item.add(new Label("portSummary", NodeRenderer.getPortSummary(node)));
                    item.add(new Label("purpose", NodeRenderer.getPurpose(node)));
                    item.add(new Label("nodeType", NodeRenderer.getNodeType(node)));
                    item.add(new Label("vendorName", NodeRenderer.getVendorName(node)));
                    item.add(new Label("osType", NodeRenderer.getOsType(node)));
                    item.add(new Label("osVersion", NodeRenderer.getOsVersion(node)));
                    item.add(new Label("managementIpAddress", NodeRenderer.getManagementIpAddress(node)));
                }

                private BookmarkablePageLink<Void> getLink(NodeDto node) {
                    if (node == null) {
                        throw new IllegalStateException("node is null.");
                    }
                    PageParameters param = NodeUtil.getParameters(node);
                    BookmarkablePageLink<Void> link = new BookmarkablePageLink<Void>("nodeNameLink", SimpleNodeDetailPage.class, param);
                    return link;
                }
            };
            add(nodeList);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }
}