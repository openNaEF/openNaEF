package voss.nms.inventory.util;

import naef.dto.atm.AtmPvcIfDto;
import org.apache.wicket.PageParameters;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.AtmPvcCoreUtil;
import voss.core.server.util.ExceptionUtils;

public class AtmPvcUtil extends AtmPvcCoreUtil {
    public static final String KEY_ATM_PORT = "atm";
    public static final String KEY_ATM_PVC_VPI = "vpi";
    public static final String KEY_ATM_PVC_VCI = "vci";

    public static AtmPvcIfDto getPvc(PageParameters param) throws InventoryException, ExternalServiceException {
        try {
            String nodeName = param.getString(NodeUtil.KEY_NODE);
            String atmIfName = param.getString(KEY_ATM_PORT);
            if (atmIfName == null) {
                throw new IllegalStateException("Invalid argument. ATM parent port not specified. " + param);
            }
            String vpi_ = param.getString(KEY_ATM_PVC_VPI);
            if (vpi_ == null) {
                throw new IllegalStateException("Invalid argument VPI not specified. " + param);
            }
            String vci_ = param.getString(KEY_ATM_PVC_VCI);
            if (vci_ == null) {
                throw new IllegalStateException("Invalid argument VCI not specified. " + param);
            }
            int vpi = Integer.parseInt(vpi_);
            int vci = Integer.parseInt(vci_);
            return getAtmPvc(nodeName, atmIfName, vpi, vci);
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static PageParameters getParameters(AtmPvcIfDto pvc) {
        if (pvc == null) {
            throw new IllegalArgumentException("pvc is null.");
        } else if (pvc.getPhysicalPort() == null) {
            throw new IllegalArgumentException("pvc is not initialized. parent is null.");
        } else if (pvc.getVpi() == null) {
            throw new IllegalArgumentException("pvc is not initialized. vpi is null.");
        } else if (pvc.getVci() == null) {
            throw new IllegalArgumentException("pvc is not initialized. vci is null.");
        }

        PageParameters param = new PageParameters();
        param.add(NodeUtil.KEY_NODE, pvc.getNode().getName());
        param.add(KEY_ATM_PORT, pvc.getPhysicalPort().getName());
        param.add(KEY_ATM_PVC_VPI, pvc.getVpi().toString());
        param.add(KEY_ATM_PVC_VCI, pvc.getVci().toString());
        return param;
    }
}