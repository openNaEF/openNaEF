package voss.multilayernms.inventory.web.mpls;

import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.PseudowireStringIdPoolDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.GenericRenderer;
import voss.nms.inventory.model.PseudoWireStringPoolModel;
import voss.nms.inventory.util.*;

import java.util.ArrayList;
import java.util.List;

public class PseudoWirePoolListPage extends WebPage {
    public static final String OPERATION_NAME = "PseudoWirePoolList";
    private final Link<Void> newPoolButton1;
    private final Link<Void> newPoolButton2;
    private final ListView<PseudowireStringIdPoolDto> pseudoWirePools;

    public static final String KEY_FILTER_NODE = "filterNode";
    public static final String KEY_FILTER_POOL = "filterPool";
    public static final String KEY_FILTER_ID = "filterId";
    private final PseudoWirePoolsModel pseudoWirePoolsModel;
    private String nodeFilter;
    private String poolFilter;
    private String idFilter;
    private int size;

    public PseudoWirePoolListPage() {
        this(new PageParameters());
    }

    public PseudoWirePoolListPage(PageParameters param) {
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            Link<Void> refreshLink = new BookmarkablePageLink<Void>("refresh", PseudoWirePoolListPage.class);
            add(refreshLink);
            log().debug(param.toString());
            setNodeFilter(param.getString(KEY_FILTER_NODE));
            setPoolFilter(param.getString(KEY_FILTER_POOL));
            setIdFilter(param.getString(KEY_FILTER_ID));

            Form<Void> filterForm = new Form<Void>("filterForm") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    log().debug("called filterForm");
                }
            };
            add(filterForm);
            Button filterButton = new Button("filter") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    log().debug("called filterButton");
                    PageParameters param = new PageParameters();
                    param.add(KEY_FILTER_NODE, getNodeFilter());
                    param.add(KEY_FILTER_ID, getIdFilter());
                    param.add(KEY_FILTER_POOL, getPoolFilter());
                    PseudoWirePoolListPage page = new PseudoWirePoolListPage(param);
                    setResponsePage(page);
                }
            };
            filterForm.add(filterButton);
            Button clearFilterButton = new Button("clearFilter") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    log().debug("called clearFilterButton");
                    setResponsePage(PseudoWirePoolListPage.class);
                }
            };
            filterForm.add(clearFilterButton);
            TextField<String> nodeFilter = new TextField<String>("nodeFilter", new PropertyModel<String>(this, "nodeFilter"));
            filterForm.add(nodeFilter);
            TextField<String> poolFilter = new TextField<String>("poolFilter", new PropertyModel<String>(this, "poolFilter"));
            filterForm.add(poolFilter);
            TextField<String> vlanFilter = new TextField<String>("idFilter", new PropertyModel<String>(this, "idFilter"));
            filterForm.add(vlanFilter);

            this.newPoolButton1 = createNewPoolButton("newPoolButton1");
            add(this.newPoolButton1);

            this.newPoolButton2 = createNewPoolButton("newPoolButton2");
            add(this.newPoolButton2);

            this.pseudoWirePoolsModel = new PseudoWirePoolsModel();

            Label filterResult = new Label("filterResult", new PropertyModel<Integer>(this, "size"));
            add(filterResult);

            this.pseudoWirePools = new ListView<PseudowireStringIdPoolDto>("pseudoWirePools", pseudoWirePoolsModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<PseudowireStringIdPoolDto> item) {
                    final PseudowireStringIdPoolDto pool = item.getModelObject();
                    PseudoWireStringPoolModel model = new PseudoWireStringPoolModel(pool);
                    String endOfUseClass = null;
                    if (NodeUtil.isEndOfUse(GenericRenderer.getOperStatus(pool))) {
                        endOfUseClass = "end-of-use";
                    }
                    AttributeAppender appender = new AttributeAppender("class", new Model<String>(endOfUseClass), " ");
                    item.add(appender);
                    item.add(new Label("domain", new Model<String>("Nationwide")));
                    Link<Void> poolLink = new Link<Void>("poolLink") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            setResponsePage(PseudoWirePoolDetailPage.class, PseudoWireUtil.getPoolParam(pool));
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
                    Link<Void> link = new Link<Void>("editPoolButton") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            setResponsePage(new PseudoWirePoolEditPage(PseudoWirePoolListPage.this, pool));
                        }
                    };
                    item.add(link);
                    item.add(HistoryUtil.createHistoryLink(pool));
                }
            };
            add(this.pseudoWirePools);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private List<PseudowireStringIdPoolDto> filter(List<PseudowireStringIdPoolDto> pools) {
        List<PseudowireStringIdPoolDto> result = new ArrayList<PseudowireStringIdPoolDto>();
        if(pools == null){
            return result;
        }
        for (PseudowireStringIdPoolDto pool : pools) {
            log().debug(pool.getName());
            if (pool.getName().contains(ATTR.DELETED)) {
                continue;
            }
            if (poolFilter != null && !pool.getName().toLowerCase().contains(poolFilter.toLowerCase())) {
                continue;
            }
            if (nodeFilter != null && !containsNode(pool, nodeFilter)) {
                continue;
            }
            if (idFilter != null && !containsId(pool, idFilter)) {
                continue;
            }
            result.add(pool);
        }
        return result;
    }

    private boolean containsNode(PseudowireStringIdPoolDto pool, String nodeFilter) {
        for (PseudowireDto user : pool.getUsers()) {
            if (user.getAc1() != null &&
                    user.getAc1().getNode().getName().toLowerCase().contains(nodeFilter.toLowerCase())) {
                return true;
            } else if (user.getAc2() != null &&
                    user.getAc2().getNode().getName().toLowerCase().contains(nodeFilter.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsId(PseudowireStringIdPoolDto pool, String idFilter) {
        return true;
    }

    private Link<Void> createNewPoolButton(String id) {
        Link<Void> link = new Link<Void>(id) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                setResponsePage(new PseudoWirePoolEditPage(PseudoWirePoolListPage.this, null));
            }
        };
        return link;
    }

    private Logger log() {
        return LoggerFactory.getLogger(PseudoWirePoolListPage.class);
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
        this.pseudoWirePoolsModel.renew();
        super.onModelChanged();
    }

    private class PseudoWirePoolsModel extends AbstractReadOnlyModel<List<PseudowireStringIdPoolDto>> {
        private static final long serialVersionUID = 1L;
        private final List<PseudowireStringIdPoolDto> pools = new ArrayList<PseudowireStringIdPoolDto>();

        public PseudoWirePoolsModel() {
            renew();
        }

        @Override
        public synchronized List<PseudowireStringIdPoolDto> getObject() {
            return this.pools;
        }

        public synchronized void renew() {
            try {
                List<PseudowireStringIdPoolDto> pseudoWirePools = MplsNmsInventoryConnector.getInstance().getPseudoWireStringIdPools();
                pseudoWirePools = filter(pseudoWirePools);
                setSize(pseudoWirePools.size());
                this.pools.clear();
                this.pools.addAll(pseudoWirePools);
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
    }
}