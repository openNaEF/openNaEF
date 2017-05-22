package voss.multilayernms.inventory.web.vlan;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIfDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.BuildResult;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.web.parts.InterfaceContainer;
import voss.multilayernms.inventory.web.parts.MultiPortSelectionPanel;
import voss.multilayernms.inventory.web.parts.NodeSelectionPanel;
import voss.nms.inventory.builder.VlanIfBindingCommandBuilder;
import voss.nms.inventory.builder.VlanIfCommandBuilder;
import voss.nms.inventory.constants.SwitchPortMode;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.PageUtil;
import voss.nms.inventory.util.VlanUtil;

import java.util.ArrayList;
import java.util.List;

public class NodeVlanPortSelectionPage extends WebPage {
    private static final long serialVersionUID = 1L;
    public static final String OPERATION_NAME = "NodeVlanPortSelection";
    private final NodeSelectionPanel panel;
    private final MultiPortSelectionPanel portPanel;
    private final VlanDto vlan;
    private String nodeName;
    private NodeDto node;
    private String message;
    private final String editorName;
    private final WebPage backPage;

    public NodeVlanPortSelectionPage(WebPage backPage, final VlanDto vlan, final String nodeName) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.nodeName = nodeName;
            this.backPage = backPage;
            this.vlan = vlan;

            if (nodeName != null) {
                this.node = NodeUtil.getNode(nodeName);
                if (this.node == null) {
                    this.message = "The node does not exist.";
                }
            }

            add(new FeedbackPanel("feedback"));

            Label messageLabel = new Label("message", new PropertyModel<String>(this, "message"));
            add(messageLabel);

            Form<Void> form = new Form<Void>("nodeSelectionForm");
            add(form);

            Button selectButton = new Button("select") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    if (panel.getNodeName() == null) {
                        return;
                    }
                    setResponsePage(new NodeVlanPortSelectionPage(getBackPage(),
                            getVlan(), panel.getNodeName()));
                }
            };
            form.add(selectButton);

            Button back2Button = new Button("back2") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(getBackPage());
                }
            };
            form.add(back2Button);
            this.panel = new NodeSelectionPanel("nodeSelectionPanel", nodeName);
            form.add(this.panel);

            Form<Void> form2 = new Form<Void>("portSelectionForm");
            form2.setEnabled(this.node != null);
            add(form2);

            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        processVlanIf();
                        getVlan().renew();
                        getBackPage().modelChanged();
                        PageUtil.setModelChanged(getBackPage());
                        setResponsePage(getBackPage());
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            proceedButton.setEnabled(node != null);
            proceedButton.setVisible(node != null);
            form2.add(proceedButton);

            Button backButton = new Button("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(getBackPage());
                }
            };
            form2.add(backButton);

            this.portPanel = new MultiPortSelectionPanel(
                    "portSelectionPanel", NodeVlanPortSelectionPage.this,
                    node, VlanUtil.getMemberPorts(this.vlan), new VlanPortListFilter());
            form2.add(this.portPanel);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public void processVlanIf() throws Exception {
        if (node == null) {
            throw new IllegalStateException("A node is not specified.");
        }
        if (VlanUtil.isVlanEnabled(this.node)) {
            VlanIfDto vif = VlanUtil.getSwitchVlanIf(node, vlan.getVlanId());
            if (vif == null) {
                createSwitchVlanIf();
                node.renew();
                vif = VlanUtil.getVlanIf(this.node, this.vlan.getVlanId());
            }
            processBind(vif);
        } else {
            for (InterfaceContainer container : this.portPanel.getListed()) {
                if (container.isNew()) {
                    createRouterVlanIf(container.getPort());
                } else if (container.isObsolete()) {
                    deleteRouterVlanIf(container.getPort());
                }
            }
        }
        vlan.renew();
        this.message = "Has been updated.";
    }

    private void createSwitchVlanIf() throws Exception {
        VlanIfCommandBuilder builder1 = new VlanIfCommandBuilder(node, editorName);
        builder1.setVlan(vlan);
        builder1.setVlanId(vlan.getVlanId());
        BuildResult result = builder1.buildCommand();
        if (BuildResult.SUCCESS != result) {
            throw new IllegalStateException("Cannot create vlan-if. build-result is [" + result + "]");
        }
        ShellConnector.getInstance().execute(builder1);
    }

    private void processBind(VlanIfDto vif) throws Exception {
        VlanIfBindingCommandBuilder builder2 = new VlanIfBindingCommandBuilder(vif, editorName);
        List<PortDto> tagged = getTaggedPorts(this.portPanel.getSelected());
        List<PortDto> untagged = getUntaggedPorts(this.portPanel.getSelected());
        builder2.setTaggedPorts(toAbsoluteNameList(tagged), toIfNameList(tagged));
        builder2.setUntaggedPorts(toAbsoluteNameList(untagged), toIfNameList(untagged));
        BuildResult result2 = builder2.buildCommand();
        if (BuildResult.SUCCESS != result2) {
            throw new IllegalStateException("Cannot create vlan-if. build-result is [" + result2 + "]");
        }
        ShellConnector.getInstance().execute(builder2);
    }

    private void createRouterVlanIf(PortDto port) throws Exception {
        VlanIfDto subIF = VlanUtil.getRouterVlanIf(port, this.vlan.getVlanId());
        if (subIF != null) {
            return;
        }
        VlanIfCommandBuilder builder = new VlanIfCommandBuilder(node, editorName);
        builder.setVlan(vlan);
        builder.setVlanId(vlan.getVlanId());
        BuildResult result = builder.buildCommand();
        if (BuildResult.SUCCESS != result) {
            throw new IllegalStateException("Cannot create vlan-if. build-result is [" + result + "]");
        }
        ShellConnector.getInstance().execute(builder);
    }

    private void deleteRouterVlanIf(PortDto port) throws Exception {
        VlanIfDto subIF = VlanUtil.getRouterVlanIf(port, this.vlan.getVlanId());
        if (subIF == null) {
            return;
        }
        VlanIfCommandBuilder builder = new VlanIfCommandBuilder(subIF, editorName);
        BuildResult result = builder.buildDeleteCommand();
        if (BuildResult.SUCCESS != result) {
            throw new IllegalStateException("Cannot delete vlan-if. build-result is [" + result + "]");
        }
        ShellConnector.getInstance().execute(builder);
    }

    private List<PortDto> getTaggedPorts(List<PortDto> ports) {
        List<PortDto> result = new ArrayList<PortDto>();
        for (PortDto port : ports) {
            SwitchPortMode mode = PortRenderer.getSwitchPortModeValue(port);
            if (mode == SwitchPortMode.TRUNK) {
                result.add(port);
            }
        }
        return result;
    }

    private List<PortDto> getUntaggedPorts(List<PortDto> ports) {
        List<PortDto> result = new ArrayList<PortDto>();
        for (PortDto port : ports) {
            SwitchPortMode mode = PortRenderer.getSwitchPortModeValue(port);
            if (mode != SwitchPortMode.TRUNK) {
                result.add(port);
            }
        }
        return result;
    }

    private List<String> toAbsoluteNameList(List<PortDto> ports) {
        List<String> result = new ArrayList<String>();
        for (PortDto port : ports) {
            result.add(port.getAbsoluteName());
        }
        return result;
    }

    private List<String> toIfNameList(List<PortDto> ports) {
        List<String> result = new ArrayList<String>();
        for (PortDto port : ports) {
            String ifName = DtoUtil.getIfName(port);
            if (ifName == null) {
                continue;
            }
            result.add(ifName);
        }
        return result;
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public VlanDto getVlan() {
        return this.vlan;
    }

    public String getNodeName() {
        return this.nodeName;
    }

    public void setNodeName(String name) {
        this.nodeName = name;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<PortDto> getSelectedPort() {
        return this.portPanel.getSelected();
    }

    @SuppressWarnings("unused")
    private Logger log() {
        return LoggerFactory.getLogger(NodeVlanPortSelectionPage.class);
    }
}