package voss.multilayernms.inventory.nmscore.view;

import jp.iiga.nmt.core.model.Device;
import jp.iiga.nmt.core.model.PhysicalEthernetPort;
import jp.iiga.nmt.core.model.PhysicalLink;
import net.phalanx.core.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.view.filteringfields.*;

public class FilteringFieldsMakerFactory {

    private final static Logger log = LoggerFactory.getLogger(FilteringFieldsMakerFactory.class);

    public static FilteringFieldsMaker getFilteringFieldsMaker(String objectType) {
        log.debug("objectType:" + objectType);

        if (objectType.equals(Device.class.getSimpleName())) {
            return new NodeFilteringFieldsMaker();
        } else if (objectType.equals(PhysicalEthernetPort.class.getSimpleName())) {
            return new PortFilteringFieldsMaker();
        } else if (objectType.equals(PhysicalLink.class.getSimpleName())) {
            return new LinkFilteringFieldsMaker();
        } else if (objectType.equals(LabelSwitchedPath.class.getSimpleName())) {
            return new RsvpLspFilteringFieldsMaker();
        } else if (objectType.equals(PseudoWire.class.getSimpleName())) {
            return new PseudoWireFilteringFieldsMaker();
        } else if (objectType.equals(Vpls.class.getSimpleName())) {
            return new VplsFilteringFieldsMaker();
        } else if (objectType.equals(Vrf.class.getSimpleName())) {
            return new VrfFilteringFieldsMaker();
        } else if (objectType.equals(Vlan.class.getSimpleName())) {
            return new VlanFilteringFieldsMaker();
        } else if (objectType.equals(Subnet.class.getSimpleName())) {
            return new SubnetFilteringFieldsMaker();
        } else if (objectType.equals(SubnetIp.class.getSimpleName())) {
            return new SubnetIpFilteringFieldsMaker();
        } else if (objectType.equals(VlanLink.class.getSimpleName())) {
            return new VlanLinkFilteringFieldsMaker();
        } else if (objectType.equals(CustomerInfo.class.getSimpleName())) {
            return new CustomerInfoFilteringFieldsMaker();
        }
        throw new IllegalArgumentException("objectType is unknown.");
    }

}