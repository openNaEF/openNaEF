package voss.multilayernms.inventory.web.parts;

import naef.dto.HardPortDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.atm.AtmPvpIfDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.pos.PosApsIfDto;
import naef.dto.serial.TdmSerialIfDto;
import naef.dto.vlan.VlanIfDto;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import voss.core.server.config.CoreConfiguration;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.web.link.L3LinkUtil;
import voss.multilayernms.inventory.web.node.*;
import voss.nms.inventory.constants.SwitchPortMode;
import voss.nms.inventory.util.HistoryUtil;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.NodeUtil;

public class PortRowPanel extends Panel {
    private static final long serialVersionUID = 1L;

    public PortRowPanel(String id, final WebPage parentPage, final PortDto port, final String editorName) {
        super(id);
        try {
            final NodeDto node = port.getNode();
            port.renew();
            String endOfUseClass = null;
            if (NodeUtil.isEndOfUse(port)) {
                endOfUseClass = "end-of-use";
            }
            AttributeAppender appender = new AttributeAppender("class", new Model<String>(endOfUseClass), " ");
            add(appender);
            String ifName = NameUtil.getIfName(port);
            if (NodeUtil.isSubInterface(port)) {
                if (port instanceof AtmPvpIfDto && !NodeUtil.isImplicit(port)) {
                    ifName = "_ " + ifName;
                } else {
                    ifName = ">　" + ifName;
                }
            }
            Link<Void> editLink = new Link<Void>("editInterface") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    port.renew();
                    if (port.isAlias()) {
                        AliasPortEditPage page = new AliasPortEditPage(parentPage, node, port);
                        setResponsePage(page);
                    } else if (port instanceof IpIfDto) {
                        LoopbackPortEditPage page = new LoopbackPortEditPage(parentPage, node, (IpIfDto) port);
                        setResponsePage(page);
                    } else if (port instanceof AtmPvcIfDto) {
                        AtmPvcIfDto pvc = (AtmPvcIfDto) port;
                        AtmPvcEditPage page = new AtmPvcEditPage(parentPage, node, pvc.getPhysicalPort(), pvc);
                        setResponsePage(page);
                    } else if (port instanceof TdmSerialIfDto) {
                        TdmSerialIfDto channel = (TdmSerialIfDto) port;
                        PortDto parent = (PortDto) channel.getOwner();
                        ChannelPortEditPage page = new ChannelPortEditPage(parentPage, node,
                                parent, channel);
                        setResponsePage(page);
                    } else if (port instanceof VlanIfDto) {
                        VlanIfDto vlanIf = (VlanIfDto) port;
                        VlanIfEditPage page = new VlanIfEditPage(parentPage,
                                (PortDto) vlanIf.getOwner(), vlanIf);
                        setResponsePage(page);
                    } else if (port instanceof AtmApsIfDto || port instanceof PosApsIfDto || port instanceof EthLagIfDto) {
                        LogicalPortEditPage page = new LogicalPortEditPage(parentPage,
                                port.getNode(), port);
                        setResponsePage(page);
                    } else if (port instanceof HardPortDto) {
                        PortEditPage page = new PortEditPage(parentPage, (HardPortDto) port);
                        setResponsePage(page);
                    } else {
                        throw new IllegalStateException("unexpected type: " + port.getAbsoluteName());
                    }
                }
            };
            editLink.add(new Label("ifName", new Model<String>(ifName)));

            Link<Void> createSubIf = new Link<Void>("createSubIf") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    try {
                        WebPage next = SubInterfaceLogic.getSubInterfacePage(port, parentPage, editorName);
                        setResponsePage(next);
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            Label captionLabel = new Label("caption", Model.of(SubInterfaceLogic.getCaption(port)));
            createSubIf.add(captionLabel);
            createSubIf.setEnabled(SubInterfaceLogic.isSubInterfaceEnable(port));
            createSubIf.setVisible(SubInterfaceLogic.isSubInterfaceEnable(port));
            add(createSubIf);
            add(editLink);
            add(new Label("ifName2", new Model<String>(ifName)));
            add(new Label("sourceIfName", new Model<String>(NameUtil.getNodeIfName(port.getAliasSource()))));
            add(new Label("resourcePermission", new Model<String>(PortRenderer.getResourcePermission(port))));
            add(new Label("operStatus", new Model<String>(PortRenderer.getOperStatus(port))));
            add(new Label("portType", new Model<String>(PortRenderer.getPortType(port))));
            add(new Label("portMode", new Model<String>(PortRenderer.getPortMode(port))));
            add(new Label("switchPortMode", new Model<String>(PortRenderer.getSwitchPortMode(port))));
            add(new Label("bandwidth", new Model<String>(PortRenderer.getBandwidth(port))));
            add(new Label("purpose", new Model<String>(PortRenderer.getPurpose(port))));
            add(new Label("note", new Model<String>(PortRenderer.getNote(port))));
            Link<Void> vportListLink = new Link<Void>("vportListLink") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    VportListPage page = new VportListPage(this.getWebPage(), port);
                    setResponsePage(page);
                }
            };
            vportListLink.add(new Label("swPort", new Model<String>(PortRenderer.getSwPort(port))));
            if (PortRenderer.getSwPort(port) != null && PortRenderer.getSwPort(port).equals(SwitchPortMode.TRUNK.name())) {
                vportListLink.setEnabled(true);
            } else {
                vportListLink.setEnabled(false);
            }
            add(vportListLink);

            add(new Label("autoNegotiation", new Model<String>(PortRenderer.getAutoNegotiation(port))));
            add(new Label("administrativeSpeed", new Model<String>(PortRenderer.getAdministrativeSpeedAsString(port))));
            add(new Label("operationalSpeed", new Model<Integer>(PortRenderer.getOperationalSpeed(port))));
            add(new Label("operationalDuplex", new Model<String>(PortRenderer.getOperationalDuplex(port))));
            String portIpAddress = PortRenderer.getIpAddress(port);
            String portIpMask1 = PortRenderer.getSubnetMask(port);
            if (portIpAddress != null && portIpMask1 != null) {
                portIpAddress = portIpAddress + "/" + portIpMask1;
            }
            add(new Label("portIpAddress", new Model<String>(portIpAddress)));
            add(new Label("ospfAreaID", new Model<String>(PortRenderer.getOspfAreaID(port))));
            add(new Label("igpCost", new Model<String>(PortRenderer.getIgpCost(port))));
            add(new Label("vpi", new Model<String>(PortRenderer.getVpi(port))));
            add(new Label("vci", new Model<String>(PortRenderer.getVci(port))));

            Label lastEditor = new Label("lastEditor", new Model<String>(PortRenderer.getLastEditor(port)));
            add(lastEditor);
            Label lastEditTime = new Label("lastEditTime", new Model<String>(PortRenderer.getLastEditTime(port)));
            add(lastEditTime);
            Label version = new Label("version", Model.of(PortRenderer.getVersion(port)));
            add(version);
            ExternalLink history = HistoryUtil.createHistoryLink(port, "History");
            history.setEnabled(CoreConfiguration.getInstance().isDebug());
            history.setVisible(CoreConfiguration.getInstance().isDebug());
            add(history);

            PortDto l2Neighbor = NodeUtil.getLayer2Neighbor(port);
            Link<Void> l2Link = NodePageUtil.createPortLink("layer2Neighbor", l2Neighbor);
            add(l2Link);
            Link<Void> editNeighbor1 = NodePageUtil.createNeighborEditLink(parentPage, "editNeighbor2", port);
            editNeighbor1.setEnabled(L3LinkUtil.isL3LinkCapablePort(port));
            editNeighbor1.setVisible(L3LinkUtil.isL3LinkCapablePort(port));
            if (port instanceof EthPortDto) {
                Label editNeighbor2Caption = new Label("caption", Model.of("Edit"));
                editNeighbor1.add(editNeighbor2Caption);
            } else {
                Label editNeighbor2Caption = new Label("caption", Model.of(""));
                editNeighbor1.add(editNeighbor2Caption);

            }
            add(editNeighbor1);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public WebPage createDestinationSelector(PortDto port) {
        DestinationSelectionPage page = new DestinationSelectionPage(getWebPage(), port);
        return page;
    }
}