package voss.multilayernms.inventory.web.vrf;


import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.PerfLog;
import voss.multilayernms.inventory.web.parts.VrfIfPanel;
import voss.nms.inventory.util.AAAWebUtil;

public class VrfListPage extends WebPage {
    public static final String OPERATION_NAME = "VrfList";
    public static final String KEY_NODE_NAME = "node";
    public static final String KEY_Vrf_ID = "VrfId";
    private final String editorName;
    private final VrfModel VrfModel;
    private long startTime = System.currentTimeMillis();
    private final WebPage backPage;

    public WebPage getBackPage() {
        return backPage;
    }

    public VrfListPage(PageParameters param) {
        this(null, param);
    }

    public VrfListPage(WebPage backPage, PageParameters param) {
        this.backPage = backPage;

        try {
            long prev = System.currentTimeMillis();
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);

            long time = System.currentTimeMillis();
            PerfLog.info(prev, time, "build header component");
            prev = time;
            String nodeName = param.getString(KEY_NODE_NAME);
            String VrfId = param.getString(KEY_Vrf_ID);

            this.VrfModel = new VrfModel(nodeName, VrfId);

            time = System.currentTimeMillis();
            PerfLog.info(prev, time, "build vrf Model");
            prev = time;

            VrfIfPanel VrfIfPanel = new VrfIfPanel("vrfIfPanel", null, VrfModel, editorName);
            add(VrfIfPanel);

            Link<Void> editLink = new Link<Void>("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    if (getBackPage() == null) {
                        return;
                    }
                    setResponsePage(getBackPage());
                }
            };
            editLink.setEnabled(getBackPage() != null);
            add(editLink);

            time = System.currentTimeMillis();
            PerfLog.info(prev, time, "VrfListPage() - end");
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
        PerfLog.info(startTime, System.currentTimeMillis(), "VrfListPage->onDetach");
    }

    @Override
    protected void onModelChanged() {
        try {
            log().debug("model changed.");
            this.VrfModel.renew();
            super.onModelChanged();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }


    private Logger log() {
        return LoggerFactory.getLogger(VrfListPage.class);
    }

}