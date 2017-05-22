package voss.multilayernms.inventory.web.node;

import naef.dto.SlotDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.builder.ModuleCommandBuilder;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.NodeUtil;

public class ModuleRemovePage extends WebPage {
    public static final String OPERATION_NAME = "ModuleRemove";
    private final String editorName;

    public ModuleRemovePage(final WebPage backPage, final SlotDto slot) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            if (slot == null) {
                throw new IllegalStateException("slot is null.");
            }
            Label confirmLabel = new Label("slotId", Model.of(NameUtil.getCaption(slot)));
            add(confirmLabel);
            Label moduleLabel = new Label("moduleName", Model.of(slot.getModule().getName()));
            add(moduleLabel);

            Form<Void> form = new Form<Void>("removeConfirmationForm");
            add(form);

            Button proceed = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        try {
                            NodeUtil.checkDeletable(slot);
                        } catch (InventoryException e) {
                            throw ExceptionUtils.throwAsRuntime(e);
                        }
                        ModuleCommandBuilder builder = new ModuleCommandBuilder(slot.getModule(), editorName);
                        builder.setCascadeDelete(true);
                        builder.buildDeleteCommand();
                        ShellConnector.getInstance().execute(builder);
                        slot.getNode().renew();
                        setResponsePage(SimpleNodeDetailPage.class, NodeUtil.getNodeParameters(slot));
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            form.add(proceed);

            Button back = new Button("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(backPage);
                }
            };
            form.add(back);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    @SuppressWarnings("unused")
    private Logger log() {
        return LoggerFactory.getLogger(ModuleRemovePage.class);
    }

}