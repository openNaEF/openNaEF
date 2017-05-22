package voss.multilayernms.inventory.web.mpls;

import naef.dto.NodeDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.dto.mpls.RsvpLspHopSeriesIdPoolDto;
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
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.PerfLog;
import voss.core.server.util.TaskUtil;
import voss.multilayernms.inventory.renderer.PathRenderer;
import voss.multilayernms.inventory.web.node.SimpleNodeDetailPage;
import voss.multilayernms.inventory.web.parts.CommandExecutionConfirmationPage;
import voss.nms.inventory.model.RsvpLspPathHopPoolModel;
import voss.nms.inventory.util.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RsvpLspPathHopPoolDetailPage extends WebPage {
    public static final String OPERATION_NAME = "RsvpLspPathHopPoolDetail";
    public static final String RELEASE_OPERATION_NAME = "RsvpLspPathHopIdRelease";
    public static final String KEY_PSEUDOWIRE_ID = "pwID";

    private final String editorName;
    private final Label range;
    private final Label used;
    private final Label poolName;
    private final Label purpose;
    private final Label status;
    private final RsvpLspHopSeriesIdPoolDto pool;
    private final RsvpLspPathHopPoolModel poolModel;
    private final RsvpLspPathHopIdsModel pwsModel;
    private long startTime = System.currentTimeMillis();

    public RsvpLspPathHopPoolDetailPage() {
        this(new PageParameters());
    }

    public RsvpLspPathHopPoolDetailPage(PageParameters param) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            pool = MplsPageUtil.getRsvpLspPathPool(param);
            if (pool == null) {
                throw new IllegalStateException("unknown pool: " + param);
            }

            this.pwsModel = new RsvpLspPathHopIdsModel(pool);
            this.pwsModel.renew();
            final Map<String, RsvpLspHopSeriesDto> pathHops = pwsModel.getPathMap();

            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            BookmarkablePageLink<Void> parentLink = new BookmarkablePageLink<Void>("parent", RsvpLspPathHopPoolListPage.class);
            add(parentLink);
            BookmarkablePageLink<Void> reloadLink = new BookmarkablePageLink<Void>("refresh",
                    RsvpLspPathHopPoolDetailPage.class, MplsPageUtil.getRsvpLspPathPoolParam(pool));
            add(reloadLink);

            this.poolModel = new RsvpLspPathHopPoolModel(pool);
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

            final ListView<String> pathes = new ListView<String>("pathes", pwsModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<String> item) {
                    final String pathName = item.getModelObject();
                    final RsvpLspHopSeriesDto path = pathHops.get(pathName);
                    Link<Void> editPseudoWire = new Link<Void>("editPath") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            setResponsePage(new RsvpLspPathHopEditPage(RsvpLspPathHopPoolDetailPage.this, path));
                        }
                    };
                    item.add(editPseudoWire);
                    editPseudoWire.add(new Label("id", new Model<String>(path.getName())));

                    item.add(new Label("pathName", new Model<String>(PathRenderer.getName(path))));
                    NodeDto ingress = RsvpLspUtil.getIngressNode(path);
                    NodeDto egress = RsvpLspUtil.getEgressNode(path);
                    BookmarkablePageLink<Void> fromLink = new BookmarkablePageLink<Void>("from",
                            SimpleNodeDetailPage.class, NodeUtil.getParameters(ingress));
                    Label fromName = new Label("name", Model.of(PathRenderer.getIngressNodeName(path)));
                    fromLink.add(fromName);
                    item.add(fromLink);
                    BookmarkablePageLink<Void> toLink = new BookmarkablePageLink<Void>("to",
                            SimpleNodeDetailPage.class, NodeUtil.getParameters(egress));
                    Label toName = new Label("name", Model.of(PathRenderer.getEgressNodeName(path)));
                    toLink.add(toName);
                    item.add(toLink);
                    item.add(new Label("note", Model.of(PathRenderer.getNote(path))));
                    item.add(new Label("lastEditor", Model.of(PathRenderer.getLastEditor(path))));
                    item.add(new Label("lastEditTime", Model.of(PathRenderer.getLastEditTime(path))));
                    item.add(new Label("version", Model.of(PathRenderer.getVersion(path))));
                    Link<Void> deleteIDLink = new Link<Void>("deleteID") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            ReleaseNetworkIDConfirmationPage page =
                                    new ReleaseNetworkIDConfirmationPage(RsvpLspPathHopPoolDetailPage.this,
                                            RELEASE_OPERATION_NAME, path);
                            setResponsePage(page);
                        }
                    };
                    deleteIDLink.setEnabled(path != null);
                    deleteIDLink.setVisible(path != null);
                    item.add(deleteIDLink);
                    item.add(HistoryUtil.createHistoryLink(path, "History"));
                    try {
                        String taskName = TaskUtil.getTaskName(path);
                        Label taskNameLabel = new Label("taskName", Model.of(taskName));
                        item.add(taskNameLabel);
                        Link<Void> deleteTaskLink = new Link<Void>("deleteTask") {
                            private static final long serialVersionUID = 1L;

                            public void onClick() {
                                List<TaskCommandBuilder> builders = new ArrayList<TaskCommandBuilder>();
                                try {
                                    createDeleteTask(builders, path);
                                    WebPage page = new CommandExecutionConfirmationPage(
                                            RsvpLspPathHopPoolDetailPage.this, "Do you want to delete task?", builders);
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

                private void createDeleteTask(List<TaskCommandBuilder> builders, RsvpLspHopSeriesDto path)
                        throws IOException, InventoryException, ExternalServiceException {
                    if (path == null) {
                        return;
                    }
                    TaskCommandBuilder builder = new TaskCommandBuilder(path, editorName);
                    builder.buildDeleteCommand();
                    builders.add(builder);
                }
            };
            add(pathes);
            addNewPathLink("newPath1", pool);
            addNewPathLink("newPath2", pool);
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
        pwsModel.renew();
        super.onModelChanged();
    }

    private Logger log() {
        return LoggerFactory.getLogger(RsvpLspPathHopPoolDetailPage.class);
    }

    private void addNewPathLink(final String id, final RsvpLspHopSeriesIdPoolDto pool) {
        Link<Void> newPath = new Link<Void>(id) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                setResponsePage(new RsvpLspPathCreationPage(RsvpLspPathHopPoolDetailPage.this, pool));
            }
        };
        add(newPath);
    }

    ;

}