package voss.multilayernms.inventory.web.vm;

import naef.dto.LocationDto;
import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.PatternValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.web.location.LocationUtil;
import voss.multilayernms.inventory.web.location.LocationViewPage;
import voss.multilayernms.inventory.web.parts.CommandExecutionConfirmationPage;
import voss.multilayernms.inventory.web.parts.WithEmptyChoiceRenderer;
import voss.multilayernms.inventory.web.util.AsciiTextValidator;
import voss.multilayernms.inventory.web.util.IpAddressValidator;
import voss.nms.inventory.builder.VirtualNodeCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.PageUtil;
import voss.nms.inventory.util.VirtualNodeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VirtualNodeEditPage extends WebPage {
    public static final String OPERATION_NAME = "VirtualNodeEdit";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(VirtualNodeEditPage.class);
    private final NodeDto host;
    private final NodeDto guest;
    private final Form<Void> form;
    private final TextField<String> nodeIdField;
    private final TextField<String> managementIpAddressField;
    private final DropDownChoice<String> consoleTypeChoice;

    private String nodeId = null;
    private String hyperVisorName = null;
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
    private String osType = null;
    private String osVersion = null;
    private String note = null;

    private final String editorName;
    private final WebPage backPage;

    public VirtualNodeEditPage(final WebPage backPage, final NodeDto guest, final NodeDto host) {
        if (host == null) {
            throw new IllegalArgumentException();
        }
        log.debug("arg: guest=" + DtoUtil.toDebugString(guest));
        log.debug("arg: host=" + DtoUtil.toDebugString(host));
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            this.host = host;
            this.hyperVisorName = this.host.getName();
            this.guest = guest;
            if (guest != null) {
                this.nodeId = VirtualNodeUtil.getGuestNodeName(guest);
                String _hyperVisorName = VirtualNodeUtil.getHostNodeName(guest);
                if (!_hyperVisorName.equals(this.hyperVisorName)) {
                    throw new IllegalArgumentException("hypervisor mismatch: " + this.hyperVisorName + ", " + _hyperVisorName);
                }
                this.vendor = NodeRenderer.getVendorName(guest);
                this.nodeType = NodeRenderer.getNodeType(guest);
                this.managementIpAddress = NodeRenderer.getManagementIpAddress(guest);
                this.snmpMode = NodeRenderer.getSnmpMode(guest);
                this.snmpCommunity = NodeRenderer.getSnmpCommunity(guest);
                this.consoleType = NodeRenderer.getCliMode(guest);
                this.loginUser = NodeRenderer.getConsoleLoginUserName(guest);
                this.loginPassword = NodeRenderer.getConsoleLoginPassword(guest);
                this.adminUser = NodeRenderer.getPrivilegedUserName(guest);
                this.adminPassword = NodeRenderer.getPrivilegedLoginPassword(guest);
                this.purpose = NodeRenderer.getPurpose(guest);
                this.osType = NodeRenderer.getOsType(guest);
                this.osVersion = NodeRenderer.getOsVersion(guest);
                this.note = NodeRenderer.getNote(guest);
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
                    log.debug("delete vm...");
                    List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
                    try {
                        builders.add(getBuilder());
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                    LocationDto loc = LocationUtil.getLocation(guest);
                    PageParameters params = LocationUtil.getParameters(loc);
                    WebPage forwardTo = new LocationViewPage(params);
                    WebPage page = new CommandExecutionConfirmationPage(
                            getBackPage(), forwardTo, "Are you sure to delete the virtual node?", builders);
                    setResponsePage(page);
                }
            };
            deleteLink.setEnabled(this.guest != null);
            deleteLink.setVisible(this.guest != null);
            add(deleteLink);

            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();

            this.form = new Form<Void>("form");
            add(this.form);

            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    log.debug("submit vm...");
                    process();
                    PageUtil.setModelChanged(getBackPage());
                    setResponsePage(getBackPage());
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

            TextField<String> vendor = new TextField<String>("vendor",
                    new PropertyModel<String>(this, "vendor"), String.class);
            this.form.add(vendor);

            TextField<String> nodeType = new TextField<String>("nodeType",
                    new PropertyModel<String>(this, "nodeType"), String.class);
            this.form.add(nodeType);

            TextField<String> purpose = new TextField<String>("purpose",
                    new PropertyModel<String>(this, "purpose"), String.class);
            this.form.add(purpose);

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

            TextArea<String> note = new TextArea<String>("note",
                    new PropertyModel<String>(this, "note"));
            this.form.add(note);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void process() {
        if (getNodeId() == null) {
            throw new IllegalArgumentException("Node Name is mandatory.");
        }
        try {
            VirtualNodeCommandBuilder builder;
            if (this.guest == null) {
                builder = new VirtualNodeCommandBuilder(editorName);
            } else {
                builder = new VirtualNodeCommandBuilder(this.guest, editorName);
            }
            builder.setHyperVisor(this.host);
            builder.setGuestNodeName(this.nodeId);
            builder.setIpAddress(this.managementIpAddress);
            builder.setSnmpMode(this.snmpMode);
            builder.setSnmpCommunityRO(this.snmpCommunity);
            builder.setCliMode(this.consoleType);
            builder.setAdminPassword(this.adminPassword);
            builder.setAdminUser(this.adminUser);
            builder.setLoginUser(this.loginUser);
            builder.setLoginPassword(this.loginPassword);
            builder.setNote(this.note);
            builder.setOsType(this.osType);
            builder.setOsVersion(this.osVersion);
            builder.setPurpose(this.purpose);
            builder.setVendor(this.vendor);
            builder.setNodeType(this.nodeType);
            if (guest == null) {
                builder.setSource(DiffCategory.INVENTORY.name());
            }
            builder.buildCommand();
            ShellConnector.getInstance().execute(builder);
            if (guest != null) {
                guest.renew();
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private VirtualNodeCommandBuilder getBuilder() throws InventoryException, ExternalServiceException, IOException {
        if (guest == null) {
            throw new IllegalArgumentException("no guest node.");
        }
        checkDeletable();
        VirtualNodeCommandBuilder builder = new VirtualNodeCommandBuilder(guest, editorName);
        builder.setCascadeDelete(true);
        builder.buildDeleteCommand();
        return builder;
    }

    private void checkDeletable() {
        if (this.guest == null) {
            return;
        }
        for (PortDto port : guest.getPorts()) {
            Set<NetworkDto> networks = port.getNetworks();
            if (networks != null && networks.size() > 0) {
                throw new IllegalStateException("port is in use: Internal-ID=" + port.getAbsoluteName());
            }
        }
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public NodeDto getHost() {
        return this.host;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

}