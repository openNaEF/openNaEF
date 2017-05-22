package voss.multilayernms.inventory.web.node;

import naef.dto.JackDto;
import naef.dto.NodeDto;
import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.pos.PosApsIfDto;
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
import voss.core.server.database.ShellConnector;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.util.OspfAreaIdValidator;
import voss.multilayernms.inventory.web.parts.OperationStatusChoiceRenderer;
import voss.multilayernms.inventory.web.util.IpAddressValidator;
import voss.nms.inventory.builder.AtmAPSCommandBuilder;
import voss.nms.inventory.builder.EthernetLAGCommandBuilder;
import voss.nms.inventory.builder.POSAPSCommandBuilder;
import voss.nms.inventory.constants.PortMode;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.constants.SwitchPortMode;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.PageUtil;

import java.util.*;

public class LogicalPortEditPage extends WebPage {
    public static final String OPERATION_NAME = "PortEdit";
    private final Form<Void> form;

    private final NodeDto node;
    private PortDto port;
    private String defaultIfName = null;
    private String ifName = null;
    private PortType ifType;
    private String vpnPrefix;
    private String ipAddress;
    private String maskLength;
    private String purpose;
    private String ospfAreaID;
    private Integer igpCost;
    private String operStatus;
    private FacilityStatus facilityStatus;
    private PortMode portMode;
    private SwitchPortMode switchPortMode;
    private String note;
    private final String editorName;
    private final Set<String> supportdAttributes = new HashSet<String>();
    private final WebPage backPage;

    public LogicalPortEditPage(WebPage backPage, final NodeDto node, final PortDto port) {
        if (node == null) {
            throw new IllegalArgumentException("node is null.");
        }
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            this.node = node;
            if (port != null) {
                if (!isTarget(port)) {
                    throw new IllegalArgumentException("unexpected type: " + port.getClass().getSimpleName());
                }
                this.port = port;
                this.ifName = PortRenderer.getIfName(port);
                this.defaultIfName = NameUtil.getDefaultIfName(port);
                this.ifType = PortType.getByType(port.getObjectTypeName());
                this.vpnPrefix = PortRenderer.getVpnPrefix(port);
                this.ipAddress = PortRenderer.getIpAddress(port);
                this.maskLength = PortRenderer.getSubnetMask(port);
                this.purpose = PortRenderer.getPurpose(port);
                this.ospfAreaID = PortRenderer.getOspfAreaID(port);
                this.igpCost = PortRenderer.getIgpCostAsInteger(port);
                this.operStatus = PortRenderer.getOperStatus(port);
                this.facilityStatus = PortRenderer.getFacilityStatusValue(port);
                this.portMode = PortRenderer.getPortModeValue(port);
                this.switchPortMode = PortRenderer.getSwitchPortModeValue(port);
                this.note = PortRenderer.getNote(port);
                log().debug("ifType=" + ifType.getCaption());
            } else {
                this.operStatus = MPLSNMS_ATTR.UP;
            }

            Label nodeNameLabel = new Label("nodeName", Model.of(port.getNode().getName()));
            add(nodeNameLabel);

            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();

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
            this.form.add(proceedButton);

            supportdAttributes.clear();
            supportdAttributes.addAll(DtoUtil.getSupportedAttributeNames(port));
            LoggerFactory.getLogger(LogicalPortEditPage.class).debug("supported attr:" + supportdAttributes);

            TextField<String> ifNameField = new TextField<String>("ifName", new PropertyModel<String>(this, "ifName"));
            this.form.add(ifNameField);

            Label defaultIfNameLabel = new Label("defaultIfName", Model.of(this.defaultIfName));
            this.form.add(defaultIfNameLabel);

            List<PortType> portTypes = new ArrayList<PortType>();
            if (this.ifType != null) {
                portTypes.add(this.ifType);
            } else {
                portTypes.add(PortType.ATM_APS);
                portTypes.add(PortType.POS_APS);
                portTypes.add(PortType.LAG);
            }
            DropDownChoice<PortType> ifTypeField = new DropDownChoice<PortType>("ifType",
                    new PropertyModel<PortType>(this, "ifType"),
                    portTypes,
                    new ChoiceRenderer<PortType>("caption"));
            ifTypeField.setEnabled(this.ifType == null);
            ifTypeField.setRequired(true);
            this.form.add(ifTypeField);

            DropDownChoice<String> operStatusSelection = new DropDownChoice<String>("operStatus",
                    new PropertyModel<String>(this, "operStatus"),
                    conn.getStatusList(),
                    new OperationStatusChoiceRenderer());
            this.form.add(operStatusSelection);
            DropDownChoice<FacilityStatus> facilityStatusList = new DropDownChoice<FacilityStatus>("facilityStatus",
                    new PropertyModel<FacilityStatus>(this, "facilityStatus"),
                    Arrays.asList(FacilityStatus.values()),
                    new ChoiceRenderer<FacilityStatus>("displayString"));
            this.form.add(facilityStatusList);
            DropDownChoice<PortMode> portList = new DropDownChoice<PortMode>("portMode",
                    new PropertyModel<PortMode>(this, "portMode"),
                    Arrays.asList(PortMode.values()));
            this.form.add(portList);
            DropDownChoice<SwitchPortMode> swPortList = new DropDownChoice<SwitchPortMode>("switchPortMode",
                    new PropertyModel<SwitchPortMode>(this, "switchPortMode"),
                    Arrays.asList(SwitchPortMode.values()));
            this.form.add(swPortList);

            addTextField(port, "vpnPrefix", "vpnPrefix", MPLSNMS_ATTR.VPN_PREFIX);
            addTextField(port, "ipAddress", "ipAddress", MPLSNMS_ATTR.IP_ADDRESS, new IpAddressValidator());
            addTextField(port, "maskLength", "maskLength", MPLSNMS_ATTR.MASK_LENGTH);
            addTextField(port, "ospfAreaID", "ospfAreaID", MPLSNMS_ATTR.OSPF_AREA_ID);
            addTextField(port, "igpCost", "igpCost", MPLSNMS_ATTR.IGP_COST);
            addTextField(port, "purpose", "purpose", MPLSNMS_ATTR.PURPOSE);
            addTextField(port, "note", "note", MPLSNMS_ATTR.NOTE);

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
        tf.setEnabled(supportdAttributes.contains(attributeName));
        tf.setVisible(supportdAttributes.contains(attributeName));
        if (validator != null) {
            tf.add(validator);
        }
        return tf;
    }

    public void processUpdate() {
        if (defaultIfName == null) {
            return;
        }
        try {
            PortDto port = getPort();
            LoggerFactory.getLogger(LogicalPortEditPage.class).debug("port=" + port);
            OspfAreaIdValidator.validate(port, ospfAreaID);

            NodeElementDto owner = port.getOwner();
            if (owner instanceof JackDto) {
                owner = owner.getOwner();
            }
            CommandBuilder builder;
            switch (getIfType()) {
                case ATM_APS:
                    builder = processAtmAps((AtmApsIfDto) getPort());
                    break;
                case POS_APS:
                    builder = processPosAps((PosApsIfDto) getPort());
                    break;
                case LAG:
                    builder = processLag((EthLagIfDto) getPort());
                    break;
                default:
                    throw new IllegalArgumentException("not supported-type: " + getIfType().getCaption());
            }
            builder.buildCommand();
            ShellConnector.getInstance().execute(builder);
            if (port != null) {
                port.renew();
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private CommandBuilder processAtmAps(AtmApsIfDto port) {
        AtmAPSCommandBuilder builder;
        if (getPort() != null) {
            builder = new AtmAPSCommandBuilder(port, editorName);
        } else {
            builder = new AtmAPSCommandBuilder(getNode().getName(), getIfName(), editorName);
            builder.setSource(DiffCategory.INVENTORY.name());
        }
        builder.setIfName(getIfName());
        builder.setNewIpAddress(this.vpnPrefix, ipAddress, this.maskLength);
        builder.setOperStatus(operStatus);
        if (this.facilityStatus == null) {
            builder.setValue(MPLSNMS_ATTR.FACILITY_STATUS, (String) null);
        } else {
            builder.setValue(MPLSNMS_ATTR.FACILITY_STATUS, this.facilityStatus.getDisplayString());
        }
        builder.setOspfAreaID(ospfAreaID);
        builder.setIgpCost(igpCost);
        builder.setPurpose(purpose);
        builder.setNote(note);
        return builder;
    }

    private CommandBuilder processPosAps(PosApsIfDto port) {
        POSAPSCommandBuilder builder;
        if (getPort() != null) {
            builder = new POSAPSCommandBuilder(port, editorName);
        } else {
            builder = new POSAPSCommandBuilder(getNode().getName(), getIfName(), editorName);
            builder.setSource(DiffCategory.INVENTORY.name());
        }
        builder.setIfName(getIfName());
        builder.setNewIpAddress(this.vpnPrefix, ipAddress, this.maskLength);
        builder.setOperStatus(operStatus);
        if (this.facilityStatus == null) {
            builder.setValue(MPLSNMS_ATTR.FACILITY_STATUS, (String) null);
        } else {
            builder.setValue(MPLSNMS_ATTR.FACILITY_STATUS, this.facilityStatus.getDisplayString());
        }
        builder.setOspfAreaID(ospfAreaID);
        builder.setIgpCost(igpCost);
        builder.setPurpose(purpose);
        builder.setNote(note);
        return builder;
    }

    private CommandBuilder processLag(EthLagIfDto port) {
        EthernetLAGCommandBuilder builder;
        if (getPort() != null) {
            builder = new EthernetLAGCommandBuilder(port, editorName);
        } else {
            builder = new EthernetLAGCommandBuilder(getNode().getName(), getIfName(), editorName);
            builder.setSource(DiffCategory.INVENTORY.name());
        }
        builder.setConstraint(AtmPortDto.class);
        builder.setIfName(getIfName());
        builder.setNewIpAddress(this.vpnPrefix, ipAddress, this.maskLength);
        builder.setOperStatus(operStatus);
        if (this.facilityStatus == null) {
            builder.setValue(MPLSNMS_ATTR.FACILITY_STATUS, (String) null);
        } else {
            builder.setValue(MPLSNMS_ATTR.FACILITY_STATUS, this.facilityStatus.getDisplayString());
        }
        builder.setOspfAreaID(ospfAreaID);
        builder.setIgpCost(igpCost);
        builder.setPurpose(purpose);
        builder.setNote(note);
        return builder;
    }

    private boolean isTarget(PortDto port) {
        if (port instanceof EthLagIfDto) {
            return true;
        } else if (port instanceof AtmApsIfDto) {
            return true;
        } else if (port instanceof PosApsIfDto) {
            return true;
        }
        return false;
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

    public String getDefaultIfName() {
        return this.defaultIfName;
    }

    public NodeDto getNode() {
        return this.node;
    }

    public PortDto getPort() {
        return this.port;
    }

    public PortType getIfType() {
        return this.ifType;
    }

    public void setIfType(PortType type) {
        this.ifType = type;
    }

    public String getVpnPrefix() {
        return vpnPrefix;
    }

    public void setVpnPrefix(String vpnPrefix) {
        this.vpnPrefix = vpnPrefix;
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

    public PortMode getPortMode() {
        return portMode;
    }

    public void setPortMode(PortMode portMode) {
        this.portMode = portMode;
    }

    public SwitchPortMode getSwitchPortMode() {
        return switchPortMode;
    }

    public void setSwitchPortMode(SwitchPortMode switchPortMode) {
        this.switchPortMode = switchPortMode;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
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

    public String getOperStatus() {
        return operStatus;
    }

    public void setOperStatus(String operStatus) {
        this.operStatus = operStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Logger log() {
        return LoggerFactory.getLogger(LogicalPortEditPage.class);
    }
}