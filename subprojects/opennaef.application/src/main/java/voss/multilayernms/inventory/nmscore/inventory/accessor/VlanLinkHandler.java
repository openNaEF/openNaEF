package voss.multilayernms.inventory.nmscore.inventory.accessor;

import naef.dto.NetworkDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vlan.VlanLinkDto;
import net.phalanx.core.models.VlanLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.model.creator.VlanLinkModelCreator;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class VlanLinkHandler {

    private static final Logger log = LoggerFactory.getLogger(VlanLinkHandler.class);

    public static List<VlanLink> getList(String mvoId) throws NotBoundException, AuthenticationException, IOException, InventoryException, ExternalServiceException {
        List<VlanLink> vlanLinkList = new ArrayList<VlanLink>();

        log.debug("vlan mvoId : " + mvoId);
        VlanDto vlan = MplsNmsInventoryConnector.getInstance().getMvoDto(mvoId, VlanDto.class);

        if (vlan != null) {
            for (VlanIfDto vlanIf : vlan.getMemberVlanifs()) {
                HashSet<NetworkDto> vlanlinks = (HashSet<NetworkDto>) vlanIf.getNetworks();
                for (NetworkDto vlanlink : vlanlinks) {
                    if (vlanlink instanceof VlanLinkDto) {
                        if (!vlanLinkList.contains(VlanLinkModelCreator.createModel((VlanLinkDto) vlanlink))) {
                            vlanLinkList.add(VlanLinkModelCreator.createModel((VlanLinkDto) vlanlink));
                        }
                    }
                }
            }
        }
        log.debug("vlanLinkList : " + vlanLinkList.toString());
        return vlanLinkList;
    }

}