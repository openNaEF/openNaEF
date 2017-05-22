package voss.multilayernms.inventory.util;

import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsIfDto;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VplsExtUtil {

    public static List<VplsIfDto> getVplsIfDtos() throws IOException, ExternalServiceException {
        List<VplsIfDto> result = new ArrayList<VplsIfDto>();
        for (VplsDto user : MplsNmsInventoryConnector.getInstance().getVplsStringPool().getUsers()) {
            result.addAll(user.getMemberVplsifs());
        }
        return result;
    }

}