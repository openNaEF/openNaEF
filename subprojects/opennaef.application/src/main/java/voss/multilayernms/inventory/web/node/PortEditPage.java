package voss.multilayernms.inventory.web.node;

import naef.dto.*;
import naef.dto.atm.AtmPortDto;
import naef.dto.eth.EthPortDto;
import naef.dto.pos.PosPortDto;
import naef.dto.serial.SerialPortDto;
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
import voss.multilayernms.inventory.web.util.IpAddressValidator;
import voss.nms.inventory.builder.PhysicalPortCommandBuilder;
import voss.nms.inventory.constants.PortMode;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.constants.SwitchPortMode;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.PageUtil;

import java.util.*;

public class PortEditPage extends WebPage {
    public static final String OPERATION_NAME = "PortEdit";
    private final Form<Void> form;
    private final NodeDto node;
    private final HardPortDto port;

    private String defaultIfName = null;
    private String ifName = null;
    private String portName;
    private PortType ifType;
    private String vpnPrefix;
    private String ipAddress;
    private String maskLength;
    private String portMode;
    private String switchPortMode;
    private String purpose;
    private String resourcePermission;
    private Long bandwidth;
    private String operStatus;
    private FacilityStatus facilityStatus;
    private String note;
    private final String editorName;
    private final Set<String> supportdAttributes = new HashSet<String>();
    private final WebPage backPage;

    public PortEditPage(WebPage backPage, final HardPortDto port) {
        if (port == null) {
            throw new IllegalArgumentException();
        }
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            this.port = port;
            this.node = port.getNode();
            this.portName = port.getOwner().getName();
            this.ifName = PortRenderer.getIfName(port);
            this.defaultIfName = NameUtil.getDefaultIfName(port);
            if (this.ifName == null) {
                this.ifName = this.defaultIfName;
            }
            this.ifType = PortType.getByType(port.getObjectTypeName());
            this.vpnPrefix = PortRenderer.getVpnPrefix(port);
            this.ipAddress = PortRenderer.getIpAddress(port);
            this.maskLength = PortRenderer.getSubnetMask(port);
            this.portMode = PortRenderer.getPortMode(port);
            this.switchPortMode = PortRenderer.getSwitchPortMode(port);
            this.bandwidth = PortRenderer.getBandwidthAsLong(port);
            this.operStatus = PortRenderer.getOperStatus(port);
            this.purpose = PortRenderer.getPurpose(port);
            this.resourcePermission = PortRenderer.getResourcePermission(port);
            this.note = PortRenderer.getNote(port);
            this.facilityStatus = PortRenderer.getFacilityStatusValue(port);
            log().debug("ifType=" + ifType.getCaption());
            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            Label nodeNameLabel = new Label("nodeName", Model.of(this.node.getName()));
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

            supportdAttributes.clear();
            supportdAttributes.addAll(DtoUtil.getSupportedAttributeNames(port));
            LoggerFactory.getLogger(PortEditPage.class).debug("supported attr:" + supportdAttributes);

            String portName = port.getName();
            if (portName == null || portName.length() == 0) {
                portName = port.getOwner().getName();
            }
            Label portNumber = new Label("portNumber", portName);
            this.form.add(portNumber);

            TextField<String> ifNameField = new TextField<String>("ifName", new PropertyModel<String>(this, "ifName"));
            this.form.add(ifNameField);
            ifNameField.setEnabled(true);
            Label defaultIfNameLabel = new Label("defaultIfName", Model.of(this.defaultIfName));
            this.form.add(defaultIfNameLabel);

            List<PortType> portTypes;
            if (this.ifType != null) {
                portTypes = Arrays.asList(PortType.values());
            } else {
                portTypes = PortType.getUserSelectableTypes();
            }
            DropDownChoice<PortType> ifTypeField = new DropDownChoice<PortType>("ifType",
                    new PropertyModel<PortType>(this, "ifType"),
                    portTypes,
                    new ChoiceRenderer<PortType>("caption"));
            ifTypeField.setEnabled(this.ifType == null);
            ifTypeField.setRequired(true);
            this.form.add(ifTypeField);

            addTextField(port, "vpnPrefix", "vpnPrefix", MPLSNMS_ATTR.VPN_PREFIX);
            addTextField(port, "ipAddress", "ipAddress", MPLSNMS_ATTR.IP_ADDRESS, new IpAddressValidator());
            addTextField(port, "maskLength", "maskLength", MPLSNMS_ATTR.MASK_LENGTH);
            addTextField(port, "bandwidth", "bandwidth", MPLSNMS_ATTR.BANDWIDTH);
            addTextField(port, "purpose", "purpose", MPLSNMS_ATTR.PURPOSE);
            addTextField(port, "resourcePermission", "resourcePermission", "resource_permission");
            addTextField(port, "note", "note", MPLSNMS_ATTR.NOTE);

            DropDownChoice<String> operStatusSelection = new DropDownChoice<String>("operStatus",
                    new PropertyModel<String>(this, "operStatus"),
                    conn.getStatusList());
            this.form.add(operStatusSelection);
            DropDownChoice<FacilityStatus> facilityStatusList = new DropDownChoice<FacilityStatus>("facilityStatus",
                    new PropertyModel<FacilityStatus>(this, "facilityStatus"),
                    Arrays.asList(FacilityStatus.values()),
                    new ChoiceRenderer<FacilityStatus>("displayString"));
            this.form.add(facilityStatusList);
            DropDownChoice<String> portModeSelection = new DropDownChoice<String>("portMode",
                    new PropertyModel<String>(this, "portMode"),
                    toList(PortMode.values()));
            this.form.add(portModeSelection);
            DropDownChoice<String> switchPortModeSelection = new DropDownChoice<String>("switchPortMode",
                    new PropertyModel<String>(this, "switchPortMode"),
                    toList(SwitchPortMode.values()));
            this.form.add(switchPortModeSelection);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private List<String> toList(Enum<?>[] values) {
        List<String> result = new ArrayList<String>();
        if (values != null) {
            for (Enum<?> value : values) {
                result.add(value.name());
            }
        }
        return result;
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
            List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();

            HardPortDto port = getPort();
            LoggerFactory.getLogger(PortEditPage.class).debug("port=" + port);
            NodeElementDto owner = port.getOwner();
            if (owner instanceof JackDto) {
                owner = owner.getOwner();
            }
            PhysicalPortCommandBuilder builder = new PhysicalPortCommandBuilder(owner, port, editorName);
            switch (getIfType()) {
                case ATM:
                    builder.setConstraint(AtmPortDto.class);
                    break;
                case POS:
                    builder.setConstraint(PosPortDto.class);
                    break;
                case SERIAL:
                    builder.setConstraint(SerialPortDto.class);
                    break;
                case ETHERNET:
                    builder.setConstraint(EthPortDto.class);
                    break;
                default:
                    throw new IllegalArgumentException("not supported-type: " + getIfType().getCaption());
            }
            String ifName = getIfName();
            if (ifName == null) {
                ifName = this.defaultIfName;
            }
            if (this.portMode == null) {
                builder.setSwitchPortMode(null);
                builder.setPortMode(null);
            } else {
                builder.setPortMode(this.portMode);
                builder.setSwitchPortMode(this.switchPortMode);
            }
            builder.setIfName(ifName);
            builder.setPortType(getIfType());
            builder.setBandwidth(bandwidth);
            builder.setNewIpAddress(this.vpnPrefix, ipAddress, maskLength);
            builder.setNote(note);
            builder.setOperStatus(operStatus);
            builder.setPortName(portName);
            builder.setPurpose(purpose);
            builder.setValue("resource_permission", resourcePermission);

            if (facilityStatus != null) {
                builder.setValue(MPLSNMS_ATTR.FACILITY_STATUS, facilityStatus.getDisplayString());
            } else {
                builder.setValue(MPLSNMS_ATTR.FACILITY_STATUS, (String) null);
            }
            builder.buildCommand();
            commandBuilderList.add(builder);
            ShellConnector.getInstance().executes(commandBuilderList);
            if (port != null) {
                port.renew();
            }
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

    public String getDefaultIfName() {
        return this.defaultIfName;
    }

    public HardPortDto getPort() {
        return this.port;
    }

    public PortType getIfType() {
        return this.ifType;
    }

    public void setIfType(PortType type) {
        this.ifType = type;
    }

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
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

    public void setMaskLength(String subnetMask) {
        this.maskLength = subnetMask;
    }

    public String getPortMode() {
        return portMode;
    }

    public void setPortMode(String portMode) {
        this.portMode = portMode;
    }

    public String getSwitchPortMode() {
        return switchPortMode;
    }

    public void setSwitchPortMode(String switchPortMode) {
        this.switchPortMode = switchPortMode;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getResourcePermission() {
        return resourcePermission;
    }

    public void setResourcePermission(String resourcePermission) {
        this.resourcePermission = resourcePermission;
    }

    public Long getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(Long bandwidth) {
        this.bandwidth = bandwidth;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Logger log() {
        return LoggerFactory.getLogger(PortEditPage.class);
    }
}