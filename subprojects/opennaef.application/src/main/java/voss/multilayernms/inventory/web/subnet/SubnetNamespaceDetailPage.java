package voss.multilayernms.inventory.web.subnet;

import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.renderer.IpSubnetAddressRenderer;
import voss.multilayernms.inventory.web.parts.CommandExecutionConfirmationPage;
import voss.nms.inventory.builder.IpSubnetCommandBuilder;
import voss.nms.inventory.builder.RegularIpSubnetNamespaceCommandBuilder;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.Comparators;
import voss.nms.inventory.util.UrlUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubnetNamespaceDetailPage extends WebPage {
    public static final String OPERATION_NAME = "SubnetList";
    private final WebPage backPage;
    private final String editorName;
    private final IpSubnetAddressChildrenModel listModel;
    private final IpSubnetAddressUserModel usersModel;
    private final IpSubnetNamespaceDto subnet;

    public SubnetNamespaceDetailPage(WebPage backPage, IpSubnetNamespaceDto subnet) {
        if (subnet == null) {
            throw new IllegalArgumentException("subnet is null.");
        }
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            this.subnet = subnet;
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            Link<Void> refresh = new Link<Void>("refresh") {
                private static final long serialVersionUID = 1L;

                public void onClick() {
                    SubnetNamespaceDetailPage page = SubnetNamespaceDetailPage.this;
                    page.modelChanged();
                    setResponsePage(page);
                }
            };
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
            this.listModel = new IpSubnetAddressChildrenModel(this.subnet);
            this.listModel.renew();
            this.usersModel = new IpSubnetAddressUserModel(this.subnet);
            this.usersModel.renew();

            IpSubnetAddressRenderer renderer = new IpSubnetAddressRenderer(this.subnet);
            add(new Label("head", new PropertyModel<String>(renderer, "head")));
            add(new Label("vpnPrefix", new PropertyModel<String>(renderer, "vpnPrefix")));
            add(new Label("ipSubnetNamespaceName", new PropertyModel<String>(renderer, "ipSubnetNamespaceName")));
            add(new Label("ipSubnetAddressName", new PropertyModel<String>(renderer, "ipSubnetAddressName")));
            add(new Label("subnetAddress", new PropertyModel<String>(renderer, "subnetAddress")));
            add(new Label("lastEditor", new PropertyModel<String>(renderer, "lastEditor")));
            add(new Label("lastEditTime", new PropertyModel<String>(renderer, "lastEditTime")));

            IpSubnetAddressRenderer renderer2 = new IpSubnetAddressRenderer(this.subnet.getParent());
            add(new Label("parentVpnPrefix", new PropertyModel<String>(renderer2, "vpnPrefix")));
            add(new Label("parentIpSubnetNamespaceName", new PropertyModel<String>(renderer2, "ipSubnetNamespaceName")));
            add(new Label("parentIpSubnetAddressName", new PropertyModel<String>(renderer2, "ipSubnetAddressName")));
            add(new Label("parentSubnetAddress", new PropertyModel<String>(renderer2, "subnetAddress")));
            add(new Label("parentLastEditor", new PropertyModel<String>(renderer2, "lastEditor")));
            add(new Label("parentLastEditTime", new PropertyModel<String>(renderer2, "lastEditTime")));

            Link<Void> addLink = new Link<Void>("addChild") {
                private static final long serialVersionUID = 1L;

                public void onClick() {
                    log().debug("add child.");
                    SubnetNamespaceEditPage page = new SubnetNamespaceEditPage(SubnetNamespaceDetailPage.this, null, getAddress());
                    setResponsePage(page);
                }
            };
            add(addLink);
            Link<Void> editLink = new Link<Void>("edit") {
                private static final long serialVersionUID = 1L;

                public void onClick() {
                    log().debug("edit.");
                    SubnetNamespaceEditPage page = new SubnetNamespaceEditPage(SubnetNamespaceDetailPage.this, getAddress());
                    setResponsePage(page);
                }
            };
            add(editLink);

            ListView<IpSubnetNamespaceDto> childrenList = new ListView<IpSubnetNamespaceDto>("children", this.listModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<IpSubnetNamespaceDto> item) {
                    final IpSubnetNamespaceDto subnet = item.getModelObject();
                    IpSubnetAddressRenderer renderer = new IpSubnetAddressRenderer(subnet);
                    item.add(new Label("vpnPrefix", renderer.getVpnPrefix()));
                    Link<Void> link1 = new Link<Void>("ipSubnetDetail") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            SubnetNamespaceDetailPage page = new SubnetNamespaceDetailPage(SubnetNamespaceDetailPage.this, subnet);
                            setResponsePage(page);
                        }
                    };
                    link1.add(new Label("ipSubnetNamespaceName", renderer.getIpSubnetNamespaceName()));
                    item.add(link1);
                    item.add(new Label("ipSubnetAddressName", renderer.getIpSubnetAddressName()));
                    item.add(new Label("subnetAddress", renderer.getSubnetAddress()));
                    Link<Void> removeLink = new Link<Void>("remove") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            log().debug("remove.");
                            RegularIpSubnetNamespaceCommandBuilder builder =
                                    new RegularIpSubnetNamespaceCommandBuilder(subnet, editorName);
                            try {
                                BuildResult result = builder.buildDeleteCommand();
                                if (result != BuildResult.SUCCESS) {
                                    return;
                                }
                                List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
                                builders.add(builder);
                                CommandExecutionConfirmationPage page = new CommandExecutionConfirmationPage(
                                        SubnetNamespaceDetailPage.this, "Removing this IP delegation...", builders);
                                setResponsePage(page);
                            } catch (Exception e) {
                                throw ExceptionUtils.throwAsRuntime(e);
                            }
                        }
                    };
                    item.add(removeLink);
                }
            };
            add(childrenList);

            Link<Void> newSubnet = new Link<Void>("newSubnet") {
                private static final long serialVersionUID = 1L;

                public void onClick() {
                    log().debug("new subnet.");
                    SubnetEditPage page = new SubnetEditPage(SubnetNamespaceDetailPage.this, getAddress());
                    setResponsePage(page);
                }
            };
            add(newSubnet);

            ListView<IpSubnetDto> usersList = new ListView<IpSubnetDto>("users", this.usersModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<IpSubnetDto> item) {
                    final IpSubnetDto subnet = item.getModelObject();
                    String vpnPrefix = DtoUtil.getStringOrNull(subnet, ATTR.VPN_PREFIX);
                    item.add(new Label("vpnPrefix", vpnPrefix));
                    Link<Void> link1 = new Link<Void>("ipSubnetDetail") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            SubnetDetailPage page = new SubnetDetailPage(SubnetNamespaceDetailPage.this, subnet);
                            setResponsePage(page);
                        }
                    };
                    link1.add(new Label("ipSubnetName", subnet.getSubnetName()));
                    item.add(link1);
                    IpSubnetAddressDto _addr = subnet.getSubnetAddress();
                    String addr = (_addr == null ? "N/A" : _addr.getAddress().toString() + "/" + _addr.getSubnetMask());
                    item.add(new Label("subnetAddress", addr));
                    Link<Void> removeLink = new Link<Void>("remove") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            log().debug("remove.");
                            IpSubnetCommandBuilder builder =
                                    new IpSubnetCommandBuilder(subnet, editorName);
                            try {
                                BuildResult result = builder.buildDeleteCommand();
                                if (result != BuildResult.SUCCESS) {
                                    return;
                                }
                                List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
                                builders.add(builder);
                                CommandExecutionConfirmationPage page = new CommandExecutionConfirmationPage(
                                        SubnetNamespaceDetailPage.this, "Removing this IP subnet...", builders);
                                setResponsePage(page);
                            } catch (Exception e) {
                                throw ExceptionUtils.throwAsRuntime(e);
                            }
                        }
                    };
                    item.add(removeLink);
                }
            };
            add(usersList);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static Link<Void> getIpSubnetLink(IpSubnetNamespaceDto address) {
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

    public IpSubnetNamespaceDto getAddress() {
        return subnet;
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        if (this.subnet != null) {
            this.subnet.renew();
            this.listModel.renew();
            this.usersModel.renew();
        }
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }

    @SuppressWarnings("serial")
    private static class IpSubnetAddressChildrenModel extends RenewableAbstractReadOnlyModel<List<IpSubnetNamespaceDto>> {
        private final List<IpSubnetNamespaceDto> children = new ArrayList<IpSubnetNamespaceDto>();
        private final IpSubnetNamespaceDto parent;

        public IpSubnetAddressChildrenModel(IpSubnetNamespaceDto parent) {
            this.parent = parent;
        }

        @Override
        public List<IpSubnetNamespaceDto> getObject() {
            return children;
        }

        @Override
        public void renew() {
            if (parent == null) {
                return;
            }
            this.parent.renew();
            this.children.clear();
            this.children.addAll(this.parent.getChildren());
            Collections.sort(this.children, Comparators.getDtoComparator());
        }
    }

    @SuppressWarnings("serial")
    private static class IpSubnetAddressUserModel extends RenewableAbstractReadOnlyModel<List<IpSubnetDto>> {
        private final List<IpSubnetDto> users = new ArrayList<IpSubnetDto>();
        private final IpSubnetNamespaceDto parent;

        public IpSubnetAddressUserModel(IpSubnetNamespaceDto parent) {
            this.parent = parent;
        }

        @Override
        public List<IpSubnetDto> getObject() {
            return users;
        }

        @Override
        public void renew() {
            if (parent == null) {
                return;
            }
            this.parent.renew();
            this.users.clear();
            this.users.addAll(this.parent.getUsers());
            Collections.sort(this.users, Comparators.getDtoComparator());
        }
    }
}