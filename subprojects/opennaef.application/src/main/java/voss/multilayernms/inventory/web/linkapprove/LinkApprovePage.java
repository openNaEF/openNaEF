package voss.multilayernms.inventory.web.linkapprove;

import naef.dto.PortDto;
import naef.dto.ip.IpSubnetDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import voss.core.server.config.CoreConfiguration;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.LinkRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.util.IpSubnetExtUtil;
import voss.multilayernms.inventory.web.link.L3LinkUtil;
import voss.nms.inventory.builder.IpLinkCommandBuilder;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.HistoryUtil;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;

public class LinkApprovePage extends WebPage {
    public static final String OPERATION_NAME = "LinkApprove";
    private final String editorName;

    public LinkApprovePage() {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            final LinkModel model = new LinkModel();
            model.renew();
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            ExternalLink refresh = UrlUtil.getLink("refresh", "linkapprove");
            add(refresh);
            ListView<IpSubnetDto> linkView = new ListView<IpSubnetDto>("links", model) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<IpSubnetDto> item) {
                    final IpSubnetDto subnet = item.getModelObject();
                    final PortDto portA = LinkRenderer.getPort1(subnet);
                    final PortDto portZ = LinkRenderer.getPort2(subnet);
                    item.add(new Label("linkName", Model.of(LinkRenderer.getName(subnet))));
                    item.add(new Label("nodeNameA", Model.of(PortRenderer.getNodeName(portA))));
                    item.add(new Label("ifNameA", Model.of(PortRenderer.getIfName(portA))));
                    item.add(new Label("ipAddressA", Model.of(PortRenderer.getIpAddress(portA))));
                    item.add(new Label("nodeNameZ", Model.of(PortRenderer.getNodeName(portZ))));
                    item.add(new Label("ifNameZ", Model.of(PortRenderer.getIfName(portZ))));
                    item.add(new Label("ipAddressZ", Model.of(PortRenderer.getIpAddress(portZ))));
                    item.add(new Label("status", Model.of(LinkRenderer.getFacilityStatus(subnet))));
                    item.add(new Label("source", Model.of(LinkRenderer.getSource(subnet))));
                    item.add(new Label("onNetwork", Model.of(LinkRenderer.getOnNetworkStatus(subnet))));
                    item.add(new Label("registered", Model.of(LinkRenderer.getRegisteredDate(subnet))));

                    boolean ipAllocated = true;
                    for (PortDto member : subnet.getMemberIpifs()) {
                        if (NodeUtil.getIpAddress(member) == null) {
                            ipAllocated = false;
                            break;
                        }
                    }

                    Form<Void> approveForm = new Form<Void>("form");
                    item.add(approveForm);
                    SubmitLink link = new SubmitLink("approve") {
                        private static final long serialVersionUID = 1L;

                        public void onSubmit() {
                            try {
                                subnet.renew();
                                if (!L3LinkUtil.isApproved(subnet)) {
                                    IpLinkCommandBuilder builder = new IpLinkCommandBuilder(subnet, editorName);
                                    builder.setApproved(true);
                                    builder.buildCommand();
                                    ShellConnector.getInstance().execute(builder);
                                }
                                model.renew();
                                setResponsePage(LinkApprovePage.this);
                            } catch (Exception e) {
                                throw ExceptionUtils.throwAsRuntime(e);
                            }
                        }
                    };
                    link.setEnabled(ipAllocated);
                    link.setVisible(ipAllocated);
                    approveForm.add(link);
                    try {
                        ExternalLink history = HistoryUtil.createHistoryLink(subnet, "History");
                        history.setEnabled(CoreConfiguration.getInstance().isDebug());
                        history.setVisible(CoreConfiguration.getInstance().isDebug());
                        item.add(history);
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            add(linkView);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private static class LinkModel extends RenewableAbstractReadOnlyModel<List<IpSubnetDto>> {
        private static final long serialVersionUID = 1L;
        private final List<IpSubnetDto> subnets = new ArrayList<IpSubnetDto>();

        public LinkModel() {

        }

        public List<IpSubnetDto> getObject() {
            return this.subnets;
        }

        public void renew() {
            this.subnets.clear();
            try {
                MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
                for (IpSubnetDto subnet : conn.getIpSubnetRootPool().getUsers()) {
                    if (!IpSubnetExtUtil.isApproved(subnet)) {
                        this.subnets.add(subnet);
                    }
                }
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
    }

}