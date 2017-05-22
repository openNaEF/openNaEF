package voss.multilayernms.inventory.web.subnet;

import naef.dto.ip.IpSubnetNamespaceDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.IpSubnetAddressRenderer;
import voss.multilayernms.inventory.web.parts.CommandExecutionConfirmationPage;
import voss.nms.inventory.builder.RootIpSubnetNamespaceCommandBuilder;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.Comparators;
import voss.nms.inventory.util.UrlUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RootSubnetListPage extends WebPage {
    public static final String OPERATION_NAME = "SubnetList";
    private final WebPage backPage;
    private final String editorName;
    private final IpSubnetRootModel listModel;

    public RootSubnetListPage() {
        this(null);
    }

    public RootSubnetListPage(WebPage backPage) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            Link<Void> refresh = new BookmarkablePageLink<Void>("refresh", RootSubnetListPage.class);
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

            this.listModel = new IpSubnetRootModel();
            this.listModel.renew();
            Link<Void> addLink = new Link<Void>("addRoot") {
                private static final long serialVersionUID = 1L;

                public void onClick() {
                    log().debug("add new root.");
                    RootSubnetNamespaceEditPage page = new RootSubnetNamespaceEditPage(RootSubnetListPage.this, null);
                    setResponsePage(page);
                }
            };
            add(addLink);
            ListView<IpSubnetNamespaceDto> childrenList = new ListView<IpSubnetNamespaceDto>("children", this.listModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<IpSubnetNamespaceDto> item) {
                    final IpSubnetNamespaceDto address = item.getModelObject();
                    IpSubnetAddressRenderer renderer = new IpSubnetAddressRenderer(address);
                    item.add(new Label("vpnPrefix", renderer.getVpnPrefix()));
                    item.add(new Label("ipSubnetNamespaceName", renderer.getIpSubnetNamespaceName()));
                    item.add(new Label("ipSubnetAddressName", renderer.getIpSubnetAddressName()));
                    item.add(new Label("subnetAddress", renderer.getSubnetAddress()));
                    item.add(new Label("lastEditor", renderer.getLastEditor()));
                    item.add(new Label("lastEditTime", renderer.getLastEditTime()));

                    Link<Void> detailLink = new Link<Void>("detail") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            SubnetNamespaceDetailPage page = new SubnetNamespaceDetailPage(RootSubnetListPage.this, address);
                            setResponsePage(page);
                        }
                    };
                    item.add(detailLink);
                    Link<Void> editLink = new Link<Void>("edit") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            log().debug("edit.");
                            RootSubnetNamespaceEditPage page = new RootSubnetNamespaceEditPage(RootSubnetListPage.this, address);
                            setResponsePage(page);
                        }
                    };
                    item.add(editLink);
                    Link<Void> removeLink = new Link<Void>("remove") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            log().debug("remove.");
                            RootIpSubnetNamespaceCommandBuilder builder = new RootIpSubnetNamespaceCommandBuilder(address, editorName);
                            try {
                                BuildResult result = builder.buildDeleteCommand();
                                if (result != BuildResult.SUCCESS) {
                                    return;
                                }
                                List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
                                builders.add(builder);
                                WebPage forwardPage = getBackPage();
                                if (forwardPage == null) {
                                    forwardPage = RootSubnetListPage.this;
                                }
                                CommandExecutionConfirmationPage page = new CommandExecutionConfirmationPage(
                                        RootSubnetListPage.this, forwardPage, "Delete this root IP subnet.", builders);
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
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    @Override
    protected void onModelChanged() {
        this.listModel.renew();
        super.onModelChanged();
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }

    @SuppressWarnings("serial")
    private static class IpSubnetRootModel extends RenewableAbstractReadOnlyModel<List<IpSubnetNamespaceDto>> {
        private final List<IpSubnetNamespaceDto> roots = new ArrayList<IpSubnetNamespaceDto>();

        public IpSubnetRootModel() {
        }

        @Override
        public List<IpSubnetNamespaceDto> getObject() {
            return roots;
        }

        @Override
        public void renew() {
            try {
                LoggerFactory.getLogger(getClass()).debug("renew.");
                MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
                this.roots.clear();
                this.roots.addAll(conn.getActiveRootIpSubnetNamespaces());
                Collections.sort(this.roots, Comparators.getDtoComparator());
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
    }
}