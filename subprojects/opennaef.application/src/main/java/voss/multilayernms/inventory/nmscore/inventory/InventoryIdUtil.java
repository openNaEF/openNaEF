package voss.multilayernms.inventory.nmscore.inventory;

import naef.dto.InterconnectionIfDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vrf.VrfIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdBuilder;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.naming.inventory.InventoryIdDecoder;

public class InventoryIdUtil {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(InventoryIdUtil.class);

    public static String getInventoryId(NodeDto node) {
        return InventoryIdCalculator.getId(node);
    }

    public static String getInventoryId(PortDto port) {
        return InventoryIdCalculator.getId(port);
    }

    public static String getInventoryId(IpSubnetDto link) {
        return InventoryIdCalculator.getId(link);
    }

    public static String getInventoryId(RsvpLspDto lsp) {
        return InventoryIdCalculator.getId(lsp);
    }

    public static String getInventoryIdOnAc1(PseudowireDto pw) {
        return InventoryIdCalculator.getId(pw, pw.getAc1());
    }

    public static String getInventoryIdOnAc2(PseudowireDto pw) {
        return InventoryIdCalculator.getId(pw, pw.getAc2());
    }

    public static String getInventoryId(PseudowireDto pw) {
        return InventoryIdCalculator.getId(pw);
    }

    public static String getInventoryIdOnAc(InterconnectionIfDto pipe, PortDto ac) {
        return InventoryIdCalculator.getId(pipe, ac);
    }

    public static String getInventoryId(InterconnectionIfDto pipe) {
        return InventoryIdCalculator.getId(pipe);
    }

    public static String getInventoryId(VplsIfDto vpls) {
        return InventoryIdCalculator.getId(vpls);
    }

    public static String getInventoryId(VrfIfDto vrf) {
        return InventoryIdCalculator.getId(vrf);
    }

    public static String normalizeLspId(String lsppathInventoryId) throws InventoryException {
        try {
            String nodeName = InventoryIdDecoder.getNode(lsppathInventoryId);
            String lspName = InventoryIdDecoder.getLspName(lsppathInventoryId);
            return InventoryIdBuilder.getRsvpLspID(nodeName, lspName);
        } catch (Exception e) {
            throw new InventoryException("parse failed: " + lsppathInventoryId, e);
        }
    }

    public static String escape(String str) {
        return InventoryIdCalculator.escape(str);
    }

    public static String unEscape(String str) {
        return InventoryIdCalculator.unEscape(str);
    }

    public static boolean contains(String inventoryId, String delimiter) {
        return (indexOf(inventoryId, delimiter) != -1);
    }

    public static int containsAmount(String inventoryId, String delimiter) {
        int amount = 0;
        int index = 0;
        while (true) {
            index = indexOf(inventoryId, index, delimiter);

            if (index == -1) {
                break;
            }
            index++;
            amount++;
        }
        return amount;
    }


    public static int indexOf(String inventoryId, String delimiter) {
        return indexOf(inventoryId, 0, delimiter);
    }

    public static int indexOf(String inventoryId, int fromIndex, String delimiter) {

        int index = inventoryId.indexOf(delimiter, fromIndex);
        int indexOfEscaped = inventoryId.indexOf(escape(delimiter), fromIndex);

        if (index == -1) {
            return -1;
        }

        if (index - indexOfEscaped == 1) {
            return indexOf(inventoryId, index + 1, delimiter);
        }

        return index;
    }


}