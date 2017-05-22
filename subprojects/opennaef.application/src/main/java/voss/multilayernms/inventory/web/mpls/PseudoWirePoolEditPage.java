package voss.multilayernms.inventory.web.mpls;

import naef.dto.mpls.PseudowireLongIdPoolDto;
import naef.dto.mpls.PseudowireStringIdPoolDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.web.util.IdRangeValidator;
import voss.nms.inventory.builder.SimplePseudoWireBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.PageUtil;
import voss.nms.inventory.util.PseudoWireUtil;

import java.util.HashMap;
import java.util.Map;

public class PseudoWirePoolEditPage extends WebPage {
    public static final String OPERATION_NAME = "PseudoWirePoolEdit";
    private final PseudowireStringIdPoolDto pool;
    private String domain = "Nationwide";
    private String range;
    private String name;
    private final Map<String, String> attributes = new HashMap<String, String>();
    private final String editorName;
    private final WebPage backPage;

    public PseudoWirePoolEditPage(WebPage backPage, PseudowireStringIdPoolDto pool) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.pool = pool;
            this.backPage = backPage;
            if (pool != null) {
                range = pool.getConcatenatedIdRangesStr();
                name = pool.getName();
                DtoUtil.putValues(attributes, MPLSNMS_ATTR.PURPOSE, pool);
                DtoUtil.putValues(attributes, MPLSNMS_ATTR.NOTE, pool);
                DtoUtil.putValuesWithDefault(attributes, MPLSNMS_ATTR.OPER_STATUS, pool, "In Use");
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

            TextField<String> rangeField = new TextField<String>("range", new PropertyModel<String>(this, "range"));
            rangeField.setRequired(true);
            rangeField.add(new IdRangeValidator());
            form.add(rangeField);
            TextField<String> nameField = new TextField<String>("name", new PropertyModel<String>(this, "name"));
            nameField.setRequired(true);
            form.add(nameField);
            TextField<String> purposeField = new TextField<String>("purpose", new PropertyModel<String>(attributes, MPLSNMS_ATTR.PURPOSE));
            form.add(purposeField);
            TextArea<String> noteArea = new TextArea<String>("noteArea", new PropertyModel<String>(attributes, MPLSNMS_ATTR.NOTE));
            form.add(noteArea);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void processUpdate() {
        try {
            PseudowireStringIdPoolDto target = this.pool;
            String range = Util.formatRange(getRange());
            ShellCommands commands = new ShellCommands(editorName);
            if (target == null) {
                PseudowireLongIdPoolDto existingPool = PseudoWireUtil.getPool(name);
                if (existingPool != null) {
                    throw new IllegalStateException("There is the pool with same name already. Please select another name: [" + name + "]");
                }
                SimplePseudoWireBuilder.buildPseudoWireStringTypeIdPoolCreationCommands(commands, name, domain, range, attributes);
            } else {
                commands.addVersionCheckTarget(target);
                InventoryBuilder.changeContext(commands, target);
                InventoryBuilder.buildAttributeUpdateCommand(commands, target, attributes);
                String range_old = Util.formatRange(this.pool.getConcatenatedIdRangesStr());
                if (!range_old.equals(range)) {
                    throw new IllegalStateException("Changing range is not supported.");
                }
                String currentPoolName = pool.getName();
                if (!currentPoolName.equals(name)) {
                    PseudowireLongIdPoolDto existingPool = PseudoWireUtil.getPool(name);
                    if (existingPool != null) {
                        throw new IllegalStateException("There is the pool with same name already. Please select another name: [" + name + "]");
                    }
                    InventoryBuilder.buildRenameCommands(commands, name);
                }
            }
            ShellConnector.getInstance().execute2(commands);
            if (target != null) {
                target.renew();
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public PseudowireStringIdPoolDto getPool() {
        return this.pool;
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
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
}