package voss.multilayernms.inventory.web.node;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.atm.AtmPvpIfDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.apache.wicket.validation.validator.RangeValidator;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.constants.PortFacilityStatus;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.util.OspfAreaIdValidator;
import voss.multilayernms.inventory.web.parts.CommandExecutionConfirmationPage;
import voss.multilayernms.inventory.web.parts.OperationStatusChoiceRenderer;
import voss.multilayernms.inventory.web.util.IpAddressValidator;
import voss.nms.inventory.builder.AtmPvcPortCommandBuilder;
import voss.nms.inventory.builder.AtmVpCommandBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AtmPvcEditPage extends WebPage {
    public static final String OPERATION_NAME = "AtmPvcEdit";
    private final Form<Void> form;

    private final WebPage backPage;
    private NodeDto node;
    private PortDto atmPort;
    private final AtmPvcIfDto port;
    private String ifName;
    private Integer vpi;
    private Integer vci;
    private String vpnPrefix;
    private String ipAddress;
    private String subnetMask;
    private Long bandwidth;
    private String ospfAreaID;
    private Integer igpCost;
    private String purpose;
    private String operStatus;
    private FacilityStatus facilityStatus;
    private String note;
    private boolean atmPortEditable = false;
    private final String editorName;

    public AtmPvcEditPage(final WebPage backPage, final NodeDto node, final PortDto atmPort, final AtmPvcIfDto port) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            if (port != null) {
                this.port = port;
                this.ifName = PortRenderer.getIfName(port);
                this.vpi = PortRenderer.getVpiAsInteger(port);
                this.vci = PortRenderer.getVciAsInteger(port);
                this.atmPort = AtmPvcUtil.getAtmPort(port);
                this.ipAddress = PortRenderer.getIpAddress(port);
                this.subnetMask = PortRenderer.getSubnetMask(port);
                this.bandwidth = PortRenderer.getBandwidthAsLong(port);
                this.ospfAreaID = PortRenderer.getOspfAreaID(port);
                this.igpCost = PortRenderer.getIgpCostAsInteger(port);
                this.purpose = PortRenderer.getPurpose(port);
                this.operStatus = PortRenderer.getOperStatus(port);
                String fs = PortRenderer.getFacilityStatus(port);
                if (fs != null) {
                    this.facilityStatus = FacilityStatus.getByDisplayString(fs);
                }
                this.note = PortRenderer.getNote(port);
            } else if (atmPort != null) {
                this.port = null;
                this.ifName = null;
                this.atmPort = atmPort;
                this.atmPortEditable = false;
            } else {
                this.port = null;
                this.ifName = null;
                this.atmPort = null;
                this.atmPortEditable = true;
            }
            this.node = node;

            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();

            Form<Void> deleteAtmPvc = new Form<Void>("deleteAtmPvc");
            add(deleteAtmPvc);
            Button proceedButton2 = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        NodeUtil.checkDeletable(port);
                        AtmPvcIfDto pvc = (AtmPvcIfDto) port;
                        AtmPvpIfDto vp = (AtmPvpIfDto) pvc.getOwner();
                        List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
                        AtmPvcPortCommandBuilder builder = new AtmPvcPortCommandBuilder(pvc, editorName);
                        builder.buildDeleteCommand();
                        builders.add(builder);
                        if (vp.getSubElements().size() <= 1) {
                            AtmVpCommandBuilder builder2 = new AtmVpCommandBuilder(vp, editorName);
                            builder2.buildDeleteCommand();
                            builders.add(builder2);
                        }
                        String msg = "Are you sure you want to delete the ATM PVC?";
                        WebPage page = new CommandExecutionConfirmationPage(getBackPage(), msg, builders);
                        setResponsePage(page);
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            proceedButton2.setEnabled(port != null);
            proceedButton2.setVisible(port != null);
            deleteAtmPvc.add(proceedButton2);

            Form<Void> backForm = new Form<Void>("back");
            add(backForm);
            Button backButton2 = new Button("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(getBackPage());
                }
            };
            backForm.add(backButton2);

            this.form = new Form<Void>("editAtmPvc");
            add(this.form);

            form.add(new FeedbackPanel("feedback"));

            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    processEdit();
                    PageUtil.setModelChanged(getBackPage());
                    setResponsePage(getBackPage());
                }
            };
            form.add(proceedButton);

            DropDownChoice<PortDto> atmPortList = new DropDownChoice<PortDto>("atmPort",
                    new PropertyModel<PortDto>(this, "atmPort"),
                    AtmPvcUtil.getAtmPorts(node),
                    new IfNameRenderer());
            atmPortList.setEnabled(atmPortEditable);
            this.form.add(atmPortList);

            TextField<String> ifNameField = new TextField<String>("ifName", new PropertyModel<String>(this, "ifName"));
            this.form.add(ifNameField);

            TextField<Integer> tfvpi = new TextField<Integer>("vpi", new PropertyModel<Integer>(this, "vpi"));
            tfvpi.add(new RangeValidator<Integer>(0, 4095));
            tfvpi.setRequired(true);
            tfvpi.setEnabled(this.port == null);
            this.form.add(tfvpi);
            TextField<Integer> tfvci = new TextField<Integer>("vci", new PropertyModel<Integer>(this, "vci"));
            tfvci.add(new RangeValidator<Integer>(0, 65535));
            tfvci.setRequired(true);
            this.form.add(tfvci);

            DropDownChoice<String> operStatusSelection = new DropDownChoice<String>("operStatus",
                    new PropertyModel<String>(this, "operStatus"),
                    conn.getStatusList(),
                    new OperationStatusChoiceRenderer());
            this.form.add(operStatusSelection);
            DropDownChoice<PortFacilityStatus> facilityStatusList = new DropDownChoice<PortFacilityStatus>("facilityStatus",
                    new PropertyModel<PortFacilityStatus>(this, "facilityStatus"),
                    Arrays.asList(PortFacilityStatus.values()),
                    new ChoiceRenderer<PortFacilityStatus>("displayString"));
            this.form.add(facilityStatusList);

            addTextField(port, "vpnPrefix", "vpnPrefix");
            addTextField(port, "portIpAddress", "ipAddress", new IpAddressValidator());
            addTextField(port, "portIpAddressMask1", "subnetMask");
            addTextField(port, "bandwidth", "bandwidth");
            addTextField(port, "ospfAreaID", "ospfAreaID");
            addTextField(port, "igpCost", "igpCost");
            addTextField(port, "purpose", "purpose");
            addTextField(port, "note", "note");
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private TextField<String> addTextField(PortDto pvc, String id,
                                           String attributeName) {
        return addTextField(pvc, id, attributeName, null, false);
    }

    private TextField<String> addTextField(PortDto pvc, String id,
                                           String attributeName, AbstractValidator<String> validator) {
        return addTextField(pvc, id, attributeName, validator, false);
    }

    private TextField<String> addTextField(PortDto pvc, String id,
                                           String attributeName, AbstractValidator<String> validator, boolean required) {
        TextField<String> tf = new TextField<String>(id, new PropertyModel<String>(this, attributeName));
        form.add(tf);
        tf.setRequired(required);
        if (validator != null) {
            tf.add(validator);
        }
        return tf;
    }

    private void processEdit() {
        if (atmPort == null) {
            throw new IllegalStateException("Parent port is not selected.");
        }
        try {
            List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();

            OspfAreaIdValidator.validate(port, ospfAreaID);
            AtmPvcPortCommandBuilder builder;
            if (port == null) {
                builder = new AtmPvcPortCommandBuilder(atmPort, editorName);
            } else {
                builder = new AtmPvcPortCommandBuilder(port, editorName);
            }
            builder.setIfName(ifName);
            builder.setVpi(vpi);
            builder.setVci(vci);
            builder.setNewIpAddress(vpnPrefix, ipAddress, subnetMask);
            builder.setOperStatus(operStatus);
            builder.setBandwidth(bandwidth);
            builder.setOspfAreaID(ospfAreaID);
            builder.setIgpCost(igpCost);
            builder.setPurpose(purpose);
            if (this.facilityStatus == null) {
                builder.setValue(MPLSNMS_ATTR.FACILITY_STATUS, (String) null);
            } else {
                builder.setValue(MPLSNMS_ATTR.FACILITY_STATUS, this.facilityStatus.getDisplayString());
            }
            builder.setNote(note);
            builder.buildCommand();
            commandBuilderList.add(builder);
            ShellConnector.getInstance().executes(commandBuilderList);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public PortDto getAtmPort() {
        return atmPort;
    }

    public void setAtmPort(PortDto atmPort) {
        this.atmPort = atmPort;
    }

    public AtmPvcIfDto getPort() {
        return port;
    }

    public NodeDto getNode() {
        return node;
    }

    public String getIfName() {
        return this.ifName;
    }

    public void setIfName(String name) {
        this.ifName = name;
    }

    public Integer getVpi() {
        return vpi;
    }

    public void setVpi(Integer vpi) {
        this.vpi = vpi;
    }

    public Integer getVci() {
        return vci;
    }

    public void setVci(Integer vci) {
        this.vci = vci;
    }

    public String getVpnPrefix() {
        return vpnPrefix;
    }

    public void setVpnPrefix(String vpnPrefix) {
        this.vpnPrefix = vpnPrefix;
    }
}