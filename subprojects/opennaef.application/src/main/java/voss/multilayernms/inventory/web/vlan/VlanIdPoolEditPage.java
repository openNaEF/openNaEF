package voss.multilayernms.inventory.web.vlan;

import naef.dto.vlan.VlanIdPoolDto;
import opennaef.builder.VlanIdPoolCommandBuilder;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.web.util.IdRangeValidator;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.PageUtil;

import java.util.HashMap;
import java.util.Map;

public class VlanIdPoolEditPage extends WebPage {
    public static final String OPERATION_NAME = "VlanIdPoolEdit";
    private final VlanIdPoolDto pool;
    private String range;
    private String name;
    private String purpose;
    private String note;
    private final String editorName;
    private final WebPage backPage;
    private static final Logger log =  LoggerFactory.getLogger(VlanIdPoolEditPage.class);

    public VlanIdPoolEditPage(WebPage backPage, VlanIdPoolDto pool) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.pool = pool;
            this.backPage = backPage;
            if (pool != null) {
                range = pool.getConcatenatedIdRangesStr();
                name = pool.getName();
                this.purpose = DtoUtil.getString(pool,ATTR.PURPOSE);
                this.note = DtoUtil.getString(pool,ATTR.NOTE);
            }

            add(new FeedbackPanel("feedback"));

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

            Form<Void> form = new Form<Void>("poolEditForm");
            add(form);

            Button processButton = new Button("process") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    processUpdate();
                    PageUtil.setModelChanged(getBackPage());
                    setResponsePage(getBackPage());
                }
            };
            form.add(processButton);

            Button deleteButton = new Button("delete") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    delete();
                    PageUtil.setModelChanged(getBackPage());
                    setResponsePage(getBackPage());
                }
            };
            form.add(deleteButton);

            TextField<String> rangeField = new TextField<String>("range", new PropertyModel<String>(this, "range"));
            rangeField.setRequired(true);
            rangeField.add(new IdRangeValidator());
            rangeField.setEnabled(pool == null);
            form.add(rangeField);
            TextField<String> nameField = new TextField<String>("name", new PropertyModel<String>(this, MPLSNMS_ATTR.NAME));
            nameField.setRequired(true);
            form.add(nameField);
            TextField<String> purposeField = new TextField<String>("purpose", new PropertyModel<String>(this, "purpose"));
            form.add(purposeField);
            TextArea<String> noteArea = new TextArea<String>("note", new PropertyModel<String>(this, "note"));
            form.add(noteArea);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void processUpdate() {
        try {
            VlanIdPoolDto target = this.pool;
            String range = Util.formatRange(getRange());
            if (target == null) {
                log.info("new vlan pool:" + getName() + " range:" + range);
                VlanIdPoolCommandBuilder builder = new VlanIdPoolCommandBuilder(getName(), range, editorName);
                builder.setPurpose(getPurpose());
                builder.setNote(getNote());
                builder.buildCommand();
                ShellConnector.getInstance().execute(builder);
            } else {
                log.info("update vlan pool:" + getName() + " range:" + range);
                VlanIdPoolCommandBuilder builder = new VlanIdPoolCommandBuilder(target, editorName);
                builder.setPurpose(getPurpose());
                builder.setNote(getNote());
                builder.buildCommand();
                ShellConnector.getInstance().execute(builder);
            }
            if (target != null) {
                target.renew();
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void delete() {
        try {
            VlanIdPoolDto target = this.pool;
            if (target == null) {
                throw new IllegalArgumentException("No VLAN Pool is indicated.");
            } else {
                log.info("delete vlan pool:" + getName());
                VlanIdPoolCommandBuilder builder = new VlanIdPoolCommandBuilder(target, editorName);

                builder.buildDeleteCommand();
                ShellConnector.getInstance().execute(builder);
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public VlanIdPoolDto getPool() {
        return this.pool;
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

}