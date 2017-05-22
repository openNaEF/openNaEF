package voss.multilayernms.inventory.web.mpls;

import naef.dto.mpls.RsvpLspHopSeriesIdPoolDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.nms.inventory.model.RsvpLspPathHopPoolModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.HistoryUtil;
import voss.nms.inventory.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;

public class RsvpLspPathHopPoolListPage extends WebPage {
    public static final String OPERATION_NAME = "RsvpLspPathHopPoolList";
    private final ListView<RsvpLspHopSeriesIdPoolDto> pools;
    private final PoolsModel poolsModel;
    private String nodeFilter;
    private String poolFilter;
    private String idFilter;
    private int size;

    public RsvpLspPathHopPoolListPage() {
        this(new PageParameters());
    }

    public RsvpLspPathHopPoolListPage(PageParameters param) {
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            Link<Void> reloadLink = new BookmarkablePageLink<Void>("refresh", RsvpLspPathHopPoolListPage.class);
            add(reloadLink);
            log().debug(param.toString());

            this.poolsModel = new PoolsModel();
            this.pools = new ListView<RsvpLspHopSeriesIdPoolDto>("pseudoWirePools", poolsModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<RsvpLspHopSeriesIdPoolDto> item) {
                    final RsvpLspHopSeriesIdPoolDto pool = item.getModelObject();
                    RsvpLspPathHopPoolModel model = new RsvpLspPathHopPoolModel(pool);
                    Link<Void> poolLink = new Link<Void>("poolLink") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            setResponsePage(RsvpLspPathHopPoolDetailPage.class, MplsPageUtil.getRsvpLspPathPoolParam(pool));
                        }
                    };
                    item.add(poolLink);
                    Label poolName = new Label("poolName", new PropertyModel<String>(model, "poolName"));
                    poolLink.add(poolName);
                    item.add(poolLink);
                    item.add(new Label("range", new PropertyModel<String>(model, "range")));
                    item.add(new Label("purpose", new PropertyModel<String>(model, "usage")));
                    item.add(new Label("status", new PropertyModel<String>(model, "status")));
                    item.add(new Label("note", new PropertyModel<String>(model, "note")));
                    item.add(HistoryUtil.createHistoryLink(pool, "History"));
                }
            };
            add(this.pools);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    @SuppressWarnings("unused")
    private Link<Void> createNewPoolButton(String id) {
        Link<Void> link = new Link<Void>(id) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                setResponsePage(new PseudoWirePoolEditPage(RsvpLspPathHopPoolListPage.this, null));
            }
        };
        return link;
    }

    private Logger log() {
        return LoggerFactory.getLogger(RsvpLspPathHopPoolListPage.class);
    }

    public String getNodeFilter() {
        return nodeFilter;
    }

    public void setNodeFilter(String nodeFilter) {
        this.nodeFilter = nodeFilter;
    }

    public String getPoolFilter() {
        return poolFilter;
    }

    public void setPoolFilter(String poolFilter) {
        this.poolFilter = poolFilter;
    }

    public String getIdFilter() {
        return idFilter;
    }

    public void setIdFilter(String idFilter) {
        this.idFilter = idFilter;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    protected void onModelChanged() {
        this.poolsModel.renew();
        super.onModelChanged();
    }

    private class PoolsModel extends AbstractReadOnlyModel<List<RsvpLspHopSeriesIdPoolDto>> {
        private static final long serialVersionUID = 1L;
        private final List<RsvpLspHopSeriesIdPoolDto> pools = new ArrayList<RsvpLspHopSeriesIdPoolDto>();

        public PoolsModel() {
            renew();
        }

        @Override
        public synchronized List<RsvpLspHopSeriesIdPoolDto> getObject() {
            return this.pools;
        }

        public synchronized void renew() {
            try {
                List<RsvpLspHopSeriesIdPoolDto> allPools = MplsNmsInventoryConnector.getInstance().getRsvpLspHopSeriesIdPool();
                setSize(allPools.size());
                this.pools.clear();
                this.pools.addAll(allPools);
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
    }

    ;
}