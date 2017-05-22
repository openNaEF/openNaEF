package voss.multilayernms.inventory.web.node;

import naef.dto.CustomerInfoDto;
import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.vlan.VlanSegmentGatewayIfDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.renderer.CustomerInfoRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.UrlUtil;
import voss.nms.inventory.util.VlanUtil;

import java.util.ArrayList;
import java.util.List;

public class VportListPage extends WebPage {
    public static final String OPERATION_NAME = "VportList";

    private final WebPage backPage;

    public VportListPage(final WebPage backPage, PortDto parentPort) {
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);

            this.backPage = backPage;
            Link<Void> backLink = new Link<Void>("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    setResponsePage(getBackPage());
                }
            };
            add(backLink);

            Label ifNameLabel = new Label("ifName", NameUtil.getIfName(parentPort));
            add(ifNameLabel);

            List<VlanSegmentGatewayIfDto> vlanSegmentGatewayIfDtoList = new ArrayList<VlanSegmentGatewayIfDto>();
            for (NodeElementDto el : parentPort.getSubElements()) {
                if (el != null && el instanceof VlanSegmentGatewayIfDto) {
                    vlanSegmentGatewayIfDtoList.add((VlanSegmentGatewayIfDto) el);
                }
            }


            ListView<VlanSegmentGatewayIfDto> vports = new ListView<VlanSegmentGatewayIfDto>("vports", vlanSegmentGatewayIfDtoList) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<VlanSegmentGatewayIfDto> item) {
                    VlanSegmentGatewayIfDto vport = item.getModelObject();

                    item.add(new Label("vportId", new Model<String>(vport.getIfname())));
                    item.add(new Label("outerVlanId", new Model<String>(PortRenderer.getTagChargerOuterVlanId(vport))));
                    item.add(new Label("vwanId", new Model<Integer>(VlanUtil.getVlanId(vport.getVlanIf()))));
                    String id = null;
                    if (vport != null && vport.getCustomerInfos() != null) {
                        for (CustomerInfoDto customerInfo : vport.getCustomerInfos()) {
                            id = CustomerInfoRenderer.getCustomerInfoId(customerInfo);
                            break;
                        }
                    }
                    item.add(new Label("customerId", new Model<String>(id)));

                }

            };
            add(vports);


        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public WebPage getBackPage() {
        return backPage;
    }

}