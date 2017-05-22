package voss.nms.inventory.util;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vpls.VplsIfDto;
import naef.mvo.vpls.VplsIf;
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

public class VplsUtil extends voss.core.server.util.VplsUtil {

    public static VplsIfDto getVplsIf(NodeDto node, String vplsName) throws IOException, InventoryException, ExternalServiceException {
        try {
            InventoryConnector conn = InventoryConnector.getInstance();
            NaefDtoFacade facade = conn.getDtoFacade();
            Set<VplsIfDto> vplses = facade.selectNodeElements(node, VplsIfDto.class,
                    SearchMethod.EXACT_MATCH, VplsIf.Attr.VPLS_ID.getName(), vplsName);
            if (vplses.size() > 1) {
                throw new InventoryException("ambiguous name: " + node.getName() + ":" + vplsName);
            } else if (vplses.size() == 0) {
                return null;
            }
            return vplses.iterator().next();
        } catch (RemoteException e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static List<VplsIfDto> getVplsIfsOn(PortDto port) {
        List<VplsIfDto> result = new ArrayList<VplsIfDto>();
        List<VplsIfDto> vplsIfs = NodeUtil.getSpecificPortOn(port.getNode(), VplsIfDto.class);
        for (VplsIfDto vplsIf : vplsIfs) {
            for (PortDto member : vplsIf.getAttachedPorts()) {
                if (DtoUtil.isSameMvoEntity(port, member)) {
                    result.add(vplsIf);
                    break;
                }
            }
        }
        return result;
    }
}