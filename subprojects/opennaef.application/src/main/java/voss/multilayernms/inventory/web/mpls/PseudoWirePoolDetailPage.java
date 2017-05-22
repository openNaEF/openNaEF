package voss.multilayernms.inventory.web.mpls;

import naef.dto.PortDto;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.PseudowireStringIdPoolDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.behavior.AttributeAppender;
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
import voss.core.server.util.DtoUtil;
import voss.core.server.util.PerfLog;
import voss.multilayernms.inventory.renderer.PseudoWireRenderer;
import voss.multilayernms.inventory.web.node.NodePageUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.model.PseudoWireStringPoolModel;
import voss.nms.inventory.session.InventoryWebSession;
import voss.nms.inventory.util.*;

import java.util.Map;

public class PseudoWirePoolDetailPage extends WebPage {
    public static final String OPERATION_NAME = "PseudoWirePoolDetail";
    public static final String RELEASE_OPERATION_NAME = "PseudoWirePoolIdRelease";
    public static final String KEY_PSEUDOWIRE_ID = "pwID";

    private final Label domain;
    private final Label range;
    private final Label remains;
    private final Label used;
    private final Label poolName;
    private final Label purpose;
    private final Label status;
    private final Link<Void> editPool;
    private final PseudowireStringIdPoolDto pool;
    private final PseudoWireStringPoolModel poolModel;
    private final PseudoWireIdsModel pwsModel;
    private long startTime = System.currentTimeMillis();

    public PseudoWirePoolDetailPage() {
        this(new PageParameters());
    }

    public PseudoWirePoolDetailPage(PageParameters param) {
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            pool = PseudoWireUtil.getStringPool(param);
            if (pool == null) {
                throw new IllegalStateException("unknown pool: " + param);
            }

            InventoryWebSession session = (InventoryWebSession) getSession();
            this.pwsModel = new PseudoWireIdsModel(pool);
            pwsModel.renew(session.isShowAllPseudoWires());
            final Map<String, PseudowireDto> pseudowires = pwsModel.getVcsMap();

            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            BookmarkablePageLink<Void> parentLink = new BookmarkablePageLink<Void>("parent", PseudoWirePoolListPage.class);
            add(parentLink);
            BookmarkablePageLink<Void> refreshLink = new BookmarkablePageLink<Void>("refresh",
                    PseudoWirePoolDetailPage.class, PseudoWireUtil.getPoolParam(pool));
            add(refreshLink);

            this.poolModel = new PseudoWireStringPoolModel(pool);
            this.domain = new Label("domain", new Model<String>("Nationwide"));
            add(this.domain);
            this.range = new Label("range", new PropertyModel<String>(poolModel, "range"));
            add(this.range);
            this.remains = new Label("remains", new PropertyModel<String>(poolModel, "remains"));
            add(this.remains);
            this.used = new Label("used", new PropertyModel<String>(poolModel, "used"));
            add(this.used);
            this.poolName = new Label("poolName", new PropertyModel<String>(poolModel, "poolName"));
            add(this.poolName);
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

            this.editPool = new Link<Void>("editPool") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    setResponsePage(new PseudoWirePoolEditPage(PseudoWirePoolDetailPage.this, pool));
                }
            };
            add(this.editPool);

            final ListView<String> pseudoWires = new ListView<String>("pseudoWires", pwsModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<String> item) {
                    final String vcId = item.getModelObject();
                    final PseudowireDto pw = pseudowires.get(vcId);

                    String endOfUseClass = null;
                    if (NodeUtil.isEndOfUse(DtoUtil.getString(pw, "運用状態"))) {
                        endOfUseClass = "end-of-use";
                    }
                    AttributeAppender appender = new AttributeAppender("class", new Model<String>(endOfUseClass), " ");
                    item.add(appender);

                    Map<String, String> values = PseudoWireRenderer.getValues(pw);
                    Link<Void> editPseudoWire = new Link<Void>("editPseudoWire") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            if (pw == null) {
                                setResponsePage(new PseudoWireCreationPage(PseudoWirePoolDetailPage.this, pool, vcId));
                            } else {
                                setResponsePage(new PseudoWireEditPage(PseudoWirePoolDetailPage.this, pw));
                            }
                        }
                    };
                    item.add(editPseudoWire);
                    editPseudoWire.add(new Label("pwId", new Model<String>(vcId.toString())));
                    item.add(new Label("facilityStatus", new PropertyModel<String>(values, MPLSNMS_ATTR.FACILITY_STATUS)));
                    item.add(new Label("operStatus", new PropertyModel<String>(values, MPLSNMS_ATTR.OPER_STATUS)));
                    item.add(new Label("interworking", PseudoWireUtil.getInterworking(pw)));
                    item.add(new Label("note", Model.of(PseudoWireRenderer.getNote(pw))));
                    item.add(new Label("lastEditor", Model.of(PseudoWireRenderer.getLastEditor(pw))));
                    item.add(new Label("lastEditTime", Model.of(PseudoWireRenderer.getLastEditTime(pw))));
                    item.add(new Label("version", Model.of(PseudoWireRenderer.getVersion(pw))));
                    PortDto ac1 = (pw == null ? null : pw.getAc1());
                    PortDto ac2 = (pw == null ? null : pw.getAc2());
                    populateAttachedCircuit(item, pw, ac1, "1");
                    populateAttachedCircuit(item, pw, ac2, "2");
                    Link<Void> deleteIDLink = new Link<Void>("deleteID") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            ReleaseNetworkIDConfirmationPage page =
                                    new ReleaseNetworkIDConfirmationPage(PseudoWirePoolDetailPage.this,
                                            RELEASE_OPERATION_NAME, pw);
                            setResponsePage(page);
                        }
                    };
                    deleteIDLink.setEnabled(pw != null);
                    deleteIDLink.setVisible(pw != null);
                    item.add(deleteIDLink);
                    item.add(HistoryUtil.createHistoryLink(pw, "History"));
                }

                private void populateAttachedCircuit(final ListItem<String> item,
                                                     final PseudowireDto pw, final PortDto port, String suffix) {
                    String ifName = NameUtil.getIfName(port);
                    Link<Void> nodeLink = NodePageUtil.createNodeLink("node_" + suffix, port);
                    String vpi = null;
                    String vci = null;
                    if (port != null && port instanceof AtmPvcIfDto) {
                        AtmPvcIfDto pvc = (AtmPvcIfDto) port;
                        vpi = pvc.getVpi().toString();
                        vci = pvc.getVci().toString();
                    }
                    item.add(nodeLink);
                    item.add(new Label("ifName_" + suffix, new Model<String>(ifName)));
                    item.add(new Label("vpi_" + suffix, new Model<String>(vpi)));
                    item.add(new Label("vci_" + suffix, new Model<String>(vci)));
                }
            };
            add(pseudoWires);
            addNewPseudoWireLink("newPseudoWire1", pool);
            addNewPseudoWireLink("newPseudoWire2", pool);

        } catch (Exception e) {
            throw new IllegalStateException(e);
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
        return LoggerFactory.getLogger(PseudoWirePoolDetailPage.class);
    }

    private void addNewPseudoWireLink(final String id, final PseudowireStringIdPoolDto pool) {
        Link<Void> newPseudoWire = new Link<Void>(id) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                setResponsePage(new PseudoWireCreationPage(PseudoWirePoolDetailPage.this, pool));
            }
        };
        long remains = pool.getTotalNumberOfIds() - (long) pool.getUsers().size();
        newPseudoWire.setEnabled(remains > 0);
        add(newPseudoWire);
    }

    ;

}