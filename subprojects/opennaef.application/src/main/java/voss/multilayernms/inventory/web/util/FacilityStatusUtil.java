package voss.multilayernms.inventory.web.util;

import naef.dto.InterconnectionIfDto;
import naef.dto.NaefDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vrf.VrfIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.nmscore.model.FakePseudoWire;
import voss.multilayernms.inventory.renderer.GenericRenderer;

public class FacilityStatusUtil {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(FacilityStatusUtil.class);

    public static FacilityStatus getStatus(NaefDto dto) {
        if (dto == null) {
            return null;
        }
        String statusValue = GenericRenderer.getFacilityStatus(dto);
        if (statusValue == null) {
            return null;
        }
        return FacilityStatus.getByDisplayString(statusValue);
    }

    public static boolean isMonitorStatus(NaefDto dto) {
        FacilityStatus fs = getStatus(dto);
        return isMonitorStatus(fs);
    }

    public static boolean isMonitorStatus(String status) {
        if (status == null) {
            return false;
        }
        FacilityStatus fs = FacilityStatus.getByDisplayString(status);
        return isMonitorStatus(fs);
    }

    public static boolean isMonitorStatus(FacilityStatus fs) {
        if (fs == null) {
            return false;
        }
        switch (fs) {
            case RESERVED:
                return false;
            case CONFIGURED:
                return true;
            case TESTED:
                return true;
            case IN_OPERATION:
                return true;
            case NOT_MONITOR:
                return false;
            case END_OPERATION:
                return false;
            case WAITING_FOR_REVOKE:
                return false;
            case REVOKED:
                return false;
            case LOST:
                return false;
            case UNKNOWN:
                return true;
        }
        throw new IllegalArgumentException("unknown status:" + fs.getDisplayString());
    }

    public static boolean isSimulationTarget(NaefDto dto) {
        FacilityStatus fs = getStatus(dto);
        if (fs == null) {
            return false;
        } else if (fs == FacilityStatus.REVOKED) {
            return false;
        } else if (fs == FacilityStatus.LOST) {
            return false;
        }
        return true;
    }

    public static boolean isDeletedStatus(NaefDto dto) {
        FacilityStatus fs = getStatus(dto);
        return fs == FacilityStatus.REVOKED || fs == FacilityStatus.LOST;
    }

    public static boolean isDeletableStatus(NaefDto dto) {
        FacilityStatus fs = getStatus(dto);
        return isDeletableStatus(fs);
    }

    public static boolean isDeletableStatus(FacilityStatus fs) {
        if (fs == null) {
            return false;
        }
        switch (fs) {
            case RESERVED:
                return false;
            case CONFIGURED:
                return true;
            case TESTED:
                return true;
            case IN_OPERATION:
                return true;
            case NOT_MONITOR:
                return true;
            case END_OPERATION:
                return true;
            case WAITING_FOR_REVOKE:
                return true;
            case REVOKED:
                return false;
            case LOST:
                return true;
            case UNKNOWN:
                return true;
        }
        throw new IllegalArgumentException("unknown status:" + fs.getDisplayString());
    }

    public static boolean isAllowableChange(FacilityStatus before, FacilityStatus after) {
        return true;
    }

    public static boolean isAllowableChangeByGUI(RsvpLspDto lsp, FacilityStatus fs) {
        if (lsp == null || fs == null) {
            throw new IllegalArgumentException();
        }
        return true;
    }

    public static boolean isAllowableChangeByGUI(PseudowireDto pw, FacilityStatus fs) {
        if (pw == null || fs == null) {
            throw new IllegalArgumentException();
        }
        return true;
    }

    public static boolean isAllowableChangeByGUI(FakePseudoWire fpw, FacilityStatus fs) {
        if (fpw == null || fs == null) {
            throw new IllegalArgumentException();
        }
        if (fpw.isPseudoWire()) {
            return isAllowableChangeByGUI(fpw.getPseudowireDto(), fs);
        }
        return true;
    }

    public static boolean isAllowableChangeByGUI(VplsIfDto vpls, FacilityStatus fs) {
        return true;
    }

    public static boolean isAllowableChangeByGUI(VrfIfDto vpls, FacilityStatus fs) {
        return true;
    }


    public static boolean isAllowableChangeByGUI(FacilityStatus current, FacilityStatus fs) {
        return true;
    }

    public static boolean isOperStatusTarget(RsvpLspDto lsp) {
        FacilityStatus fs = getStatus(lsp);
        if (fs == null) {
            return false;
        }
        switch (fs) {
            case UNKNOWN:
            case TESTED:
            case CONFIGURED:
            case IN_OPERATION:
                return true;
        }
        return false;
    }

    public static boolean isOperStatusTarget(PseudowireDto pw) {
        FacilityStatus fs = getStatus(pw);
        if (fs == null) {
            return false;
        }
        switch (fs) {
            case UNKNOWN:
            case TESTED:
            case CONFIGURED:
            case IN_OPERATION:
                return true;
        }
        return false;
    }

    public static boolean isOperStatusTarget(InterconnectionIfDto pipe) {
        FacilityStatus fs = getStatus(pipe);
        if (fs == null) {
            return false;
        }
        switch (fs) {
            case UNKNOWN:
            case TESTED:
            case CONFIGURED:
            case IN_OPERATION:
                return true;
        }
        return false;
    }

}