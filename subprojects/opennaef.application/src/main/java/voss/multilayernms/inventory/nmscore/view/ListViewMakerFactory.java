package voss.multilayernms.inventory.nmscore.view;

import jp.iiga.nmt.core.model.Device;
import jp.iiga.nmt.core.model.PhysicalEthernetPort;
import jp.iiga.nmt.core.model.PhysicalLink;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.view.list.*;

import java.io.IOException;

public class ListViewMakerFactory {
    private final static Logger log = LoggerFactory.getLogger(ListViewMakerFactory.class);

    public static ListViewMaker getListViewMaker(ObjectFilterQuery query) throws IOException {
        String targetClassName = query.getTarget().getName();
        log.debug("getTarget:" + targetClassName);

        if (targetClassName.equals(Device.class.getName())) {
            return new NodeListViewMaker(query);
        } else if (targetClassName.equals(PhysicalEthernetPort.class.getName())) {
            return new PortListViewMaker(query);
        } else if (targetClassName.equals(PhysicalLink.class.getName())) {
            return new L2LinkListViewMaker(query);
        } else if (targetClassName.equals(VlanLink.class.getName())) {
            return new VlanLinkListViewMaker(query);
        } else if (targetClassName.equals(LabelSwitchedPath.class.getName())) {
            return new RsvpLspListViewMaker(query);
        } else if (targetClassName.equals(PseudoWire.class.getName())) {
            return new PseudoWireListViewMaker(query);
        } else if (targetClassName.equals(Vpls.class.getName())) {
            return new VplsListViewMaker(query);
        } else if (targetClassName.equals(Vrf.class.getName())) {
            return new VrfListViewMaker(query);
        } else if (targetClassName.equals(Vlan.class.getName())) {
            return new VlanListViewMaker(query);
        } else if (targetClassName.equals(Subnet.class.getName())) {
            return new SubnetListViewMaker(query);
        } else if (targetClassName.equals(SubnetIp.class.getName())) {
            return new SubnetIpListViewMaker(query);
        } else if (targetClassName.equals(CustomerInfo.class.getName())) {
            return new CustomerInfoListViewMaker(query);
        } else {
            throw new IllegalArgumentException("Target is unknown.");
        }
    }

}
