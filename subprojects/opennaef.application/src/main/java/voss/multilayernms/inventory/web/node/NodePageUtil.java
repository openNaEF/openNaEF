package voss.multilayernms.inventory.web.node;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import voss.multilayernms.inventory.web.link.LinkEditPage;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.NodeUtil;

public class NodePageUtil {

    public static BookmarkablePageLink<Void> createSimpleNodeLink(final String id, final NodeDto node, final String captionId) {
        BookmarkablePageLink<Void> link = new BookmarkablePageLink<Void>(id, SimpleNodeDetailPage.class, NodeUtil.getParameters(node));
        link.setEnabled(node != null);
        String nodeName = (node == null ? "" : node.getName());
        if (captionId != null) {
            Label label = new Label(captionId, nodeName);
            link.add(label);
        }
        return link;
    }

    public static BookmarkablePageLink<Void> createNodeLink(final String id, final NodeDto node) {
        return createSimpleNodeLink(id, node, "nodeName");
    }

    public static Link<Void> createNodeLink(final String id, final PortDto port) {
        final PageParameters param;
        final String nodeName;
        if (port != null) {
            param = NodeUtil.getParameters(port.getNode());
            nodeName = port.getNode().getName();
        } else {
            param = null;
            nodeName = null;
        }
        BookmarkablePageLink<Void> link = new BookmarkablePageLink<Void>(id, SimpleNodeDetailPage.class, param);
        link.setEnabled(port != null);
        Label label = new Label("nodeName", nodeName);
        link.add(label);
        return link;
    }

    public static Link<Void> createPortLink(String id, PortDto port) {
        Label caption = new Label("ifName",
                new org.apache.wicket.model.Model<String>(
                        NameUtil.getNodeIfName(port)));
        final PageParameters params = NodeUtil.getNodeParameters(port);
        BookmarkablePageLink<Void> link = new BookmarkablePageLink<Void>(id, SimpleNodeDetailPage.class, params);
        link.setEnabled(port != null);
        link.add(caption);
        return link;
    }

    public static Link<Void> createPortLink(String id, IModel<PortDto> model) {
        PortDto port = model.getObject();
        Label caption = new Label("ifName",
                new org.apache.wicket.model.Model<String>(
                        NameUtil.getCaption(port)));
        final PageParameters params = NodeUtil.getNodeParameters(port);
        BookmarkablePageLink<Void> link = new BookmarkablePageLink<Void>(id, SimpleNodeDetailPage.class, params);
        link.setEnabled(port != null);
        link.add(caption);
        return link;
    }

    public static Link<Void> createNeighborEditLink(final WebPage backPage, final String id, final PortDto basePort) {
        Link<Void> link = new Link<Void>(id) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                LinkEditPage page = new LinkEditPage(backPage, basePort);
                setResponsePage(page);
            }
        };
        return link;
    }

}