package voss.multilayernms.inventory.web.node;

import naef.dto.LocationDto;
import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.PatternValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.web.location.LocationChoiceRenderer;
import voss.multilayernms.inventory.web.location.LocationUtil;
import voss.multilayernms.inventory.web.location.LocationViewPage;
import voss.multilayernms.inventory.web.parts.CommandExecutionConfirmationPage;
import voss.multilayernms.inventory.web.parts.WithEmptyChoiceRenderer;
import voss.multilayernms.inventory.web.util.AsciiTextValidator;
import voss.multilayernms.inventory.web.util.IpAddressValidator;
import voss.nms.inventory.builder.NodeCommandBuilder;
import voss.nms.inventory.database.MetadataManager;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NodeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NodeEditPage extends WebPage {
    public static final String OPERATION_NAME = "NodeEdit";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(NodeEditPage.class);
    private final NodeDto node;
    private final Form<Void> form;
    private final TextField<String> nodeIdField;
    private final TextField<String> managementIpAddressField;
    private final DropDownChoice<String> consoleTypeChoice;
    private final DropDownChoice<LocationDto> areaChoice;
    private LocationDto area = null;
    private String nodeId = null;
    private String vendor = null;
    private String nodeType = null;
    private String managementIpAddress = null;
    private String snmpMode = null;
    private String snmpCommunity = null;
    private String consoleType = null;
    private String loginUser = null;
    private String loginPassword = null;
    private String adminUser = null;
    private String adminPassword = null;
    private String purpose = null;
    private String applianceType = null;
    private String resourcePermission = null;
    private String osType = null;
    private String osVersion = null;
    private String note = null;
    private boolean vmHostingEnable = false;

    private String power = null;
    private String cpus = null;
    private String cpuProduct = null;
    private String memory = null;
    private String storage = null;
    private String portSummary = null;
    private String nicProduct = null;
    private String sfpPlusProduct = null;

    private final String editorName;
    private final WebPage backPage;

    public NodeEditPage(final WebPage backPage, final NodeDto node, final LocationDto location) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            this.node = node;
            if (node != null) {
                this.nodeId = node.getName();
                this.vendor = NodeRenderer.getVendorName(node);
                this.nodeType = NodeRenderer.getNodeType(node);
                this.area = LocationUtil.getLocation(node);
                this.managementIpAddress = NodeRenderer.getManagementIpAddress(node);
                this.snmpMode = NodeRenderer.getSnmpMode(node);
                this.snmpCommunity = NodeRenderer.getSnmpCommunity(node);
                this.consoleType = NodeRenderer.getCliMode(node);
                this.loginUser = NodeRenderer.getConsoleLoginUserName(node);
                this.loginPassword = NodeRenderer.getConsoleLoginPassword(node);
                this.adminUser = NodeRenderer.getPrivilegedUserName(node);
                this.adminPassword = NodeRenderer.getPrivilegedLoginPassword(node);
                this.purpose = NodeRenderer.getPurpose(node);
                this.applianceType = NodeRenderer.getApplianceType(node);
                this.resourcePermission = NodeRenderer.getResourcePermission(node);
                this.osType = NodeRenderer.getOsType(node);
                this.osVersion = NodeRenderer.getOsVersion(node);
                this.note = NodeRenderer.getNote(node);
                this.vmHostingEnable = NodeRenderer.isVmHostingEnable(node);
                this.power = NodeRenderer.getPower(node);
                this.cpus = NodeRenderer.getCPUs(node);
                this.cpuProduct = NodeRenderer.getCPUProduct(node);
                this.memory = NodeRenderer.getMemory(node);
                this.storage = NodeRenderer.getStorage(node);
                this.portSummary = NodeRenderer.getPortSummary(node);
                this.nicProduct = NodeRenderer.getNICProduct(node);
                this.sfpPlusProduct = NodeRenderer.getSFPPlusProduct(node);

            } else if (location != null) {
                this.area = location;
            } else {
                throw new IllegalArgumentException("both location and node is null.");
            }
            add(new FeedbackPanel("feedback"));

            Link<Void> backLink = new Link<Void>("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    setResponsePage(getBackPage());
                }
            };
            add(backLink);

            Link<Void> deleteLink = new Link<Void>("delete") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    checkDeletable();
                    try {
                        List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
                        NodeCommandBuilder builder = new NodeCommandBuilder(node, editorName);
                        builder.setCascadeDelete(true);
                        builder.buildDeleteCommand();
                        builders.add(builder);
                        LocationDto loc = LocationUtil.getLocation(node);
                        PageParameters params = LocationUtil.getParameters(loc);
                        WebPage forwardTo = new LocationViewPage(params);
                        WebPage page = new CommandExecutionConfirmationPage(
                                getBackPage(), forwardTo, "Are you sure to delete the node?", builders);
                        setResponsePage(page);
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            deleteLink.setEnabled(this.node != null);
            deleteLink.setVisible(this.node != null);
            add(deleteLink);

            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();

            this.form = new Form<Void>("form");
            add(this.form);

            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    if (getNodeId() == null) {
                        throw new IllegalArgumentException("Node Name is mandatory.");
                    }
                    if (getArea() == null) {
                        throw new IllegalArgumentException("AREA Name is mandatory.");
                    }
                    try {
                        NodeCommandBuilder builder = new NodeCommandBuilder(node, editorName);
                        builder.setNodeName(getNodeId());
                        builder.setVmHostingEnabled(isVmHostingEnable());
                        builder.setIpAddress(getManagementIpAddress());
                        builder.setSnmpMode(getSnmpMode());
                        builder.setSnmpCommunityRO(getSnmpCommunity());
                        builder.setPurpose(getPurpose());
                        builder.setCliMode(getConsoleType());
                        builder.setAdminPassword(getAdminPassword());
                        builder.setAdminUser(getAdminUser());
                        builder.setLoginUser(getLoginUser());
                        builder.setLoginPassword(getLoginPassword());
                        builder.setNote(getNote());
                        builder.setOsType(getOsType());
                        builder.setOsVersion(getOsVersion());
                        builder.setPurpose(getPurpose());
                        builder.setValue("appliance_type", getApplianceType());
                        builder.setValue("resource_permission", getResourcePermission());
                        builder.setValue("電源", getPower());
                        builder.setValue("CPUs", getCPUs());
                        builder.setValue("CPUProduct", getCPUProduct());
                        builder.setValue("Memory", getMemory());
                        builder.setValue("Storage", getStorage());
                        builder.setValue("NICProduct", getNICProduct());
                        builder.setValue("SFP+Product", getSFPPlusProduct());
                        builder.setValue("PortSummary", getPortSummary());
                        builder.setLocation("location;" + getArea().getName());
                        builder.setMetadata(getVendor(), getNodeType());
                        if (node == null) {
                            builder.setSource(DiffCategory.INVENTORY.name());
                        }
                        builder.buildCommand();
                        ShellConnector.getInstance().execute(builder);
                        if (node != null) {
                            node.renew();
                        }
                        setResponsePage(SimpleNodeDetailPage.class, NodeUtil.getNodeParameters(getNodeId()));
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            this.form.add(proceedButton);

            this.nodeIdField = new TextField<String>("nodeId", new PropertyModel<String>(this, "nodeId"), String.class);
            this.nodeIdField.setRequired(true);
            this.nodeIdField.add(new PatternValidator("[ -~]*"));
            this.form.add(nodeIdField);

            this.managementIpAddressField = new TextField<String>("managementIpAddress",
                    new PropertyModel<String>(this, "managementIpAddress"), String.class);
            managementIpAddressField.add(new IpAddressValidator());
            this.form.add(managementIpAddressField);

            RadioChoice<String> snmpMethod = new RadioChoice<String>("snmpMode",
                    new PropertyModel<String>(this, "snmpMode"),
                    conn.getSnmpMethodList(),
                    new WithEmptyChoiceRenderer());
            form.add(snmpMethod);

            final MetadataManager mm = MetadataManager.getInstance();
            IModel<List<? extends String>> vendorChoices = new AbstractReadOnlyModel<List<? extends String>>() {
                private static final long serialVersionUID = 1L;

                @Override
                public List<String> getObject() {
                    List<String> vendors = mm.getVendorList();
                    if (vendors == null) {
                        if(node == null){
                            vendors = Collections.emptyList();
                        }else{
                            if(NodeRenderer.getVendorName(node) == null){
                                vendors = Collections.emptyList();
                            }else{
                                vendors.add(NodeRenderer.getVendorName(node));
                            }
                        }
                    }else{
                        if(NodeRenderer.getVendorName(node) == null){
                            vendors = Collections.emptyList();
                        }else{
                            vendors.add(NodeRenderer.getVendorName(node));
                        }
                    }
                    return vendors;
                }
            };

            IModel<List<? extends String>> nodeChoices = new AbstractReadOnlyModel<List<? extends String>>() {
                private static final long serialVersionUID = 1L;

                @Override
                public List<String> getObject() {
                    try {
                        List<String> nodeTypes = mm.getNodeTypeList(getVendor());
                        if (nodeTypes == null) {
                            if(node == null){
                                nodeTypes = Collections.emptyList();
                            }else{
                                if(NodeRenderer.getNodeType(node) == null){
                                    nodeTypes = Collections.emptyList();
                                }else{
                                    nodeTypes.add(NodeRenderer.getNodeType(node));
                                }
                            }
                        }else{
                            if(NodeRenderer.getNodeType(node) == null){
                                nodeTypes = Collections.emptyList();
                            }else{
                                nodeTypes.add(NodeRenderer.getNodeType(node));
                            }
                        }
                        return nodeTypes;
                    } catch (IOException e) {
                        log.warn("failed to find meta-data.", e);
                        return Collections.emptyList();
                    }
                }
            };

            DropDownChoice<String> vendorList = new DropDownChoice<String>("vendorList",
                    new PropertyModel<String>(this, "vendor"), vendorChoices);
            vendorList.setEnabled(this.node == null);
            vendorList.setRequired(true);
            this.form.add(vendorList);

            final DropDownChoice<String> nodeTypeList = new DropDownChoice<String>("nodeTypeList",
                    new PropertyModel<String>(this, "nodeType"), nodeChoices);
            this.form.add(nodeTypeList);
            nodeTypeList.setEnabled(this.node == null);
            nodeTypeList.setRequired(true);
            nodeTypeList.setOutputMarkupId(true);

            vendorList.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(nodeTypeList);
                }
            });

            TextField<String> purpose = new TextField<String>("purpose",
                    new PropertyModel<String>(this, "purpose"), String.class);
            this.form.add(purpose);

            TextField<String> applianceType = new TextField<String>("applianceType",
                    new PropertyModel<String>(this, "applianceType"), String.class);
            this.form.add(applianceType);

            TextField<String> resourcePermission = new TextField<String>("resourcePermission",
                    new PropertyModel<String>(this, "resourcePermission"), String.class);
            this.form.add(resourcePermission);

            TextField<String> power = new TextField<String>("power",
                    new PropertyModel<String>(this, "power"), String.class);
            this.form.add(power);

            TextField<String> cpus = new TextField<String>("cpus",
                    new PropertyModel<String>(this, "cpus"), String.class);
            this.form.add(cpus);

            TextField<String> cpuProduct = new TextField<String>("cpuProduct",
                    new PropertyModel<String>(this, "cpuProduct"), String.class);
            this.form.add(cpuProduct);

            TextField<String> memory = new TextField<String>("memory",
                    new PropertyModel<String>(this, "memory"), String.class);
            this.form.add(memory);

            TextField<String> storage = new TextField<String>("storage",
                    new PropertyModel<String>(this, "storage"), String.class);
            this.form.add(storage);

            TextField<String> portSummary = new TextField<String>("portSummary",
                    new PropertyModel<String>(this, "portSummary"), String.class);
            this.form.add(portSummary);

            TextField<String> nicProduct = new TextField<String>("nicProduct",
                    new PropertyModel<String>(this, "nicProduct"), String.class);
            this.form.add(nicProduct);

            TextField<String> sfpPlusProduct = new TextField<String>("sfpPlusProduct",
                    new PropertyModel<String>(this, "sfpPlusProduct"), String.class);
            this.form.add(sfpPlusProduct);

            List<String> consoleTypes = conn.getConsoleTypeList();
            this.consoleTypeChoice = new DropDownChoice<String>("consoleType",
                    new PropertyModel<String>(this, "consoleType"),
                    consoleTypes, new ChoiceRenderer<String>());
            this.form.add(this.consoleTypeChoice);

            PasswordTextField snmp_ro = new PasswordTextField("snmpCommunity",
                    new PropertyModel<String>(this, "snmpCommunity"));
            snmp_ro.setRequired(false);
            snmp_ro.setResetPassword(false);
            snmp_ro.add(new AsciiTextValidator());
            this.form.add(snmp_ro);

            TextField<String> loginUser = new TextField<String>("loginUser",
                    new PropertyModel<String>(this, "loginUser"), String.class);
            loginUser.setRequired(false);
            this.form.add(loginUser);

            PasswordTextField loginPassword = new PasswordTextField("loginPassword",
                    new PropertyModel<String>(this, "loginPassword"));
            loginPassword.setRequired(false);
            loginPassword.setResetPassword(false);
            this.form.add(loginPassword);

            TextField<String> adminUser = new TextField<String>("adminUser",
                    new PropertyModel<String>(this, "adminUser"), String.class);
            adminUser.setRequired(false);
            this.form.add(adminUser);

            PasswordTextField adminPassword = new PasswordTextField("adminPassword",
                    new PropertyModel<String>(this, "adminPassword"));
            adminPassword.setRequired(false);
            adminPassword.setResetPassword(false);
            this.form.add(adminPassword);

            TextField<String> osType = new TextField<String>("osType",
                    new PropertyModel<String>(this, "osType"), String.class);
            this.form.add(osType);

            TextField<String> osVersion = new TextField<String>("osVersion",
                    new PropertyModel<String>(this, "osVersion"), String.class);
            this.form.add(osVersion);

            CheckBox vmHostingEnableCheckBox = new CheckBox("vmHostingEnable",
                    new PropertyModel<Boolean>(this, "vmHostingEnable"));
            this.form.add(vmHostingEnableCheckBox);

            TextArea<String> note = new TextArea<String>("note",
                    new PropertyModel<String>(this, "note"));
            this.form.add(note);

            IModel<List<LocationDto>> areaChoices = new AbstractReadOnlyModel<List<LocationDto>>() {
                private static final long serialVersionUID = 1L;

                @Override
                public List<LocationDto> getObject() {
                    List<LocationDto> locs = new ArrayList<>();
                    List<LocationDto> alllocs = new ArrayList<>();
                    try {
                        alllocs.addAll(LocationUtil.getAreas());
                    } catch (Exception e) {
                        throw new IllegalStateException("failed to get areas.", e);
                    }
                    if(node != null){
                        locs.add(NodeRenderer.getArea(node));
                        for(LocationDto loc : alllocs){
                            if(!loc.getOid().equals(NodeRenderer.getArea(node).getOid())){
                                locs.add(loc);
                            }
                        }
                        return locs;
                    } else {
                        return alllocs;
                    }
                }
            };

            this.areaChoice = new DropDownChoice<LocationDto>("area",
                    new PropertyModel<LocationDto>(this, "area"),
                    areaChoices, new LocationChoiceRenderer());
            this.areaChoice.setNullValid(true);
            this.areaChoice.setOutputMarkupId(true);
            this.form.add(areaChoice);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void checkDeletable() {
        if (this.node == null) {
            return;
        }
        for (PortDto port : node.getPorts()) {
            Set<NetworkDto> networks = port.getNetworks();
            if (networks != null && networks.size() > 0) {
                throw new IllegalStateException("port is in use: Internal-ID=" + port.getAbsoluteName());
            }
        }
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public NodeDto getNode() {
        return this.node;
    }

    public LocationDto getArea() {
        return this.area;
    }

    public void setArea(LocationDto area) {
        this.area = area;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getManagementIpAddress() {
        return managementIpAddress;
    }

    public void setManagementIpAddress(String managementIpAddress) {
        this.managementIpAddress = managementIpAddress;
    }

    public String getSnmpMode() {
        return snmpMode;
    }

    public void setSnmpMode(String snmpMode) {
        this.snmpMode = snmpMode;
    }

    public String getSnmpCommunity() {
        return snmpCommunity;
    }

    public void setSnmpCommunity(String snmpCommunity) {
        this.snmpCommunity = snmpCommunity;
    }

    public String getConsoleType() {
        return consoleType;
    }

    public void setConsoleType(String consoleType) {
        this.consoleType = consoleType;
    }

    public String getLoginUser() {
        return loginUser;
    }

    public void setLoginUser(String loginUser) {
        this.loginUser = loginUser;
    }

    public String getLoginPassword() {
        return loginPassword;
    }

    public void setLoginPassword(String loginPassword) {
        this.loginPassword = loginPassword;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(String adminUser) {
        this.adminUser = adminUser;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getApplianceType() {
        return applianceType;
    }

    public void setApplianceType(String applianceType) {
        this.applianceType = applianceType;
    }

    public String getResourcePermission() {
        return resourcePermission;
    }

    public void setResourcePermission(String resourcePermission) { this.resourcePermission = resourcePermission; }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public boolean isVmHostingEnable() {
        return vmHostingEnable;
    }

    public void setVmHostingEnable(boolean vmHostingEnable) {
        this.vmHostingEnable = vmHostingEnable;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getPower() {
        return power;
    }

    public void setPower(String power) { this.power = power; }

    public String getCPUs() {
        return cpus;
    }

    public void setCPUs(String cpus) { this.cpus = cpus; }

    public String getCPUProduct() {
        return cpuProduct;
    }

    public void setCPUProduct(String cpuProduct) { this.cpuProduct = cpuProduct; }

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) { this.memory = memory; }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) { this.storage = storage; }

    public String getPortSummary() {
        return portSummary;
    }

    public void setPortSummary(String portSummary) { this.portSummary = portSummary; }

    public String getNICProduct() {
        return nicProduct;
    }

    public void setNICProduct(String nicProduct) { this.nicProduct = nicProduct; }

    public String getSFPPlusProduct() {
        return sfpPlusProduct;
    }

    public void setSFPPlusProduct(String sfpPlusProduct) { this.sfpPlusProduct = sfpPlusProduct; }

}