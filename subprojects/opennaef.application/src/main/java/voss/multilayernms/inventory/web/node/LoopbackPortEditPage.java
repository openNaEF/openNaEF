package voss.multilayernms.inventory.web.node;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.CONSTANTS;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.util.OspfAreaIdValidator;
import voss.multilayernms.inventory.web.parts.CommandExecutionConfirmationPage;
import voss.multilayernms.inventory.web.util.IpAddressValidator;
import voss.nms.inventory.builder.LoopbackPortCommandBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.PageUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoopbackPortEditPage extends WebPage {
    public static final String OPERATION_NAME = "LoopbackPortEdit";
    private final Form<Void> form;

    private final NodeDto node;
    private final IpIfDto loopback;
    private String ifName;
    private String vpnPrefix;
    private String ipAddress;
    private String maskLength;
    private String ospfAreaID;
    private Integer igpCost;
    private FacilityStatus facilityStatus;
    private String purpose;
    private String note;
    private boolean independentIp = false;
    private final String editorName;
    private final WebPage backPage;

    public LoopbackPortEditPage(final WebPage backPage, final NodeDto node, final IpIfDto loopback) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            this.node = node;
            if (loopback != null) {
                loopback.renew();
                this.loopback = loopback;
                this.ifName = PortRenderer.getIfName(loopback);
                if (this.ifName == null) {
                    this.ifName = loopback.getName();
                }
                this.independentIp = DtoUtil.hasStringValue(loopback, MPLSNMS_ATTR.PORT_TYPE, CONSTANTS.INTERFACE_TYPE_INDEPENDENT_IP);
                this.vpnPrefix = PortRenderer.getVpnPrefix(loopback);
                this.ipAddress = PortRenderer.getIpAddress(loopback);
                this.maskLength = PortRenderer.getSubnetMask(loopback);
                this.ospfAreaID = PortRenderer.getOspfAreaID(loopback);
                this.igpCost = PortRenderer.getIgpCostAsInteger(loopback);
                this.facilityStatus = PortRenderer.getFacilityStatusValue(loopback);
                this.purpose = PortRenderer.getPurpose(loopback);
                this.note = PortRenderer.getNote(loopback);
            } else {
                this.loopback = null;
            }
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

            Form<Void> deleteForm = new Form<Void>("deleteForm");
            add(deleteForm);
            Button deleteButton = new Button("delete") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    IpIfDto port = getLoopback();
                    try {
                        NodeUtil.checkDeletable(port);
                        LoopbackPortCommandBuilder builder = new LoopbackPortCommandBuilder(port, editorName);
                        builder.buildDeleteCommand();
                        List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
                        builders.add(builder);
                        String msg = "loopback interface: [" + DtoUtil.getIfName(port) + "] will be deleted.";
                        CommandExecutionConfirmationPage page = new CommandExecutionConfirmationPage(getBackPage(), msg, builders);
                        setResponsePage(page);
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            deleteButton.setEnabled(this.loopback != null);
            deleteButton.setVisible(this.loopback != null);
            deleteForm.add(deleteButton);

            add(new FeedbackPanel("feedback"));

            this.form = new Form<Void>("editInterface");
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
            ifNameField.setRequired(true);
            this.form.add(ifNameField);
            addTextField(loopback, "vpnPrefix", "vpnPrefix");
            addTextField(loopback, "ipAddress", "ipAddress", new IpAddressValidator());
            addTextField(loopback, "maskLength", "maskLength");
            addTextField(loopback, "ospfAreaID", "ospfAreaID");
            addTextField(loopback, "igpCost", "igpCost");
            DropDownChoice<FacilityStatus> facilityStatusList = new DropDownChoice<FacilityStatus>("facilityStatus",
                    new PropertyModel<FacilityStatus>(this, "facilityStatus"),
                    Arrays.asList(FacilityStatus.values()),
                    new ChoiceRenderer<FacilityStatus>("displayString"));
            this.form.add(facilityStatusList);
            addTextField(loopback, "purpose", "purpose");
            TextArea<String> noteField = new TextArea<String>("note", new PropertyModel<String>(this, "note"));
            this.form.add(noteField);

        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void processUpdate() {
        if (ifName == null) {
            return;
        }
        try {
            PortDto port = NodeUtil.getPortByIfName(node, getIfName());
            LoggerFactory.getLogger(LoopbackPortEditPage.class).debug("port=" + port);
            OspfAreaIdValidator.validate(port, ospfAreaID);
            LoopbackPortCommandBuilder builder;
            if (port == null) {
                builder = new LoopbackPortCommandBuilder(getNode(), editorName);
                builder.setSource(DiffCategory.INVENTORY.name());
            } else {
                builder = new LoopbackPortCommandBuilder(getLoopback(), editorName);
            }
            builder.setIfName(getIfName());
            builder.setNewIpAddress(this.vpnPrefix, this.ipAddress, this.maskLength);
            builder.setOspfAreaID(ospfAreaID);
            builder.setIgpCost(igpCost);
            if (facilityStatus != null) {
                builder.setValue(MPLSNMS_ATTR.FACILITY_STATUS, facilityStatus.getDisplayString());
            } else {
                builder.setValue(MPLSNMS_ATTR.FACILITY_STATUS, (String) null);
            }
            builder.setPurpose(this.purpose);
            builder.setNote(note);
            builder.buildCommand();
            ShellConnector.getInstance().execute(builder);
            if (port != null) {
                port.renew();
            }
            node.renew();
            setResponsePage(SimpleNodeDetailPage.class, NodeUtil.getParameters(node));
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private TextField<String> addTextField(PortDto port, String id, String attributeName) {
        return addTextField(port, id, attributeName, null);
    }

    private TextField<String> addTextField(PortDto port, String id, String attributeName, AbstractValidator<String> validator) {
        TextField<String> tf = new TextField<String>(id, new PropertyModel<String>(this, attributeName));
        form.add(tf);
        if (validator != null) {
            tf.add(validator);
        }
        return tf;
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public IpIfDto getLoopback() {
        return this.loopback;
    }

    public String getIfName() {
        return ifName;
    }

    public void setIfName(String ifName) {
        this.ifName = ifName;
    }

    public boolean isIndependentIp() {
        return this.independentIp;
    }

    public void setIndependentIp(boolean value) {
        this.independentIp = value;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMaskLength() {
        return maskLength;
    }

    public void setMaskLength(String maskLength) {
        this.maskLength = maskLength;
    }

    public String getOspfAreaID() {
        return ospfAreaID;
    }

    public void setOspfAreaID(String ospfAreaID) {
        this.ospfAreaID = ospfAreaID;
    }

    public Integer getIgpCost() {
        return igpCost;
    }

    public void setIgpCost(Integer igpCost) {
        this.igpCost = igpCost;
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

    public NodeDto getNode() {
        return this.node;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}