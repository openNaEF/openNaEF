package voss.multilayernms.inventory.web.link;

import naef.dto.LinkDto;
import naef.dto.PortDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.web.node.SimpleNodeDetailPage;
import voss.nms.inventory.builder.LinkCommandBuilder;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.PageUtil;

import java.io.Serializable;

public class LinkEditPage extends WebPage {
    public static final String OPERATION_NAME = "LinkEdit";
    private final WebPage backPage;
    private final PortDto basePort;
    private final NeighborModel model;
    private PortDto l2Neighbor;
    private final String editorName;

    private Link<Void> linkL2Node;

    public LinkEditPage(WebPage backPage, PortDto basePort) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            if (backPage == null) {
                throw new IllegalArgumentException();
            }
            this.backPage = backPage;
            if (basePort == null) {
                throw new IllegalArgumentException();
            }
            basePort.renew();
            this.basePort = basePort;
            this.l2Neighbor = NodeUtil.getLayer2Neighbor(basePort);
            this.model = new NeighborModel(basePort);

            Link<Void> backLink = new Link<Void>("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    setResponsePage(getBackPage());
                }
            };
            add(backLink);
            add(new FeedbackPanel("feedback"));

            Label baseLabel = new Label("baseName", new PropertyModel<String>(this.model, "baseName"));
            add(baseLabel);
            Label nodeLabel1 = new Label("node", new PropertyModel<String>(this.model, "hereNodeName"));
            add(nodeLabel1);
            Label ifNameLabel1 = new Label("ifName", new PropertyModel<String>(this.model, "hereIfName"));
            add(ifNameLabel1);
            BookmarkablePageLink<Void> neighborLink =
                    new BookmarkablePageLink<Void>("neighborNode",
                            SimpleNodeDetailPage.class,
                            NodeUtil.getNodeParameters(this.model.getThere()));
            add(neighborLink);
            Label nodeLabel2 = new Label("nodeName", new PropertyModel<String>(this.model, "thereNodeName"));
            neighborLink.add(nodeLabel2);
            Label ifNameLabel2 = new Label("neighborIfName", new PropertyModel<String>(this.model, "thereIfName"));
            add(ifNameLabel2);

            linkL2Node = createLink("layer2NeighborNode", l2Neighbor);
            add(linkL2Node);
            Label linkL2Label = new Label("layer2NeighborIfName", new PropertyModel<String>(this, "l2NeighborIfName"));
            add(linkL2Label);
            Link<Void> editLayer2 = createEditor("createLink", basePort, l2Neighbor);
            editLayer2.setEnabled(l2Neighbor == null);
            editLayer2.setVisible(l2Neighbor == null);
            add(editLayer2);
            Link<Void> deleteLayer2 = createDeleteButton("deleteLink", false, basePort);
            deleteLayer2.setEnabled(l2Neighbor != null);
            deleteLayer2.setVisible(l2Neighbor != null);
            add(deleteLayer2);

        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private Link<Void> createLink(String id, PortDto port) {
        PageParameters param = NodeUtil.getNodeParameters(port);
        Link<Void> link = new BookmarkablePageLink<Void>(id, SimpleNodeDetailPage.class, param);
        link.setEnabled(port != null);
        link.add(new Label("nodeName", new Model<String>(NodeUtil.getNodeName(port))));
        return link;
    }

    private Link<Void> createEditor(String id, final PortDto port,
                                    final PortDto neighbor) {
        Link<Void> link = new Link<Void>(id) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                port.renew();
                String nodeName = NodeUtil.getNodeName(neighbor);
                LinkNeighborPortSelectionPage page = new LinkNeighborPortSelectionPage(
                        LinkEditPage.this, getBackPage(), port, nodeName);
                setResponsePage(page);
            }
        };
        return link;
    }

    private Link<Void> createDeleteButton(String id, final boolean layer1, final PortDto basePort) {
        Link<Void> link = new Link<Void>(id) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                try {
                    LinkDto link = NodeUtil.getLayer2Link(basePort);
                    LinkCommandBuilder builder = new LinkCommandBuilder(link, editorName);
                    builder.buildDeleteCommand();
                    ShellConnector.getInstance().execute(builder);
                    basePort.renew();
                } catch (Exception e) {
                    ExceptionUtils.throwAsRuntime(e);
                }
                PageUtil.setModelChanged(getBackPage());
                setResponsePage(new LinkEditPage(getBackPage(), basePort));
            }
        };
        return link;
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public PortDto getBasePort() {
        return this.basePort;
    }

    public String getBaseName() {
        return NameUtil.getCaption(basePort);
    }

    public String getBaseNodeName() {
        return this.basePort.getNode().getName();
    }

    public String getBaseIfName() {
        return NameUtil.getIfName(this.basePort);
    }

    public void setL2Neighbor(PortDto port) {
        this.l2Neighbor = port;
    }

    public PortDto getL2Neighbor() {
        return this.l2Neighbor;
    }

    public String getl2NeighborNodeName() {
        if (this.l2Neighbor == null) {
            return null;
        }
        return this.l2Neighbor.getNode().getName();
    }

    public String getl2NeighborIfName() {
        if (this.l2Neighbor == null) {
            return null;
        }
        return NameUtil.getIfName(this.l2Neighbor);
    }


    @Override
    protected void onModelChanged() {
        this.basePort.renew();
        super.onModelChanged();
    }

    public static class NeighborModel implements Serializable {
        private static final long serialVersionUID = 1L;
        private final PortDto here;
        private PortDto there;

        public NeighborModel(PortDto here) {
            this.here = here;
            this.there = NodeUtil.getLayer2Neighbor(here);
        }


        public PortDto getHere() {
            return this.here;
        }


        public void setThere(PortDto there) {
            this.there = there;
        }

        public PortDto getThere() {
            return this.there;
        }


        public void renew() {
            this.here.renew();
            this.there.renew();
        }


        public String getBaseName() {
            String nodeName = PortRenderer.getNodeName(this.here);
            String portName = PortRenderer.getIfName(this.here);
            return nodeName + ":" + portName;
        }

        public String getHereNodeName() {
            return PortRenderer.getNodeName(here);
        }

        public String getHereIfName() {
            return PortRenderer.getIfName(this.here);
        }


        public String getThereNodeName() {
            return PortRenderer.getNodeName(this.there);
        }

        public String getThereIfName() {
            return PortRenderer.getIfName(this.there);
        }

    }
}