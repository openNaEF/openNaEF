package voss.multilayernms.inventory.util;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.mpls.RsvpLspDto;
import naef.ui.NaefDtoFacade;
import tef.DateTime;
import tef.TransactionId;
import tef.skelton.dto.EntityDto.Desc;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.mplsnms.MplsnmsAttrs;
import voss.nms.inventory.database.InventoryConnector;

import java.util.SortedMap;

public class RsvpLspExtUtil {

    public static RsvpLspDto getOppositLsp(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        if (lsp.get(MplsnmsAttrs.RsvpLspDtoAttr.COUPLING_PAIR) == null) {
            return null;
        }
        Desc<RsvpLspDto> desc = lsp.get(MplsnmsAttrs.RsvpLspDtoAttr.COUPLING_PAIR);
        if (desc == null) {
            return null;
        }
        return lsp.toDto(desc);
    }

    public static Integer getMaxSdpId(NodeDto node) {
        for (PortDto port : node.getPorts()) {
            if (!(port instanceof IpIfDto)) {
                continue;
            }
            int currentMax = 0;

            return Integer.valueOf(currentMax + 1);
        }
        return null;
    }

    public static RsvpLspDto getPreProvisioningLsp(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }

        try {
            NaefDtoFacade dtofacade = InventoryConnector.getInstance().getDtoFacade();
            SortedMap<TransactionId.W, DateTime> enabledTimeHistory
                    = dtofacade.getAttributeHistory(DtoUtil.getMvoId(lsp), ATTR.TASK_ENABLED_TIME);
            TransactionId.W lastProvisioningVersion
                    = enabledTimeHistory.size() == 0
                    ? null
                    : enabledTimeHistory.lastKey();
            if (lastProvisioningVersion == null) {
                return null;
            }

            TransactionId.W tickBefore = new TransactionId.W(lastProvisioningVersion.serial - 1);
            RsvpLspDto oldVersionLsp
                    = (RsvpLspDto) dtofacade.getMvoDto(DtoUtil.getMvoId(lsp), tickBefore);
            return oldVersionLsp;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }
}