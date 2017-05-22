package voss.multilayernms.inventory.web.vlan;

import naef.dto.vlan.VlanIdPoolDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
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
import voss.multilayernms.inventory.web.mpls.MplsPageUtil;
import voss.nms.inventory.model.VlanPoolModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.HistoryUtil;
import voss.nms.inventory.util.UrlUtil;
import voss.nms.inventory.util.VlanUtil;

import java.util.ArrayList;
import java.util.List;

public class VlanIdPoolListPage extends WebPage {
    public static final String OPERATION_NAME = "VlanIdPoolList";
    private final ListView<VlanIdPoolDto> pools;
    private final PoolsModel poolsModel;
    private final Link<Void> newPoolButton1;
    private final Link<Void> newPoolButton2;
    private String nodeFilter;
    private String poolFilter;
    private String idFilter;
    private int size;

    public VlanIdPoolListPage() {
        this(new PageParameters());
    }

    public VlanIdPoolListPage(PageParameters param) {
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);

            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            Link<Void> reloadLink = new BookmarkablePageLink<Void>("refresh", VlanIdPoolListPage.class);
            add(reloadLink);
            log().debug(param.toString());

            this.poolsModel = new PoolsModel();
            this.pools = new ListView<VlanIdPoolDto>("vlanIdPools", poolsModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<VlanIdPoolDto> item) {
                    final VlanIdPoolDto pool = item.getModelObject();
                    VlanPoolModel model = new VlanPoolModel(pool);
                    Link<Void> poolLink = new Link<Void>("poolLink") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            setResponsePage(VlanIdPoolDetailPage.class, MplsPageUtil.getVlanIdPoolParam(pool));
                        }
                    };
                    item.add(poolLink);
                    Label poolName = new Label("poolName", new PropertyModel<String>(model, "poolName"));
                    poolLink.add(poolName);
                    item.add(poolLink);
                    item.add(new Label("range", new PropertyModel<String>(model, "range")));
                    item.add(new Label("purpose", new PropertyModel<String>(model, "usage")));
                    item.add(new Label("note", new PropertyModel<String>(model, "note")));
                    Link<Void> editLink = new Link<Void>("editPoolButton") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            setResponsePage(new VlanIdPoolEditPage(VlanIdPoolListPage.this, pool));
                        }
                    };
                    item.add(editLink);
                    Link<Void> deleteLink = new Link<Void>("deletePoolButton") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            setResponsePage(new VlanIdPoolEditPage(VlanIdPoolListPage.this, pool));
                        }
                    };
                    item.add(deleteLink);
                    item.add(HistoryUtil.createHistoryLink(pool, "History"));
                }
            };

            Form<Void> form = new Form<Void>("vlanIdPoolForm");
            add(form);
            form.add(this.pools);
            this.newPoolButton1 = createNewPoolButton("newPoolButton1");
            form.add(this.newPoolButton1);
            this.newPoolButton2 = createNewPoolButton("newPoolButton2");
            form.add(this.newPoolButton2);

        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private Link<Void> createNewPoolButton(String id) {
        Link<Void> link = new Link<Void>(id) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                setResponsePage(new VlanIdPoolEditPage(VlanIdPoolListPage.this, null));
            }
        };
        return link;
    }

    private Logger log() {
        return LoggerFactory.getLogger(VlanIdPoolListPage.class);
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

    private class PoolsModel extends AbstractReadOnlyModel<List<VlanIdPoolDto>> {
        private static final long serialVersionUID = 1L;
        private final List<VlanIdPoolDto> pools = new ArrayList<VlanIdPoolDto>();

        public PoolsModel() {
            renew();
        }

        @Override
        public synchronized List<VlanIdPoolDto> getObject() {
            return this.pools;
        }

        public synchronized void renew() {
            try {
                List<VlanIdPoolDto> allPools = new ArrayList<VlanIdPoolDto>(VlanUtil.getPools());
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