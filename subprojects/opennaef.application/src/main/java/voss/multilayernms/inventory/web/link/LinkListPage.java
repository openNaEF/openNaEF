package voss.multilayernms.inventory.web.link;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.constants.MplsNmsPoolConstants;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.LinkRenderer;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.web.node.SimpleNodeDetailPage;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;

public class LinkListPage extends WebPage {
    public static final String KEY_NODE_NAME_FILTER = "filter";
    public static final String OPERATION_NAME = "LinkList";

    public LinkListPage() {
        this(new PageParameters());
    }

    public LinkListPage(PageParameters params) {
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            final MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            ExternalLink searchLink = UrlUtil.getLink("search", "search");
            add(searchLink);
            BookmarkablePageLink<Void> refresh = new BookmarkablePageLink<Void>("refresh", LinkListPage.class);
            add(refresh);
            IpSubnetNamespaceDto linkPool = conn.getActiveRootIpSubnetNamespace(MplsNmsPoolConstants.DEFAULT_IPSUBNET_POOL);
            List<IpSubnetDto> links = new ArrayList<IpSubnetDto>();
            links.addAll(linkPool.getUsers());
            ListView<IpSubnetDto> nodeList = new ListView<IpSubnetDto>("links", links) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<IpSubnetDto> item) {
                    final IpSubnetDto link = item.getModelObject();
                    PortDto portA = LinkRenderer.getPort1(link);
                    PortDto portB = LinkRenderer.getPort2(link);
                    Link<Void> editLink = new Link<Void>("editLink") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            WebPage page = new LinkEditPage(LinkListPage.this, LinkRenderer.getPort1(link));
                            setResponsePage(page);
                        }
                    };
                    item.add(editLink);
                    editLink.add(new Label("linkName", LinkRenderer.getName(link)));
                    item.add(new Label("linkID", InventoryIdCalculator.getId(link)));
                    item.add(new Label("facilityStatus", LinkRenderer.getFacilityStatusName(link)));
                    String approvedValue = "";
                    if (LinkRenderer.isLinkApproved(link)) {
                        approvedValue = "O";
                    }
                    item.add(new Label("approved", approvedValue));
                    String networkValue = "";
                    if (LinkRenderer.getOnNetworkStatusAsBoolean(link)) {
                        networkValue = "O";
                    }
                    item.add(new Label("network", networkValue));
                    item.add(new Label("source", LinkRenderer.getSource(link)));
                    item.add(new Label("cableName", LinkRenderer.getCableName(link)));
                    item.add(new Label("sRLG", LinkRenderer.getSRLGValue(link)));
                    item.add(getLink("nodeNameLinkA", portA.getNode()));
                    item.add(new Label("nodeTypeA", NodeRenderer.getNodeType(portA.getNode())));
                    item.add(new Label("ifNameA", PortRenderer.getIfName(portA)));
                    item.add(new Label("ipAddressA", PortRenderer.getIpAddress(portA)));
                    item.add(getLink("nodeNameLinkB", portB.getNode()));
                    item.add(new Label("nodeTypeB", NodeRenderer.getNodeType(portB.getNode())));
                    item.add(new Label("ifNameB", PortRenderer.getIfName(portB)));
                    item.add(new Label("ipAddressB", PortRenderer.getIpAddress(portB)));
                }

                private BookmarkablePageLink<Void> getLink(String id, NodeDto node) {
                    if (node == null) {
                        throw new IllegalStateException("node is null.");
                    }
                    PageParameters param = NodeUtil.getParameters(node);
                    BookmarkablePageLink<Void> link = new BookmarkablePageLink<Void>(id, SimpleNodeDetailPage.class, param);
                    Label nodeName = new Label("nodeName", node.getName());
                    link.add(nodeName);
                    return link;
                }
            };
            add(nodeList);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }
}