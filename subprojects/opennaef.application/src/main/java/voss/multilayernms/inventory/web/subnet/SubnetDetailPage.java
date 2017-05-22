package voss.multilayernms.inventory.web.subnet;

import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetDto;
import naef.mvo.ip.IpAddress;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.Comparators;
import voss.nms.inventory.util.UrlUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SubnetDetailPage extends WebPage {
    public static final String OPERATION_NAME = "SubnetDetail";
    private final WebPage backPage;
    private final IpAddressListModel listModel;
    private final IpSubnetDto subnet;
    private final IpSubnetAddressDto address;

    public SubnetDetailPage() {
        this(null, null);
    }

    public SubnetDetailPage(WebPage backPage, IpSubnetDto subnet) {
        if (subnet == null) {
            throw new IllegalArgumentException();
        }
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            this.subnet = subnet;
            this.address = subnet.getSubnetAddress();
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            Link<Void> refresh;
            if (address == null) {
                refresh = new BookmarkablePageLink<Void>("refresh", SubnetDetailPage.class);
            } else {
                refresh = new Link<Void>("refresh") {
                    private static final long serialVersionUID = 1L;

                    public void onClick() {
                        SubnetDetailPage page = SubnetDetailPage.this;
                        page.modelChanged();
                        setResponsePage(page);
                    }
                };
            }
            add(refresh);
            Link<Void> back = new Link<Void>("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    WebPage backPage = getBackPage();
                    if (backPage == null) {
                        return;
                    }
                    setResponsePage(backPage);
                }
            };
            back.setEnabled(this.backPage != null);
            back.setVisible(this.backPage != null);
            add(back);

            this.listModel = new IpAddressListModel(this.address);
            this.listModel.renew();
            add(new Label("head", new PropertyModel<String>(this.subnet, "subnetName")));
            ListView<IpIfDto> childrenList = new ListView<IpIfDto>("users", this.listModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<IpIfDto> item) {
                    final IpIfDto ipIf = item.getModelObject();
                    IpAddress ip = ipIf.getIpAddress();
                    Collection<PortDto> ports = ipIf.getAssociatedPorts();
                    String ifName = null;
                    String nodeName = null;
                    PortDto port;
                    if (ports.size() == 0) {
                        port = null;
                        nodeName = PortRenderer.getNodeName(ipIf);
                        ifName = PortRenderer.getIfName(ipIf);
                    } else if (ports.size() == 1) {
                        port = ports.iterator().next();
                        nodeName = PortRenderer.getNodeName(port);
                        ifName = PortRenderer.getIfName(port);
                    } else {
                        port = null;
                        nodeName = "*";
                        ifName = "*";
                    }
                    Integer maskLength = ipIf.getSubnetMaskLength();
                    StringBuilder sb = new StringBuilder();
                    sb.append(ip.toString());
                    if (maskLength != null) {
                        sb.append("/").append(maskLength.toString());
                    }
                    item.add(new Label("ipAddress", sb.toString()));
                    item.add(new Label("nodeName", nodeName));
                    item.add(new Label("ifName", ifName));
                }
            };
            add(childrenList);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static Link<Void> getIpSubnetLink(IpSubnetAddressDto address) {
        if (address == null) {
            throw new IllegalStateException("address is null.");
        }
        Link<Void> link = new Link<Void>("ipSubnetDetail") {
            private static final long serialVersionUID = 1L;

            public void onClick() {
            }
        };
        Label subnetName = new Label("ipSubnetName");
        link.add(subnetName);
        return link;
    }

    public IpSubnetAddressDto getAddress() {
        return address;
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        if (this.address != null) {
            this.address.renew();
            this.listModel.renew();
        }
    }

    @SuppressWarnings("serial")
    private static class IpAddressListModel extends RenewableAbstractReadOnlyModel<List<IpIfDto>> {
        private final List<IpIfDto> users = new ArrayList<IpIfDto>();
        private final IpSubnetAddressDto subnet;

        public IpAddressListModel(IpSubnetAddressDto subnet) {
            this.subnet = subnet;
        }

        @Override
        public List<IpIfDto> getObject() {
            return users;
        }

        @Override
        public void renew() {
            if (subnet == null) {
                return;
            }
            this.subnet.renew();
            this.users.clear();
            this.users.addAll(this.subnet.getUsers());
            Collections.sort(this.users, Comparators.getIfNameBasedPortComparator());
        }
    }
}