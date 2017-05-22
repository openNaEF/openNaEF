package voss.multilayernms.inventory.web.vm;

import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.web.node.SimpleNodeDetailPage;
import voss.nms.inventory.builder.VirtualNodeCommandBuilder;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VirtualNodeListPage extends WebPage {
    private static final Logger log = LoggerFactory.getLogger(VirtualNodeListPage.class);
    public static final String OPERATION_NAME = "VirtualNodeList";
    private final VirtualNodeModel model;
    private final String editorName;

    public VirtualNodeListPage(WebPage backPage, NodeDto hostNode) {
        try {
            this.model = new VirtualNodeModel(hostNode);
            this.model.renew();
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            ExternalLink searchLink = UrlUtil.getLink("search", "search");
            add(searchLink);
            Link<Void> refresh = new Link<Void>("refresh") {
                private static final long serialVersionUID = 1L;
                @Override
                public void onClick() {
                    VirtualNodeListPage.this.model.renew();
                    setResponsePage(VirtualNodeListPage.this);
                }
            };
            add(refresh);
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
                    Link<Void> deleteVM = new Link<Void>("deleteVM") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            try{
                                //FIXME can not delete
                                node.renew();
                                checkDeletable(node);
                                VirtualNodeCommandBuilder builder = new VirtualNodeCommandBuilder(node, editorName);
                                builder.setCascadeDelete(true);
                                builder.buildDeleteCommand();
                                ShellConnector.getInstance().execute(builder);
                                VirtualNodeListPage.this.model.renew();
                                setResponsePage(VirtualNodeListPage.this);
                            } catch (Exception e) {
                                throw ExceptionUtils.throwAsRuntime(e);
                            }
                        }
                    };
                    item.add(deleteVM);
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

    private static class VirtualNodeModel extends RenewableAbstractReadOnlyModel<List<NodeDto>> {
        private static final long serialVersionUID = 1L;
        private final Logger log = LoggerFactory.getLogger(getClass());
        private final List<NodeDto> nodes = new ArrayList<NodeDto>();
        private final NodeDto host;

        public VirtualNodeModel(NodeDto host) {
            this.host = host;
        }

        @Override
        public List<NodeDto> getObject() {
            return nodes;
        }

        @Override
        public void renew() {
            this.nodes.clear();
            if (this.host == null) {
                return;
            }
            this.host.renew();
            this.nodes.addAll(this.host.getVirtualizationGuestNodes());
            log.debug("* " + this.nodes.size());
        }
    }

    private void checkDeletable(NodeDto guest) {
        if (guest == null) {
            return;
        }
        for (PortDto port : guest.getPorts()) {
            Set<NetworkDto> networks = port.getNetworks();
            if (networks != null && networks.size() > 0) {
                throw new IllegalStateException("port is in use: Internal-ID=" + port.getAbsoluteName());
            }
        }
    }
}