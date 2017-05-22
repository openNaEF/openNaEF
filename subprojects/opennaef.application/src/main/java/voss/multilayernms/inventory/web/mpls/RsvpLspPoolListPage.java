package voss.multilayernms.inventory.web.mpls;

import naef.dto.mpls.RsvpLspIdPoolDto;
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
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.RsvpLspRenderer;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.model.RsvpLspPoolModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.HistoryUtil;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;

public class RsvpLspPoolListPage extends WebPage {
    public static final String OPERATION_NAME = "RsvpLspPoolList";
    private final ListView<RsvpLspIdPoolDto> pseudoWirePools;

    public static final String KEY_FILTER_NODE = "filterNode";
    public static final String KEY_FILTER_POOL = "filterPool";
    public static final String KEY_FILTER_ID = "filterId";
    private final RsvpLspPoolsModel rsvpLspPoolsModel;

    public RsvpLspPoolListPage() {
        this(new PageParameters());
    }

    public RsvpLspPoolListPage(PageParameters param) {
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            Link<Void> reloadLink = new BookmarkablePageLink<Void>("refresh", RsvpLspPoolListPage.class);
            add(reloadLink);
            log().debug(param.toString());

            this.rsvpLspPoolsModel = new RsvpLspPoolsModel();

            this.pseudoWirePools = new ListView<RsvpLspIdPoolDto>("rsvpLspPools", rsvpLspPoolsModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<RsvpLspIdPoolDto> item) {
                    final RsvpLspIdPoolDto pool = item.getModelObject();
                    RsvpLspPoolModel model = new RsvpLspPoolModel(pool);
                    String endOfUseClass = null;
                    if (NodeUtil.isEndOfUse(RsvpLspRenderer.getAdminStatus(pool))) {
                        endOfUseClass = "end-of-use";
                    }
                    AttributeAppender appender = new AttributeAppender("class", new Model<String>(endOfUseClass), " ");
                    item.add(appender);
                    Link<Void> poolLink = new Link<Void>("poolLink") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            setResponsePage(RsvpLspPoolDetailPage.class, MplsPageUtil.getRsvpLspPoolParam(pool));
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
            add(this.pseudoWirePools);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private Logger log() {
        return LoggerFactory.getLogger(RsvpLspPoolListPage.class);
    }

    @Override
    protected void onModelChanged() {
        this.rsvpLspPoolsModel.renew();
        super.onModelChanged();
    }

    private class RsvpLspPoolsModel extends RenewableAbstractReadOnlyModel<List<RsvpLspIdPoolDto>> {
        private static final long serialVersionUID = 1L;
        private final List<RsvpLspIdPoolDto> pools = new ArrayList<RsvpLspIdPoolDto>();

        public RsvpLspPoolsModel() {
            renew();
        }

        @Override
        public synchronized List<RsvpLspIdPoolDto> getObject() {
            return this.pools;
        }

        public synchronized void renew() {
            try {
                List<RsvpLspIdPoolDto> lspPools = MplsNmsInventoryConnector.getInstance().getRsvpLspIdPool();
                this.pools.clear();
                this.pools.addAll(lspPools);
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
    }

    ;
}