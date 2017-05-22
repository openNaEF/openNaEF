package voss.multilayernms.inventory.web.vpls;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsIfDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.builder.VplsBuilder;
import voss.multilayernms.inventory.util.VplsPortListFilter;
import voss.multilayernms.inventory.web.parts.MultiPortSelectionPanel;
import voss.multilayernms.inventory.web.parts.NodeSelectionPanel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.PageUtil;

import java.util.ArrayList;
import java.util.List;

public class NodeVplsPortSelectionPage extends WebPage {
    private static final long serialVersionUID = 1L;
    public static final String OPERATION_NAME = "NodeVplsPortSelection";
    private final NodeSelectionPanel panel;
    private final MultiPortSelectionPanel portPanel;
    private final VplsDto vpls;
    private String nodeName;
    private NodeDto node;
    private String message;
    private final String editorName;
    private final WebPage backPage;

    public NodeVplsPortSelectionPage(WebPage backPage, final VplsDto vpls, final String nodeName) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.nodeName = nodeName;
            this.backPage = backPage;
            this.vpls = vpls;

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
                    setResponsePage(new NodeVplsPortSelectionPage(getBackPage(),
                            getVpls(), panel.getNodeName()));
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
                        processVplsIf();
                        getVpls().renew();
                        PageUtil.setModelChanged(getBackPage());
                        getBackPage().modelChanged();
                        setResponsePage(getBackPage());
                    } catch (InventoryException e) {
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
                    "portSelectionPanel", NodeVplsPortSelectionPage.this,
                    node, getNodeVplsIfBoundPorts(), new VplsPortListFilter());
            form2.add(this.portPanel);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public void processVplsIf() throws InventoryException {
        if (node == null) {
            throw new IllegalStateException("A node is not specified.");
        }
        VplsBuilder.updateVplsIfBoundPort(vpls, node, getSelectedPort(), editorName);
        vpls.renew();
        this.message = "Has been updated.";
    }

    public List<PortDto> getNodeVplsIfBoundPorts() {
        List<PortDto> ports = new ArrayList<PortDto>();
        if (node == null) {
            return ports;
        }
        VplsIfDto vplsIf = VplsWebUtil.getVplsIf(node, vpls);
        if (vplsIf == null) {
            return ports;
        }
        ports.addAll(VplsWebUtil.getAttachedPorts(vplsIf));
        return ports;
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public VplsDto getVpls() {
        return this.vpls;
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
        return LoggerFactory.getLogger(NodeVplsPortSelectionPage.class);
    }
}