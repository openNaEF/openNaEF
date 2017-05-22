package voss.multilayernms.inventory.web.mpls;

import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.PseudowireStringIdPoolDto;
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
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.builder.PseudoWireStringTypeCommandBuilder;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.PseudoWireUtil;

public class PseudoWireCreationPage extends WebPage {
    public static final String OPERATION_NAME = "PseudoWireCreation";
    private static final Logger log = LoggerFactory.getLogger(PseudoWireCreationPage.class);
    private final PseudowireStringIdPoolDto pool;
    private IdSelectionType selectedType = IdSelectionType.TEXT;
    private String pwId;
    private final String editorName;
    private final WebPage backPage;

    public PseudoWireCreationPage(WebPage backPage, PseudowireStringIdPoolDto pool) {
        this(backPage, pool, null);
    }

    public PseudoWireCreationPage(WebPage backPage, PseudowireStringIdPoolDto pool, String defaultVcId) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            if (pool == null) {
                throw new IllegalArgumentException("pool is null.");
            }
            this.pool = pool;
            this.backPage = backPage;
            this.pwId = defaultVcId;

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
                    processCreate();
                    PseudowireDto publishedPw = PseudoWireUtil.getPseudoWire(getPool(), getPseudoWireId().toString());
                    setResponsePage(new PseudoWireEditPage(getBackPage(), publishedPw));
                }
            };
            form.add(proceedButton);
            TextField<Integer> idField = new TextField<Integer>("pseudoWireId", new PropertyModel<Integer>(this, "pseudoWireId"));
            form.add(idField);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public void processCreate() {
        String vcId = getPseudoWireId();
        log.debug("selected: " + vcId);
        if (vcId == null) {
            return;
        }
        try {
            PseudowireStringIdPoolDto pool = getPool();
            PseudoWireStringTypeCommandBuilder builder = new PseudoWireStringTypeCommandBuilder(pool, editorName);
            builder.setPseudoWireID(getPseudoWireId());
            builder.buildCommand();
            ShellConnector.getInstance().execute(builder);
            pool.renew();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public PseudowireStringIdPoolDto getPool() {
        return this.pool;
    }

    public String getPseudoWireId() {
        if (this.selectedType == IdSelectionType.TEXT) {
            return this.pwId;
        }
        throw new IllegalArgumentException("Please select a radio button.");
    }

    public void setPseudoWireId(String id) {
        this.pwId = id;
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public IdSelectionType getSelectedType() {
        return this.selectedType;
    }

    public void setSelectedType(IdSelectionType type_) {
        this.selectedType = type_;
    }

    private enum IdSelectionType {
        TEXT,
        AUTO,;
    }

}