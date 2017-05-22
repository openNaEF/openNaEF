package voss.multilayernms.inventory.web.mpls;

import naef.dto.NodeDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspIdPoolDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.nms.inventory.builder.RsvpLspCommandBuilder;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.RsvpLspUtil;

public class RsvpLspCreationPage extends WebPage {
    public static final String OPERATION_NAME = "RsvpLspCreation";
    private static final Logger log = LoggerFactory.getLogger(RsvpLspCreationPage.class);
    private final RsvpLspIdPoolDto pool;
    private final String editorName;
    private final WebPage backPage;

    private String name;
    private NodeDto ingress;

    public RsvpLspCreationPage(WebPage backPage, RsvpLspIdPoolDto pool) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            if (pool == null) {
                throw new IllegalArgumentException("pool is null.");
            }
            this.pool = pool;
            this.backPage = backPage;

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
                    RsvpLspDto published = RsvpLspUtil.getRsvpLsp(getPool(), getLspName());
                    setResponsePage(new RsvpLspEditPage(getBackPage(), published, getIngress()));
                }
            };
            form.add(proceedButton);

            TextField<Integer> idField = new TextField<Integer>("lspName", new PropertyModel<Integer>(this, "name"));
            idField.setRequired(true);
            form.add(idField);

            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            DropDownChoice<NodeDto> ingressList = new DropDownChoice<NodeDto>(
                    "ingressNodes",
                    new PropertyModel<NodeDto>(this, "ingress"),
                    conn.getActiveNodes(),
                    new ChoiceRenderer<NodeDto>("name"));
            ingressList.setRequired(true);
            form.add(ingressList);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public void processCreate() {
        String name = getName();
        log.debug("selected: " + name);
        if (name == null) {
            throw new IllegalArgumentException("LSP Name is not set.");
        }
        try {
            RsvpLspCommandBuilder builder = new RsvpLspCommandBuilder(getPool(), getName(), getIngress(), editorName);
            builder.buildCommand();
            ShellConnector.getInstance().execute(builder);
            pool.renew();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public String getName() {
        return name;
    }

    public void setName(String lspName) {
        this.name = lspName;
    }

    public NodeDto getIngress() {
        return ingress;
    }

    public void setIngress(NodeDto ingress) {
        this.ingress = ingress;
    }

    public String getLspName() {
        if (this.ingress == null) {
            throw new IllegalStateException("ingress node not selected.");
        }
        return this.ingress.getName() + ":" + this.name;
    }

    public RsvpLspIdPoolDto getPool() {
        return pool;
    }

}