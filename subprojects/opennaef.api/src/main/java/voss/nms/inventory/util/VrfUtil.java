package voss.nms.inventory.util;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vrf.VrfIfDto;
import naef.mvo.vrf.VrfIf;
import naef.ui.NaefDtoFacade;
import naef.ui.NaefDtoFacade.SearchMethod;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.NodeUtil;
import voss.nms.inventory.database.InventoryConnector;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VrfUtil extends voss.core.server.util.VrfUtil {

    public static VrfIfDto getVrfIf(NodeDto node, String vrfName) throws IOException, InventoryException, ExternalServiceException {
        try {
            InventoryConnector conn = InventoryConnector.getInstance();
            NaefDtoFacade facade = conn.getDtoFacade();
            Set<VrfIfDto> vrfs = facade.selectNodeElements(node, VrfIfDto.class,
                    SearchMethod.EXACT_MATCH, VrfIf.Attr.VRF_ID.getName(), vrfName);
            if (vrfs.size() > 1) {
                throw new InventoryException("ambiguous name: " + node.getName() + ":" + vrfName);
            } else if (vrfs.size() == 0) {
                return null;
            }
            return vrfs.iterator().next();
        } catch (RemoteException e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static List<VrfIfDto> getVplsIfsOn(PortDto port) {
        List<VrfIfDto> result = new ArrayList<VrfIfDto>();
        List<VrfIfDto> vrfIfs = NodeUtil.getSpecificPortOn(port.getNode(), VrfIfDto.class);
        for (VrfIfDto vrfIf : vrfIfs) {
            for (PortDto member : vrfIf.getAttachedPorts()) {
                if (DtoUtil.isSameMvoEntity(port, member)) {
                    result.add(vrfIf);
                    break;
                }
            }
        }
        return result;
    }
}