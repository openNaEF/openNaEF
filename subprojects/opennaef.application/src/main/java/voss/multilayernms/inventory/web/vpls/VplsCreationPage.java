package voss.multilayernms.inventory.web.vpls;

import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsStringIdPoolDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.builder.VplsBuilder;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.PageUtil;

public class VplsCreationPage extends WebPage {
    private static final Logger log = LoggerFactory.getLogger(VplsCreationPage.class);
    public static final String OPERATION_NAME = "VplsCreation";
    private final VplsStringIdPoolDto pool;
    private String vplsId;
    private final String editorName;
    private final WebPage backPage;

    public VplsCreationPage(WebPage backPage, VplsStringIdPoolDto pool) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            if (pool == null) {
                throw new IllegalArgumentException("pool is null.");
            }
            this.backPage = backPage;
            this.pool = pool;

            Form<Void> backForm = new Form<Void>("backForm");
            add(backForm);
            Button backButton = new Button("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(getBackPage());
                }
            };
            backForm.add(backButton);

            add(new FeedbackPanel("feedback"));

            Label nodeLabel = new Label("poolName", Model.of(pool.getName()));
            add(nodeLabel);

            Form<Void> form = new Form<Void>("form");
            add(form);


            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    String vplsId = getVplsId();
                    log.debug("selected: " + vplsId);
                    if (vplsId == null) {
                        return;
                    }
                    try {
                        VplsStringIdPoolDto pool = getPool();
                        String customerID = null;
                        VplsBuilder.createVpls(pool, vplsId, customerID, null, null, editorName);
                        pool.renew();
                        PageUtil.setModelChanged(getBackPage());
                        VplsDto vpls = VplsWebUtil.getVpls(pool, vplsId);
                        setResponsePage(new VplsEditPage(getBackPage(), vpls));
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            form.add(proceedButton);

            TextField<String> vlanIdField = new TextField<String>("vplsId", new PropertyModel<String>(this, "vplsId"));
            form.add(vlanIdField);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public VplsStringIdPoolDto getPool() {
        return this.pool;
    }

    public String getVplsId() {
        return this.vplsId;
    }

    public void setVplsId(String id) {
        this.vplsId = id;
    }

    public WebPage getBackPage() {
        return this.backPage;
    }
}