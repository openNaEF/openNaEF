package voss.multilayernms.inventory.web.mpls;

import naef.dto.NodeDto;
import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.atm.AtmPvpIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.fr.FrPvcIfDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.serial.TdmSerialIfDto;
import naef.dto.vlan.VlanIfDto;
import naef.ui.NaefDtoFacade;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.MVO.MvoId;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.PortFilter;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.web.parts.NodeSelectionPanel;
import voss.multilayernms.inventory.web.parts.SinglePortSelectionPanel;
import voss.nms.inventory.util.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NodePseudoWireAcPortSelectionPage extends WebPage {
    private static final long serialVersionUID = 1L;
    public static final String OPERATION_NAME = "NodePseudoWireAcPortSelection";
    private final NodeSelectionPanel panel;
    private final SinglePortSelectionPanel portPanel;
    private String nodeName;
    private NodeDto node;
    private String message;
    @SuppressWarnings("unused")
    private final String editorName;
    private final int target;
    private PortDto selected = null;
    private final PseudoWireEditPage backPage;

    public NodePseudoWireAcPortSelectionPage(PseudoWireEditPage backPage,
                                             final PortDto ac, final int target, final String targetNodeName) {
        try {
            this.backPage = backPage;
            this.target = target;
            this.selected = ac;
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            if (targetNodeName == null) {
                if (ac != null) {
                    this.nodeName = ac.getNode().getName();
                } else {
                    this.nodeName = null;
                }
            } else {
                this.nodeName = targetNodeName;
            }

            if (this.nodeName != null) {
                this.node = NodeUtil.getNode(this.nodeName);
                if (this.node == null) {
                    this.message = "The node does not exist.";
                }
            }

            add(new FeedbackPanel("feedback"));

            Label messageLabel = new Label("message", new PropertyModel<String>(this, "message"));
            add(messageLabel);

            Form<Void> form = new Form<Void>("nodeSelectionForm");
            add(form);
            this.panel = new NodeSelectionPanel("nodeSelectionPanel", this.nodeName);
            form.add(this.panel);

            Button selectPortButton = new Button("selectPort") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    if (panel.getNodeName() == null) {
                        return;
                    }
                    setResponsePage(new NodePseudoWireAcPortSelectionPage(getBackPage(),
                            ac, target, panel.getNodeName()));
                }
            };
            form.add(selectPortButton);

            Button back1Button = new Button("back1") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(getBackPage());
                }
            };
            form.add(back1Button);

            Form<Void> form2 = new Form<Void>("portSelectionForm");
            form2.setEnabled(this.node != null);
            add(form2);

            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        processPseudoWireAc();
                        PageUtil.setModelChanged(getBackPage());
                        setResponsePage(getBackPage());
                    } catch (InventoryException e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            form2.add(proceedButton);

            Button back2Button = new Button("back2") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(getBackPage());
                }
            };
            form2.add(back2Button);

            this.portPanel = new SinglePortSelectionPanel(
                    "portSelectionPanel", NodePseudoWireAcPortSelectionPage.this,
                    this.nodeName, this.selected, getFilter(),
                    new PseudoWireCheckFilter(node, getBackPage().getCurrentPwDto()));
            form2.add(this.portPanel);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public void processPseudoWireAc() throws InventoryException {
        PortDto selectedAc = portPanel.getSelected();
        if (selectedAc == null) {
            throw new IllegalStateException("Please select a port.");
        }
        getBackPage().setAc(target, selectedAc);
        PageUtil.setModelChanged(getBackPage());
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

    public PortDto getSelectedPort() {
        return this.portPanel.getSelected();
    }

    private PortFilter getFilter() {
        PortFilter filter = new PortFilter() {
            private static final long serialVersionUID = 1L;
            private Set<MvoId> pseudoWiresOnNode = null;

            public boolean match(PortDto port) {
                log().debug("target: " + port.getAbsoluteName());
                if (this.pseudoWiresOnNode == null) {
                    try {
                        MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
                        NaefDtoFacade facade = conn.getDtoFacade();
                        this.pseudoWiresOnNode = new HashSet<MvoId>();
                        for (PseudowireDto pw : facade.getPseudowires(port.getNode())) {
                            if (pw.getAc1() != null) {
                                this.pseudoWiresOnNode.add(DtoUtil.getMvoId(pw.getAc1()));
                            }
                            if (pw.getAc2() != null) {
                                this.pseudoWiresOnNode.add(DtoUtil.getMvoId(pw.getAc2()));
                            }
                        }
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
                if (this.pseudoWiresOnNode.contains(DtoUtil.getMvoId(port))) {
                    return false;
                }
                if (port instanceof EthPortDto) {
                    if (VlanUtil.isBridgePort(port)) {
                        return false;
                    }
                    return true;
                } else if (port instanceof TdmSerialIfDto) {
                    return true;
                } else if (port instanceof AtmPvcIfDto) {
                    return true;
                } else if (port instanceof AtmPvpIfDto) {
                    if (PortRenderer.getParmanent(port) != null) {
                        return true;
                    }
                    return false;
                } else if (port instanceof FrPvcIfDto) {
                    return true;
                } else if ((port instanceof VlanIfDto)) {
                    NodeElementDto owner = port.getOwner();
                    log().debug("owner: " + port.getAbsoluteName());
                    if (owner instanceof PortDto) {
                        return true;
                    }
                }

                return false;
            }
        };
        return filter;
    }

    private class PseudoWireCheckFilter implements PortFilter {
        private static final long serialVersionUID = 1L;
        private final List<PseudowireDto> pws = new ArrayList<PseudowireDto>();
        private final PseudowireDto current;

        public PseudoWireCheckFilter(NodeDto node, PseudowireDto current) {
            this.current = current;
            try {
                pws.addAll(PseudoWireUtil.getPseudoWiresOn(node));
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }

        public boolean match(PortDto port) {
            boolean isUsedByCurrent = NodeUtil.isSamePort(port, current.getAc1())
                    || NodeUtil.isSamePort(port, current.getAc2());
            if (isUsedByCurrent) {
                return true;
            }
            for (PseudowireDto pw : pws) {
                boolean inUse = isInUse(port, pw.getAc1());
                if (!inUse) {
                    inUse = isInUse(port, pw.getAc2());
                }
                if (inUse) {
                    return false;
                }
            }
            return true;
        }

        private boolean isInUse(PortDto port, PortDto ac) {
            if (ac != null && NodeUtil.isSamePort(port, ac)) {
                return true;
            }
            return false;
        }
    }

    public PseudoWireEditPage getBackPage() {
        return this.backPage;
    }

    private Logger log() {
        return LoggerFactory.getLogger(NodePseudoWireAcPortSelectionPage.class);
    }
}