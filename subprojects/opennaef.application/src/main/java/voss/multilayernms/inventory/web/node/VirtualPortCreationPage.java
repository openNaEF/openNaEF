package voss.multilayernms.inventory.web.node;

import naef.dto.NodeDto;
import naef.dto.atm.AtmPortDto;
import naef.dto.eth.EthPortDto;
import naef.dto.pos.PosPortDto;
import naef.dto.serial.SerialPortDto;
import naef.dto.vlan.VlanIfDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.VirtualPortCommandBuilder;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.PageUtil;

import java.util.Arrays;
import java.util.List;

public class VirtualPortCreationPage extends WebPage {
    public static final String OPERATION_NAME = "PortEdit";
    private final Form<Void> form;
    private final NodeDto node;
    private String ifName = null;
    private PortType ifType = PortType.ETHERNET;
    private final String editorName;
    private final WebPage backPage;

    public VirtualPortCreationPage(WebPage backPage, final NodeDto node) {
        if (node == null) {
            throw new IllegalArgumentException("node is missing.");
        } else if (!NodeUtil.isVirtualNode(node)) {
            throw new IllegalArgumentException("This node isn't virtual node: " + node.getName());
        }
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            this.node = node;
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

            TextField<String> ifNameField = new TextField<String>("ifName", new PropertyModel<String>(this, "ifName"));
            ifNameField.setRequired(true);
            this.form.add(ifNameField);
            List<PortType> portTypes = Arrays.asList(PortType.values());
            DropDownChoice<PortType> ifTypeField = new DropDownChoice<PortType>("ifType",
                    new PropertyModel<PortType>(this, "ifType"),
                    portTypes,
                    new ChoiceRenderer<PortType>("caption"));
            ifTypeField.setRequired(true);
            this.form.add(ifTypeField);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public void processUpdate() {
        try {
            VirtualPortCommandBuilder builder = new VirtualPortCommandBuilder(node, editorName);
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
                case VLANIF:
                    builder.setConstraint(VlanIfDto.class);
                    break;
                default:
                    throw new IllegalArgumentException("not supported-type: " + getIfType().getCaption());
            }
            String ifName = Util.s2n(getIfName());
            if (ifName == null) {
                throw new IllegalArgumentException("ifName missing.");
            }
            builder.setIfName(ifName);
            builder.setPortType(getIfType());
            builder.buildCommand();
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

    public PortType getIfType() {
        return this.ifType;
    }

    public void setIfType(PortType type) {
        this.ifType = type;
    }

    public Logger log() {
        return LoggerFactory.getLogger(VirtualPortCreationPage.class);
    }
}