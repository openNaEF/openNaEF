package voss.multilayernms.inventory.util;

import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.MplsNmsLogCategory;
import voss.multilayernms.inventory.constants.PseudoWireType;
import voss.multilayernms.inventory.renderer.PseudoWireRenderer;

public class PseudoWireExtUtil {
    private static final Logger log = LoggerFactory.getLogger(MplsNmsLogCategory.LOG_DEBUG);

    public static PortDto getAc1(PseudowireDto pw) {
        if (pw == null) {
            return null;
        }
        return pw.getAc1();
    }

    public static PortDto getAc2(PseudowireDto pw) {
        if (pw == null) {
            return null;
        }
        return pw.getAc2();
    }

    public static PseudoWireType getPseudoWireType(PseudowireDto pw) {
        if (pw == null) {
            return null;
        }
        String type = PseudoWireRenderer.getPseudoWireType(pw);
        try {
            PseudoWireType pwType = PseudoWireType.valueOf(type);
            return pwType;
        } catch (Exception e) {
            log.warn("unexpected pseudowire-type: " + type, e);
            return null;
        }
    }

    public static boolean isCpipeType(PseudowireDto pw) {
        PseudoWireType pwType = getPseudoWireType(pw);
        return pwType == PseudoWireType.CPIPE;
    }

    public static boolean isEpipeType(PseudowireDto pw) {
        PseudoWireType pwType = getPseudoWireType(pw);
        return pwType == PseudoWireType.EPIPE;
    }
}