package voss.multilayernms.inventory.web.mpls;

import naef.dto.NodeDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.dto.mpls.RsvpLspIdPoolDto;
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
import voss.core.server.builder.TaskCommandBuilder;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.PerfLog;
import voss.core.server.util.TaskUtil;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.renderer.PathRenderer;
import voss.multilayernms.inventory.renderer.RsvpLspRenderer;
import voss.multilayernms.inventory.util.RsvpLspExtUtil;
import voss.multilayernms.inventory.web.node.SimpleNodeDetailPage;
import voss.multilayernms.inventory.web.parts.CommandExecutionConfirmationPage;
import voss.nms.inventory.model.RsvpLspPoolModel;
import voss.nms.inventory.util.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RsvpLspPoolDetailPage extends WebPage {
    public static final String OPERATION_NAME = "RsvpLspPoolDetail";
    public static final String RELEASE_OPERATION_NAME = "RsvpLspPoolIdRelease";

    private final String editorName;
    private final Label range;
    private final Label used;
    private final Label poolName;
    private final Label purpose;
    private final Label status;
    private final RsvpLspIdPoolDto pool;
    private final RsvpLspPoolModel poolModel;
    private final RsvpLspIdsModel lspModel;
    private long startTime = System.currentTimeMillis();

    public RsvpLspPoolDetailPage() {
        this(new PageParameters());
    }

    public RsvpLspPoolDetailPage(PageParameters param) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            pool = MplsPageUtil.getRsvpLspPool(param);
            if (pool == null) {
                throw new IllegalStateException("unknown pool: " + param);
            }

            this.lspModel = new RsvpLspIdsModel(pool);
            lspModel.renew();
            final Map<String, RsvpLspDto> lsps = lspModel.getLspsMap();

            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            BookmarkablePageLink<Void> parentLink = new BookmarkablePageLink<Void>("parent", RsvpLspPoolListPage.class);
            add(parentLink);
            BookmarkablePageLink<Void> reloadLink = new BookmarkablePageLink<Void>("refresh",
                    RsvpLspPoolDetailPage.class, MplsPageUtil.getRsvpLspPoolParam(pool));
            add(reloadLink);

            this.poolModel = new RsvpLspPoolModel(pool);
            this.poolName = new Label("poolName", new PropertyModel<String>(poolModel, "poolName"));
            add(this.poolName);
            this.range = new Label("range", new PropertyModel<String>(poolModel, "range"));
            add(this.range);
            this.used = new Label("used", new PropertyModel<String>(poolModel, "used"));
            add(this.used);
            this.purpose = new Label("purpose", new PropertyModel<String>(poolModel, "usage"));
            add(this.purpose);
            this.status = new Label("status", new PropertyModel<String>(poolModel, "status"));
            add(this.status);
            Label note = new Label("note", new PropertyModel<String>(poolModel, "note"));
            add(note);
            Label lastEditor = new Label("lastEditor", new PropertyModel<String>(poolModel, "lastEditor"));
            add(lastEditor);
            Label lastEditTime = new Label("lastEditTime", new PropertyModel<String>(poolModel, "lastEditTime"));
            add(lastEditTime);
            Label version = new Label("version", new PropertyModel<String>(poolModel, "version"));
            add(version);

            final ListView<String> lspRows = new ListView<String>("lsps", lspModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<String> item) {
                    final String lspName = item.getModelObject();
                    final RsvpLspDto lsp = lsps.get(lspName);
                    final RsvpLspDto pair = RsvpLspExtUtil.getOppositLsp(lsp);
                    Link<Void> editLsp = new Link<Void>("editLsp") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            setResponsePage(new RsvpLspEditPage(RsvpLspPoolDetailPage.this, lsp));
                        }
                    };
                    item.add(editLsp);
                    editLsp.add(new Label("lspID", Model.of(lsp.getName())));
                    item.add(new Label("lspName", Model.of(RsvpLspRenderer.getLspName(lsp))));
                    String inventoryID = null;
                    try {
                        inventoryID = InventoryIdCalculator.getId(lsp);
                    } catch (Exception e) {
                        log().warn("illeagl lsp:" + lsp.getName(), e);
                    }
                    item.add(new Label("inventoryID", Model.of(inventoryID)));
                    item.add(new Label("facilityStatus", Model.of(RsvpLspRenderer.getFacilityStatus(lsp))));
                    item.add(new Label("operStatus", Model.of(RsvpLspRenderer.getOperStatus(lsp))));
                    item.add(new Label("sdpId", Model.of(RsvpLspRenderer.getSdpId(lsp))));
                    item.add(new Label("note", Model.of(RsvpLspRenderer.getNote(lsp))));
                    item.add(new Label("lastEditor", Model.of(RsvpLspRenderer.getLastEditor(lsp))));
                    item.add(new Label("lastEditTime", Model.of(RsvpLspRenderer.getLastEditTime(lsp))));
                    item.add(new Label("version", Model.of(RsvpLspRenderer.getVersion(lsp))));

                    NodeDto ingress = RsvpLspUtil.getIngressNode(lsp);
                    NodeDto egress = RsvpLspUtil.getEgressNode(lsp);
                    BookmarkablePageLink<Void> linkIngress = new BookmarkablePageLink<Void>(
                            "ingress", SimpleNodeDetailPage.class, NodeUtil.getParameters(ingress));
                    linkIngress.add(new Label("nodeName", NodeRenderer.getNodeName(ingress)));
                    BookmarkablePageLink<Void> linkEgress = new BookmarkablePageLink<Void>(
                            "egress", SimpleNodeDetailPage.class, NodeUtil.getParameters(egress));
                    linkEgress.add(new Label("nodeName", NodeRenderer.getNodeName(egress)));
                    item.add(linkIngress);
                    item.add(linkEgress);

                    Link<Void> pairLsp = new Link<Void>("pairLsp") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            setResponsePage(new RsvpLspEditPage(RsvpLspPoolDetailPage.this, pair));
                        }
                    };
                    item.add(pairLsp);
                    pairLsp.add(new Label("lspName", Model.of(RsvpLspRenderer.getLspName(pair))));

                    final RsvpLspHopSeriesDto primary = lsp.getHopSeries1();
                    Link<Void> primaryPathLink = new Link<Void>("primary") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            WebPage page = new RsvpLspPathHopEditPage(RsvpLspPoolDetailPage.this, primary);
                            setResponsePage(page);
                        }
                    };
                    primaryPathLink.setEnabled(primary != null);
                    primaryPathLink.setVisible(primary != null);
                    item.add(primaryPathLink);
                    primaryPathLink.add(new Label("pathName", Model.of(PathRenderer.getName(primary))));

                    final RsvpLspHopSeriesDto secondary = lsp.getHopSeries2();
                    Link<Void> secondaryPathLink = new Link<Void>("secondary") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            WebPage page = new RsvpLspPathHopEditPage(RsvpLspPoolDetailPage.this, secondary);
                            setResponsePage(page);
                        }
                    };
                    secondaryPathLink.setEnabled(secondary != null);
                    secondaryPathLink.setVisible(secondary != null);
                    item.add(secondaryPathLink);
                    secondaryPathLink.add(new Label("pathName", Model.of(PathRenderer.getName(secondary))));

                    item.add(new Label("mainOperStatus", RsvpLspRenderer.getMainPathOperationStatus(lsp)));
                    item.add(new Label("backupOperStatus", RsvpLspRenderer.getBackupPathOperationStatus(lsp)));

                    Link<Void> deleteIDLink = new Link<Void>("deleteID") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            ReleaseNetworkIDConfirmationPage page =
                                    new ReleaseNetworkIDConfirmationPage(RsvpLspPoolDetailPage.this,
                                            RELEASE_OPERATION_NAME, lsp);
                            setResponsePage(page);
                        }
                    };
                    deleteIDLink.setEnabled(lsp != null);
                    deleteIDLink.setVisible(lsp != null);
                    item.add(deleteIDLink);
                    item.add(HistoryUtil.createHistoryLink(lsp, "History"));

                    try {
                        String taskName = TaskUtil.getTaskName(lsp);
                        Label taskNameLabel = new Label("taskName", Model.of(taskName));
                        item.add(taskNameLabel);
                        Link<Void> deleteTaskLink = new Link<Void>("deleteTask") {
                            private static final long serialVersionUID = 1L;

                            public void onClick() {
                                List<TaskCommandBuilder> builders = new ArrayList<TaskCommandBuilder>();
                                try {
                                    createDeleteTask(builders, lsp);
                                    WebPage page = new CommandExecutionConfirmationPage(
                                            RsvpLspPoolDetailPage.this, "Do you want to delete task?", builders);
                                    setResponsePage(page);
                                } catch (Exception e) {
                                    log().error("cannot create task-deletion-commands.", e);
                                }
                            }
                        };
                        deleteTaskLink.setEnabled(taskName != null);
                        deleteTaskLink.setVisible(taskName != null);
                        item.add(deleteTaskLink);
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }

                private void createDeleteTask(List<TaskCommandBuilder> builders, RsvpLspDto lsp)
                        throws IOException, InventoryException, ExternalServiceException {
                    if (lsp == null) {
                        return;
                    }
                    TaskCommandBuilder builder = new TaskCommandBuilder(lsp, editorName);
                    builder.buildDeleteCommand();
                    builders.add(builder);
                }
            };
            add(lspRows);
            addNewRsvpLspLink("addNewRsvpLsp1", pool);
            addNewRsvpLspLink("addNewRsvpLsp2", pool);
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
        PerfLog.info(startTime, System.currentTimeMillis(), "PseudoWirePoolDetailPage:onPageAttach->onDetach");
    }

    @Override
    protected void onModelChanged() {
        pool.renew();
        poolModel.renew();
        lspModel.renew();
        super.onModelChanged();
    }

    private Logger log() {
        return LoggerFactory.getLogger(RsvpLspPoolDetailPage.class);
    }

    private void addNewRsvpLspLink(final String id, final RsvpLspIdPoolDto pool) {
        Link<Void> newPseudoWire = new Link<Void>(id) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                setResponsePage(new RsvpLspCreationPage(RsvpLspPoolDetailPage.this, pool));
            }
        };
        add(newPseudoWire);
    }

    ;

}