package voss.multilayernms.inventory.util;

import naef.dto.vrf.VrfDto;
import naef.dto.vrf.VrfIfDto;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VrfExtUtil {

    public static List<VrfIfDto> getVrfIfDtos() throws IOException, ExternalServiceException {
        List<VrfIfDto> result = new ArrayList<VrfIfDto>();
        for (VrfDto user : MplsNmsInventoryConnector.getInstance().getVrfStringPool().getUsers()) {
            result.addAll(user.getMemberVrfifs());
        }
        return result;
    }
}