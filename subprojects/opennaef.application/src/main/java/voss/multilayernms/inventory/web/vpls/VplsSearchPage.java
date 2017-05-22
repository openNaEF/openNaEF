package voss.multilayernms.inventory.web.vpls;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.PerfLog;
import voss.nms.inventory.util.AAAWebUtil;

public class VplsSearchPage extends WebPage {
    public static final String OPERATION_NAME = "VplsSearch";
    public static final String KEY_NODE_NAME = "node";
    public static final String KEY_VPLS_ID = "vplsId";
    private final String editorName;
    private long startTime = System.currentTimeMillis();
    private final Form<Void> form;
    private final String node = null;
    private final String vplsId = null;


    public VplsSearchPage() {
        this(new PageParameters());
    }

    public VplsSearchPage(PageParameters param) {
        try {
            long prev = System.currentTimeMillis();
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);

            long time = System.currentTimeMillis();


            this.form = new Form<Void>("form");
            add(form);

            this.addTextField(this.KEY_NODE_NAME, this.KEY_NODE_NAME);
            this.addTextField(this.KEY_VPLS_ID, this.KEY_VPLS_ID);

            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    PageParameters param = new PageParameters();
                    param.add(KEY_NODE_NAME, node);
                    param.add(KEY_VPLS_ID, vplsId);
                    WebPage page = new VplsListPage(getWebPage(), param);
                    setResponsePage(page);
                }
            };
            this.form.add(proceedButton);


            time = System.currentTimeMillis();
            PerfLog.info(prev, time, "NodeDetailPage() - end");
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private TextField<String> addTextField(String id, String attributeName) {
        return addTextField(id, attributeName, null);
    }

    private TextField<String> addTextField(String id, String attributeName, AbstractValidator<String> validator) {
        TextField<String> tf = new TextField<String>(id, new PropertyModel<String>(this, attributeName), String.class);
        this.form.add(tf);
        if (validator != null) {
            tf.add(validator);
        }
        return tf;
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
            super.onModelChanged();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }


    private Logger log() {
        return LoggerFactory.getLogger(VplsSearchPage.class);
    }

}