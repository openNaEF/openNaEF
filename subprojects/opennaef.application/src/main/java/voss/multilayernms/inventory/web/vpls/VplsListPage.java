package voss.multilayernms.inventory.web.vpls;


import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.PerfLog;
import voss.multilayernms.inventory.web.parts.VplsIfPanel;
import voss.nms.inventory.util.AAAWebUtil;

public class VplsListPage extends WebPage {
    public static final String OPERATION_NAME = "VplsList";
    public static final String KEY_NODE_NAME = "node";
    public static final String KEY_VPLS_ID = "vplsId";
    private final String editorName;
    private final VplsModel vplsModel;
    private long startTime = System.currentTimeMillis();
    private final WebPage backPage;

    public WebPage getBackPage() {
        return backPage;
    }

    public VplsListPage(WebPage backPage, PageParameters param) {
        this.backPage = backPage;

        try {
            long prev = System.currentTimeMillis();
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);

            long time = System.currentTimeMillis();
            PerfLog.info(prev, time, "build header component");
            prev = time;
            String nodeName = param.getString(KEY_NODE_NAME);
            String vplsId = param.getString(KEY_VPLS_ID);

            this.vplsModel = new VplsModel(nodeName, vplsId);

            time = System.currentTimeMillis();
            PerfLog.info(prev, time, "build portsModel");
            prev = time;

            VplsIfPanel vplsIfPanel = new VplsIfPanel("vplsIfPanel", null, vplsModel, editorName);
            add(vplsIfPanel);

            Link<Void> editLink = new Link<Void>("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    setResponsePage(getBackPage());
                }
            };
            add(editLink);

            time = System.currentTimeMillis();
            PerfLog.info(prev, time, "NodeDetailPage() - end");
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    @Override
    public void onPageAttached() {
        this.startTime = System.currentTimeMillis();
        log().debug("onPageAttached() called.");
        super.onPageAttached();
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        log().info("page rendering time: " + (System.currentTimeMillis() - startTime));
        PerfLog.info(startTime, System.currentTimeMillis(), "SimpleNodeDetailPage:onPageAttach->onDetach");
    }

    @Override
    protected void onModelChanged() {
        try {
            log().debug("model changed.");
            this.vplsModel.renew();
            super.onModelChanged();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }


    private Logger log() {
        return LoggerFactory.getLogger(VplsListPage.class);
    }

}