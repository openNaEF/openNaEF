package voss.multilayernms.inventory.web.link;

import naef.dto.LinkDto;
import naef.dto.PortDto;
import naef.dto.eth.EthPortDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.PortFilter;
import voss.multilayernms.inventory.web.parts.NodeSelectionPanel;
import voss.nms.inventory.builder.LinkCommandBuilder;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.PageUtil;

import java.io.IOException;
import java.io.Serializable;

public class LinkNeighborPortSelectionPage extends WebPage {
    public static final String OPERATION_NAME = "NeighborSelection";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(LinkNeighborPortSelectionPage.class);
    private final WebPage backPage;
    private final WebPage linkPage;
    private final PortDto basePort;
    private String nodeName = null;
    private PortDto current = null;
    private final NodeSelectionPanel panel;
    private final LinkNeighborPortSelectionPanel portPanel;
    private final String editorName;

    public LinkNeighborPortSelectionPage(WebPage linkPage, WebPage backPage, PortDto basePort, String nodeName) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            if (backPage == null) {
                throw new IllegalArgumentException();
            }
            this.backPage = backPage;
            this.linkPage = linkPage;
            this.basePort = basePort;
            this.current = NodeUtil.getLayer2Neighbor(basePort);
            this.nodeName = NodeUtil.getNodeName(basePort);
            log.debug("basePort=" + basePort.getAbsoluteName());

            add(new FeedbackPanel("feedback"));
            String ifName = NameUtil.getNodeIfName(basePort) + " ";
            Label ifNameLabel = new Label("ifName", ifName);
            add(ifNameLabel);

            Form<Void> form = new Form<Void>("nodeSelectionForm");
            add(form);
            Button selectNodeButton = new Button("selectNode") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        if (panel.getNodeName() == null) {
                            return;
                        } else if (NodeUtil.getNode(panel.getNodeName()) == null) {
                            throw new InventoryException("No node found with specified name: "
                                    + panel.getNodeName());
                        }
                        setResponsePage(new LinkNeighborPortSelectionPage(
                                getLinkPage(), getBackPage(), getBasePort(), panel.getNodeName()));
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            form.add(selectNodeButton);
            Button backButton = new Button("back1") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(getLinkPage());
                }
            };
            form.add(backButton);
            this.panel = new NodeSelectionPanel("nodeSelectionPanel", nodeName);
            form.add(this.panel);

            Form<Void> form2 = new Form<Void>("portSelectionForm");
            add(form2);
            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    if (getSelectedPort() == null) {
                        log.warn("no port selected");
                        return;
                    }
                    try {
                        processLink();
                        getBasePort().renew();
                        PageUtil.setModelChanged(getLinkPage());
                        PageUtil.setModelChanged(getBackPage());
                        setResponsePage(new LinkEditPage(getBackPage(), getBasePort()));
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            form2.add(proceedButton);
            Button backButton2 = new Button("back2") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(getLinkPage());
                }
            };
            form2.add(backButton2);

            this.portPanel = new LinkNeighborPortSelectionPanel(
                    "portSelectionPanel", LinkNeighborPortSelectionPage.this, nodeName, current,
                    new LinkPortFilter(basePort));
            form2.add(this.portPanel);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public void processLink() throws InventoryException, IOException, ExternalServiceException {
        if (getBasePort() == null || getSelectedPort() == null) {
            return;
        }
        LinkCommandBuilder builder = new LinkCommandBuilder(getBasePort(), getSelectedPort(), this.editorName);
        builder.buildCommand();
        ShellConnector.getInstance().execute(builder);
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public WebPage getLinkPage() {
        return this.linkPage;
    }

    public String getNodeName() {
        return this.nodeName;
    }

    public void setNodeName(String name) {
        this.nodeName = name;
    }

    public PortDto getBasePort() {
        return this.basePort;
    }

    public PortDto getSelectedPort() {
        return this.portPanel.getSelected();
    }

    public static class LinkPortFilter implements PortFilter, Serializable {
        private static final long serialVersionUID = 1L;
        private final PortDto baseEnd;

        public LinkPortFilter(PortDto port) {
            this.baseEnd = port;
        }

        @Override
        public boolean match(PortDto port) {
            Logger log = LoggerFactory.getLogger(LinkPortFilter.class);
            log.debug("base=" + baseEnd.getAbsoluteName());
            log.debug("port=" + port.getAbsoluteName());
            if (!(port instanceof EthPortDto)) {
                log.debug("link is not EthPortDto.");
                return false;
            }
            LinkDto link = NodeUtil.getLayer2Link(port);
            if (link != null) {
                log.debug("link found. result=false");
                return false;
            }
            if (DtoUtil.mvoEquals(baseEnd, port)) {
                return false;
            }
            return true;
        }
    }

}