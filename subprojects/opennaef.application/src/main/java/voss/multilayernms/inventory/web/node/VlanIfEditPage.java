package voss.multilayernms.inventory.web.node;

import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.vlan.VlanIfDto;
import naef.ui.NaefDtoFacade;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.AbstractValidator;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.web.parts.CommandExecutionConfirmationPage;
import voss.multilayernms.inventory.web.parts.OperationStatusChoiceRenderer;
import voss.multilayernms.inventory.web.util.IpAddressValidator;
import voss.nms.inventory.builder.VlanIfCommandBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.PageUtil;

import java.util.*;

public class VlanIfEditPage extends WebPage {
    public static final String OPERATION_NAME = "VlanIfEditor";

    private final WebPage backPage;
    private final PortDto parentPort;
    private VlanIfDto vlanIf;
    private Integer vlanID;
    private String ifName = null;
    private String vpnPrefix = null;
    private String ipAddress = null;
    private String maskLength = null;
    private Long bandwidth = null;
    private String ospfAreaID = null;
    private Integer igpCost = null;
    private String operStatus = null;
    private FacilityStatus facilityStatus;
    private String purpose = null;
    private String note = null;
    private final String editorName;
    private final Form<Void> form;

    public VlanIfEditPage(WebPage backPage, PortDto parentPort, VlanIfDto vlanIf) {
        if (parentPort != null) {
            if (!(parentPort instanceof EthPortDto || parentPort instanceof EthLagIfDto)) {
                throw new IllegalArgumentException("not supported type:" + parentPort.getAbsoluteName());
            }
        }
        this.backPage = backPage;
        this.parentPort = parentPort;
        this.vlanIf = vlanIf;
        if (vlanIf != null) {
            this.vlanID = (vlanIf == null ? null : vlanIf.getVlanId());
            this.ifName = PortRenderer.getIfName(vlanIf);
            this.vpnPrefix = PortRenderer.getVpnPrefix(vlanIf);
            this.ipAddress = PortRenderer.getIpAddress(vlanIf);
            this.maskLength = PortRenderer.getSubnetMask(vlanIf);
            this.bandwidth = PortRenderer.getBandwidthAsLong(vlanIf);
            this.ospfAreaID = PortRenderer.getOspfAreaID(vlanIf);
            this.igpCost = PortRenderer.getIgpCostAsInteger(vlanIf);
            this.operStatus = PortRenderer.getOperStatus(vlanIf);
            this.purpose = PortRenderer.getPurpose(vlanIf);
            this.note = PortRenderer.getNote(vlanIf);
            String fs = PortRenderer.getFacilityStatus(vlanIf);
            if (fs != null) {
                this.facilityStatus = FacilityStatus.getByDisplayString(fs);
            }
        }

        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);

            Label headTitleLabel = new Label("headTitle",
                    NameUtil.getNodeIfName(parentPort) + "Edit VLAN on port");
            add(headTitleLabel);

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
                    try {
                        VlanIfCommandBuilder builder = new VlanIfCommandBuilder(getParentPort(), getVlanIf(), editorName);
                        builder.buildDeleteCommand();
                        List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
                        builders.add(builder);
                        String msg = "Delete this vlan-if.";
                        CommandExecutionConfirmationPage page = new CommandExecutionConfirmationPage(getBackPage(), msg, builders);
                        setResponsePage(page);
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            deleteButton.setEnabled(this.vlanIf != null);
            deleteButton.setVisible(this.vlanIf != null);
            deleteForm.add(deleteButton);

            this.form = new Form<Void>("form");
            add(form);

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

            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            DropDownChoice<String> operStatusSelection = new DropDownChoice<String>("operStatus",
                    new PropertyModel<String>(this, "operStatus"),
                    conn.getStatusList(),
                    new OperationStatusChoiceRenderer());
            operStatusSelection.setNullValid(true);
            this.form.add(operStatusSelection);
            DropDownChoice<FacilityStatus> facilityStatusList = new DropDownChoice<FacilityStatus>("facilityStatus",
                    new PropertyModel<FacilityStatus>(this, "facilityStatus"),
                    Arrays.asList(FacilityStatus.values()),
                    new ChoiceRenderer<FacilityStatus>("displayString"));
            this.form.add(facilityStatusList);

            VacantVlanIdsModel vlanModel = new VacantVlanIdsModel();
            if (vlanID == null) {
                vlanModel.renewVacant();
            } else {
                vlanModel.renewUsed();
            }
            DropDownChoice<Integer> ddc = new DropDownChoice<Integer>(
                    "vlanID",
                    new PropertyModel<Integer>(this, "vlanID"),
                    vlanModel);
            ddc.setEnabled(this.vlanIf == null);
            this.form.add(ddc);

            TextField<String> tf0 = new TextField<String>("ifName", new PropertyModel<String>(this, "ifName"), String.class);
            tf0.setRequired(false);
            this.form.add(tf0);

            String defaultIfName = NameUtil.getDefaultIfName(parentPort) + "." +
                    (vlanIf == null ? "[VLAN ID]" : vlanIf.getVlanId());
            Label defaultIfNameLabel = new Label("defaultIfName", Model.of(defaultIfName));
            this.form.add(defaultIfNameLabel);

            addTextField(vlanIf, "vpnPrefix", "vpnPrefix");
            addTextField(vlanIf, "ipAddress", "ipAddress", new IpAddressValidator());
            addTextField(vlanIf, "maskLength", "maskLength");

            TextField<Long> tfBandwidth = new TextField<Long>("bandwidth", new PropertyModel<Long>(this, "bandwidth"), Long.class);
            tfBandwidth.setRequired(false);
            this.form.add(tfBandwidth);

            addTextField(vlanIf, "ospfAreaID", "ospfAreaID");

            TextField<Integer> tfIgpCost = new TextField<Integer>("igpCost", new PropertyModel<Integer>(this, "igpCost"), Integer.class);
            tfIgpCost.setRequired(false);
            this.form.add(tfIgpCost);

            addTextField(vlanIf, "purpose", "purpose");
            TextArea<String> noteArea = new TextArea<String>("note", new PropertyModel<String>(this, "note"));
            this.form.add(noteArea);

            form.add(new FeedbackPanel("feedback"));

        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private TextField<String> addTextField(PortDto port, String id, String attributeName) {
        return addTextField(port, id, attributeName, null);
    }

    private TextField<String> addTextField(PortDto port, String id, String attributeName, AbstractValidator<String> validator) {
        TextField<String> tf = new TextField<String>(id, new PropertyModel<String>(this, attributeName), String.class);
        this.form.add(tf);
        if (validator != null) {
            tf.add(validator);
        }
        return tf;
    }

    private void processUpdate() {
        try {
            List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();

            VlanIfDto vlanIf = getVlanIf();
            VlanIfCommandBuilder builder;
            if (vlanIf == null) {
                builder = new VlanIfCommandBuilder(parentPort, editorName);
            } else {
                builder = new VlanIfCommandBuilder(parentPort, vlanIf, editorName);
            }
            builder.setVlanId(getVlanId());
            if (Util.s2n(ifName) == null) {
                ifName = NameUtil.getIfName(parentPort) + "." + getVlanId().toString();
                builder.setIfName(ifName);
                builder.setSuffix(getVlanId().toString());
            } else {
                builder.setIfName(ifName);
                builder.setSuffix(Util.getSuffix(ifName));
            }
            builder.setBandwidth(bandwidth);
            builder.setNewIpAddress(this.vpnPrefix, ipAddress, maskLength);
            builder.setBandwidth(bandwidth);
            builder.setOspfAreaID(ospfAreaID);
            builder.setIgpCost(igpCost);
            builder.setPurpose(purpose);
            builder.setOperStatus(operStatus);
            if (facilityStatus != null) {
                builder.setValue(MPLSNMS_ATTR.FACILITY_STATUS, facilityStatus.getDisplayString());
            } else {
                builder.setValue(MPLSNMS_ATTR.FACILITY_STATUS, (String) null);
            }
            builder.setNote(note);
            builder.buildCommand();
            commandBuilderList.add(builder);
            ShellConnector.getInstance().executes(commandBuilderList);
            if (vlanIf != null) {
                vlanIf.renew();
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public WebPage getBackPage() {
        return backPage;
    }

    public PortDto getParentPort() {
        return this.parentPort;
    }

    public Integer getVlanId() {
        return this.vlanID;
    }

    public void setVlanId(Integer id) {
        this.vlanID = id;
    }

    public VlanIfDto getVlanIf() {
        return vlanIf;
    }

    private class VacantVlanIdsModel extends AbstractReadOnlyModel<List<Integer>> {
        private static final long serialVersionUID = 1L;
        private final List<Integer> selectableVlanIds = new ArrayList<Integer>();

        @Override
        public List<Integer> getObject() {
            return selectableVlanIds;
        }

        public void renewVacant() {
            selectableVlanIds.clear();
            List<Integer> result = new ArrayList<Integer>();
            Set<Integer> used = new HashSet<Integer>();
            PortDto parent = getParentPort();
            try {
                MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
                NaefDtoFacade facade = conn.getDtoFacade();
                Set<NodeElementDto> subElements = facade.getDescendants(parent, false);
                for (NodeElementDto subElement : subElements) {
                    if (!(subElement instanceof VlanIfDto)) {
                        continue;
                    }
                    VlanIfDto vif = (VlanIfDto) subElement;
                    used.add(vif.getVlanId());
                }
                for (int i = 1; i < 4095; i++) {
                    Integer vlanId = Integer.valueOf(i);
                    if (used.contains(vlanId)) {
                        continue;
                    }
                    result.add(vlanId);
                }
                Collections.sort(result);
                if (result.size() == 0) {
                    setVlanId(0);
                } else {
                    setVlanId(result.get(0));
                }
                selectableVlanIds.clear();
                selectableVlanIds.addAll(result);
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }

        public void renewUsed() {
            selectableVlanIds.clear();
            selectableVlanIds.add(getVlanId());
        }
    }
}