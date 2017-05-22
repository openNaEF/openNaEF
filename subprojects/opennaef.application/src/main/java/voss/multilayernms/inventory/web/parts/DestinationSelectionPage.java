package voss.multilayernms.inventory.web.parts;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.builder.IpSubnetMemberChangeCommandBuilder;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.IfNameRenderer;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.PageUtil;

import java.util.ArrayList;
import java.util.List;

public class DestinationSelectionPage extends WebPage {
    private static final long serialVersionUID = 1L;
    public static final String OPERATION_NAME = "DestinationSelection";

    private final PortDto original;
    private final WebPage backPage;
    private PortDto moveTo;
    private final String editorName;

    public DestinationSelectionPage(final WebPage backPage, final PortDto original) {
        if (original == null) {
            throw new IllegalArgumentException("original is null.");
        }
        this.backPage = backPage;
        this.original = original;
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);

            Label ifNameLabel = new Label("currentIfName", Model.of(PortRenderer.getIfName(original)));
            add(ifNameLabel);

            Label ipAddressLabel = new Label("ipAddress", Model.of(PortRenderer.getIpAddress(original)));
            add(ipAddressLabel);

            Form<Void> form = new Form<Void>("form");
            add(form);

            SubmitLink okButton = new SubmitLink("apply") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        IpSubnetMemberChangeCommandBuilder builder =
                                new IpSubnetMemberChangeCommandBuilder(getOriginal(), getMoveTo(), editorName);
                        builder.buildCommand();
                        ShellConnector.getInstance().execute(builder);
                        PageUtil.setModelChanged(getBackPage());
                        setResponsePage(getBackPage());
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            form.add(okButton);

            SubmitLink cancelButton = new SubmitLink("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(getBackPage());
                }
            };
            form.add(cancelButton);

            IModel<List<PortDto>> ifNameChoiceModel = new AbstractReadOnlyModel<List<PortDto>>() {
                private static final long serialVersionUID = 1L;

                @Override
                public List<PortDto> getObject() {
                    try {
                        List<PortDto> result = new ArrayList<PortDto>();
                        NodeDto node = original.getNode();
                        if (node == null) {
                            return result;
                        }
                        for (PortDto port : getPorts(node)) {
                            if (NodeUtil.getLayer3Link(port) == null) {
                                result.add(port);
                            }
                        }
                        return result;
                    } catch (Exception e) {
                        throw new IllegalStateException("failed to get ports.", e);
                    }
                }
            };

            final DropDownChoice<PortDto> ifNameChoice = new DropDownChoice<PortDto>(
                    "ifName",
                    new PropertyModel<PortDto>(this, "moveTo"),
                    ifNameChoiceModel,
                    new IfNameRenderer());
            ifNameChoice.setOutputMarkupId(true);
            form.add(ifNameChoice);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private List<PortDto> getPorts(NodeDto node) throws InventoryException {
        List<PortDto> result = new ArrayList<PortDto>();
        if (node == null) {
            return result;
        }
        for (PortDto p : node.getPorts()) {
            if (DtoUtil.isSameMvoEntity(p, original)) {
                continue;
            }
            if (isL3LinkAssignablePort(p)) {
                result.add(p);
            }
        }
        return result;
    }

    private boolean isL3LinkAssignablePort(PortDto port) {
        if (port instanceof IpIfDto) {
            return false;
        } else {
            if (port.getPrimaryIpIf() != null) {
                IpIfDto currentIp = port.getPrimaryIpIf();
                return NodeUtil.getLayer3Link(currentIp) == null
                        && PortRenderer.getIpAddress(currentIp) == null;
            } else {
                return true;
            }
        }
    }

    public PortDto getOriginal() {
        return this.original;
    }

    public PortDto getMoveTo() {
        return this.moveTo;
    }

    public void setMoveTo(PortDto port) {
        this.moveTo = port;
    }

    public WebPage getBackPage() {
        return this.backPage;
    }
}