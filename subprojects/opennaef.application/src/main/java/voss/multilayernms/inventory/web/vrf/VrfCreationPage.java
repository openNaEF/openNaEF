package voss.multilayernms.inventory.web.vrf;

import naef.dto.vrf.VrfDto;
import naef.dto.vrf.VrfStringIdPoolDto;
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
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.builder.VrfBuilder;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.PageUtil;

import java.util.HashMap;
import java.util.Map;

public class VrfCreationPage extends WebPage {
    private static final Logger log = LoggerFactory.getLogger(VrfCreationPage.class);
    public static final String OPERATION_NAME = "VrfCreation";
    private final VrfStringIdPoolDto pool;
    private String vrfId;
    private final String editorName;
    private final WebPage backPage;
    private final Map<Integer, VrfDto> published = new HashMap<Integer, VrfDto>();

    public VrfCreationPage(WebPage backPage, VrfStringIdPoolDto pool) {
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
                    String vrfId = getVrfId();
                    log.debug("selected: " + vrfId);
                    if (vrfId == null) {
                        return;
                    }
                    try {
                        VrfDto publishedVrf = published.get(vrfId);
                        if (publishedVrf == null) {
                            VrfStringIdPoolDto pool = getPool();
                            String customerID = null;
                            VrfBuilder.createVrf(pool, vrfId, customerID, null, null, editorName);
                            pool.renew();
                            publishedVrf = VrfWebUtil.getVrf(pool, vrfId);
                        } else {
                            ShellCommands commands = new ShellCommands(editorName);
                            InventoryBuilder.changeContext(commands, publishedVrf);
                            commands.addVersionCheckTarget(publishedVrf);
                            commands.addLastEditCommands();
                            ShellConnector.getInstance().execute2(commands);
                            publishedVrf.renew();
                        }
                        PageUtil.setModelChanged(getBackPage());
                        getBackPage().modelChanged();
                        setResponsePage(new VrfEditPage(getBackPage(), publishedVrf));
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            form.add(proceedButton);

            TextField<String> vlanIdField = new TextField<String>("vrfId", new PropertyModel<String>(this, "vrfId"));
            form.add(vlanIdField);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public VrfStringIdPoolDto getPool() {
        return this.pool;
    }

    public String getVrfId() {
        return this.vrfId;
    }

    public void setVrfId(String id) {
        this.vrfId = id;
    }

    public WebPage getBackPage() {
        return this.backPage;
    }
}