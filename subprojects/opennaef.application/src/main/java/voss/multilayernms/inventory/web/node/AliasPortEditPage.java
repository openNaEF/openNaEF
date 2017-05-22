package voss.multilayernms.inventory.web.node;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.eth.EthPortDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ATTR;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.nms.inventory.builder.AliasPortCommandBuilder;
import voss.nms.inventory.builder.AttributeUpdateCommandBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.Comparators;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.PageUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AliasPortEditPage extends WebPage {
    public static final String OPERATION_NAME = "AliasPortEdit";
    private final Form<Void> form;

    private final NodeDto virtualNode;
    private PortDto aliasSource = null;
    private PortDto alias = null;
    private String ifName = null;
    private String operStatus;
    private FacilityStatus facilityStatus;
    private String purpose;
    private String note;
    private final String editorName;
    private final WebPage backPage;

    public AliasPortEditPage(WebPage backPage, final NodeDto virtualNode, final PortDto port) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            this.virtualNode = virtualNode;
            this.alias = port;
            if (this.alias != null) {
                if (!DtoUtil.mvoEquals(this.alias.getNode(), this.virtualNode)) {
                    throw new IllegalStateException("alias/virtual-node mismatch: alias=" + DtoUtil.toDebugString(this.alias));
                }
            }
            log().debug("virtualNode=" + DtoUtil.toDebugString(this.virtualNode));
            log().debug("alias=" + DtoUtil.toDebugString(this.alias));
            if (port != null) {
                this.aliasSource = port.getAliasSource();
                this.ifName = PortRenderer.getIfName(port);
                this.operStatus = PortRenderer.getOperStatus(port);
                this.note = PortRenderer.getNote(port);
                this.facilityStatus = PortRenderer.getFacilityStatusValue(port);
                log().debug("aliasSource=" + DtoUtil.toDebugString(this.aliasSource));
            } else {
                this.operStatus = MPLSNMS_ATTR.UP;
                log().debug("no aliasSource");
            }

            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();

            Label nodeNameLabel = new Label("nodeName", Model.of(this.virtualNode.getName()));
            add(nodeNameLabel);

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
            Button deleteButton = new Button("delete") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    processDelete();
                    setResponsePage(getBackPage());
                }
            };
            backForm.add(deleteButton);

            add(new FeedbackPanel("feedback"));
            this.form = new Form<Void>("form");
            add(this.form);
            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    processUpdate();
                    PageUtil.setModelChanged(getBackPage());
                    setResponsePage(getBackPage());
                }
            };
            form.add(proceedButton);

            TextField<String> ifNameField = new TextField<String>("ifName", new PropertyModel<String>(this, "ifName"));
            ifNameField.setRequired(this.alias != null);
            this.form.add(ifNameField);
            Label defaultIfName = new Label("defaultIfName", Model.of(NameUtil.getDefaultIfName(this.alias)));
            this.form.add(defaultIfName);

            List<PortDto> aliasSources = new ArrayList<PortDto>();
            if (this.aliasSource == null) {
                NodeDto host = this.virtualNode.getVirtualizationHostNode();
                if (host == null) {
                    throw new IllegalStateException("The node is not a virtual node.");
                }
                List<PortDto> ports = new ArrayList<PortDto>(host.getPorts());
                Collections.sort(ports, Comparators.getIfNameBasedPortComparator());
                aliasSources.addAll(ports);
            } else {
                aliasSources.add(this.aliasSource);
            }
            DropDownChoice<PortDto> aliasSourceField = new DropDownChoice<PortDto>("aliasSource",
                    new PropertyModel<PortDto>(this, "aliasSource"),
                    aliasSources,
                    new PortChoiceRenderer());
            aliasSourceField.setEnabled(this.aliasSource == null);
            aliasSourceField.setRequired(true);
            this.form.add(aliasSourceField);

            addTextField(port, "purpose", "purpose", MPLSNMS_ATTR.PURPOSE);
            TextArea<String> noteField = new TextArea<String>("note", new PropertyModel<String>(this, "note"));
            this.form.add(noteField);

            DropDownChoice<String> operStatusSelection = new DropDownChoice<String>("operStatus",
                    new PropertyModel<String>(this, "operStatus"),
                    conn.getStatusList());
            this.form.add(operStatusSelection);
            DropDownChoice<FacilityStatus> facilityStatusList = new DropDownChoice<FacilityStatus>("facilityStatus",
                    new PropertyModel<FacilityStatus>(this, "facilityStatus"),
                    Arrays.asList(FacilityStatus.values()),
                    new ChoiceRenderer<FacilityStatus>("displayString"));
            this.form.add(facilityStatusList);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private TextField<String> addTextField(PortDto port, String id, String varName, String attributeName) {
        return addTextField(port, id, varName, attributeName, null);
    }

    private TextField<String> addTextField(PortDto port, String id, String varName,
                                           String attributeName, AbstractValidator<String> validator) {
        TextField<String> tf = new TextField<String>(id, new PropertyModel<String>(this, varName));
        form.add(tf);
        if (validator != null) {
            tf.add(validator);
        }
        return tf;
    }

    public void processUpdate() {
        try {
            if (ifName == null) {
                ifName = DtoUtil.getIfName(this.aliasSource);
            }
            List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
            log().debug("alias=" + DtoUtil.toDebugString(this.alias));
            log().debug("aliasSource=" + DtoUtil.toDebugString(this.aliasSource));
            AttributeUpdateCommandBuilder builder1 = new AttributeUpdateCommandBuilder(aliasSource, editorName);
            builder1.setValue(ATTR.ATTR_ALIAS_SOURCEABLE, Boolean.TRUE);
            builder1.buildCommand();
            commandBuilderList.add(builder1);
            AliasPortCommandBuilder builder2;
            if (this.alias != null) {
                builder2 = new AliasPortCommandBuilder(this.alias, editorName);
            } else {
                builder2 = new AliasPortCommandBuilder(this.virtualNode, editorName);
                builder2.setAliasSourcePort(this.aliasSource);
            }
            builder2.setConstraint(EthPortDto.class);
            builder2.setIfName(ifName);
            builder2.setPurpose(this.purpose);
            builder2.setNote(note);
            builder2.setOperStatus(operStatus);
            if (facilityStatus != null) {
                builder2.setValue(MPLSNMS_ATTR.FACILITY_STATUS, facilityStatus.getDisplayString());
            } else {
                builder2.setValue(MPLSNMS_ATTR.FACILITY_STATUS, (String) null);
            }
            builder2.buildCommand();
            commandBuilderList.add(builder2);
            ShellConnector.getInstance().executes(commandBuilderList);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void processDelete() {
        if (this.alias == null) {
            return;
        }
        try {
            AliasPortCommandBuilder builder = new AliasPortCommandBuilder(this.alias, editorName);
            builder.buildDeleteCommand();
            ShellConnector.getInstance().execute(builder);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public String getIfName() {
        return this.ifName;
    }

    public void setIfName(String name) {
        this.ifName = name;
    }

    public String getOperStatus() {
        return operStatus;
    }

    public void setOperStatus(String operStatus) {
        this.operStatus = operStatus;
    }

    public FacilityStatus getFacilityStatus() {
        return facilityStatus;
    }

    public void setFacilityStatus(FacilityStatus facilityStatus) {
        this.facilityStatus = facilityStatus;
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

    public Logger log() {
        return LoggerFactory.getLogger(AliasPortEditPage.class);
    }

    private static class PortChoiceRenderer extends ChoiceRenderer<PortDto> {
        private static final long serialVersionUID = 1L;

        @Override
        public Object getDisplayValue(PortDto port) {
            return NameUtil.getNodeIfName(port);
        }
    }
}