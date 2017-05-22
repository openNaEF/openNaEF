package voss.multilayernms.inventory.web.vlan;

import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.config.CoreConfiguration;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.PerfLog;
import voss.multilayernms.inventory.web.mpls.MplsPageUtil;
import voss.multilayernms.inventory.web.mpls.ReleaseNetworkIDConfirmationPage;
import voss.multilayernms.inventory.web.parts.CommandExecutionConfirmationPage;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.model.VlanPoolModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.HistoryUtil;
import voss.nms.inventory.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;

public class VlanIdPoolDetailPage extends WebPage {
    public static final String OPERATION_NAME = "VlanIdPoolDetail";
    public static final String RELEASE_OPERATION_NAME = "VlanIdRelease";

    private final Label range;
    private final Label poolName;
    private final Label purpose;
    private final Link<Void> editPool;
    private final VlanIdPoolDto pool;
    private final VlanPoolModel poolModel;
    private final VlanIDListModel listModel;
    private long startTime = System.currentTimeMillis();

    public VlanIdPoolDetailPage() {
        this(new PageParameters());
    }

    public VlanIdPoolDetailPage(PageParameters param) {
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            pool = MplsPageUtil.getVlanIdPool(param);
            if (pool == null) {
                throw new IllegalStateException("unknown pool: " + param);
            }

            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            BookmarkablePageLink<Void> parentLink = new BookmarkablePageLink<Void>("parent", VlanIdPoolListPage.class);
            add(parentLink);
            BookmarkablePageLink<Void> reloadLink = new BookmarkablePageLink<Void>("refresh",
                    VlanIdPoolDetailPage.class, MplsPageUtil.getVlanIdPoolParam(pool));
            add(reloadLink);

            this.poolModel = new VlanPoolModel(pool);
            this.listModel = new VlanIDListModel(pool);
            this.listModel.renew();
            this.poolName = new Label("poolName", new PropertyModel<String>(poolModel, "poolName"));
            add(this.poolName);
            this.range = new Label("range", new PropertyModel<String>(poolModel, "range"));
            add(this.range);
            this.purpose = new Label("purpose", new PropertyModel<String>(poolModel, "usage"));
            add(this.purpose);
            Label note = new Label("note", new PropertyModel<String>(poolModel, "note"));
            add(note);
            Label lastEditor = new Label("lastEditor", new PropertyModel<String>(poolModel, "lastEditor"));
            add(lastEditor);
            Label lastEditTime = new Label("lastEditTime", new PropertyModel<String>(poolModel, "lastEditTime"));
            add(lastEditTime);
            Label version = new Label("version", new PropertyModel<String>(poolModel, "version"));
            add(version);

            this.editPool = new Link<Void>("editPool") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    setResponsePage(new VlanIdPoolEditPage(VlanIdPoolDetailPage.this, pool));
                }
            };
            add(this.editPool);

            final ListView<VlanDto> vlans = new ListView<VlanDto>("vlans", this.listModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<VlanDto> item) {
                    final VlanDto vlan = item.getModelObject();
                    item.add(new Label("vlanId", Model.of(vlan.getVlanId())));
                    item.add(new Label("vlanSize", Model.of(vlan.getMemberVlanifs().size())));
                    item.add(new Label("operStatus", Model.of(DtoUtil.getStringOrNull(vlan, MPLSNMS_ATTR.OPER_STATUS))));
                    item.add(new Label("note", Model.of(DtoUtil.getStringOrNull(vlan, MPLSNMS_ATTR.NOTE))));
                    item.add(new Label("lastEditor", Model.of(DtoUtil.getStringOrNull(vlan, MPLSNMS_ATTR.LAST_EDITOR))));
                    item.add(new Label("lastEditTime", Model.of(DtoUtil.getDate(vlan, MPLSNMS_ATTR.LAST_EDIT_TIME))));
                    Link<Void> editLink = new Link<Void>("edit") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            VlanEditPage page = new VlanEditPage(VlanIdPoolDetailPage.this, vlan);
                            setResponsePage(page);
                        }
                    };
                    editLink.setEnabled(vlan != null);
                    editLink.setVisible(vlan != null);
                    item.add(editLink);
                    Link<Void> deleteIDLink = new Link<Void>("delete") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            ReleaseNetworkIDConfirmationPage page =
                                    new ReleaseNetworkIDConfirmationPage(VlanIdPoolDetailPage.this,
                                            RELEASE_OPERATION_NAME, vlan);
                            setResponsePage(page);
                        }
                    };
                    deleteIDLink.setEnabled(vlan != null);
                    deleteIDLink.setVisible(vlan != null);
                    item.add(deleteIDLink);
                    Link<Void> resetLink = new Link<Void>("reset") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            String msg = "Resets the VLAN All vlan-link, vlan-if are deleted, and the association between vlan-if and port is also deleted.";
                            List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
                            CommandExecutionConfirmationPage page =
                                    new CommandExecutionConfirmationPage(VlanIdPoolDetailPage.this, msg, builders);
                            setResponsePage(page);
                        }
                    };
                    resetLink.setEnabled(vlan != null);
                    resetLink.setVisible(vlan != null);
                    item.add(resetLink);
                    ExternalLink history = HistoryUtil.createHistoryLink(vlan, DtoUtil.getMvoVersionString(vlan));
                    history.setEnabled(CoreConfiguration.getInstance().isDebug());
                    item.add(history);
                }

            };
            add(vlans);
            addNewVlanLink("newVlan1", pool);
            addNewVlanLink("newVlan2", pool);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    @Override
    public void onPageAttached() {
        this.startTime = System.currentTimeMillis();
        super.onPageAttached();
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        log().info("page rendering time: " + (System.currentTimeMillis() - startTime));
        PerfLog.info(startTime, System.currentTimeMillis(), "VlanIdPoolDetailPage:onPageAttach->onDetach");
    }

    @Override
    protected void onModelChanged() {
        this.pool.renew();
        this.poolModel.renew();
        this.listModel.renew();
        super.onModelChanged();
    }

    private Logger log() {
        return LoggerFactory.getLogger(VlanIdPoolDetailPage.class);
    }

    private void addNewVlanLink(final String id, final VlanIdPoolDto pool) {
        Link<Void> newPath = new Link<Void>(id) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                setResponsePage(new VlanCreationPage(VlanIdPoolDetailPage.this, pool));
            }
        };
        add(newPath);
    }

    ;

    private static class VlanIDListModel extends RenewableAbstractReadOnlyModel<List<VlanDto>> {
        private static final long serialVersionUID = 1L;
        private final List<VlanDto> vlans = new ArrayList<VlanDto>();
        private final VlanIdPoolDto pool;

        public VlanIDListModel(VlanIdPoolDto pool) {
            if (pool == null) {
                throw new IllegalArgumentException();
            }
            this.pool = pool;
        }

        @Override
        public List<VlanDto> getObject() {
            return vlans;
        }

        @Override
        public void renew() {
            this.pool.renew();
            this.vlans.clear();
            this.vlans.addAll(this.pool.getUsers());
        }

    }
}