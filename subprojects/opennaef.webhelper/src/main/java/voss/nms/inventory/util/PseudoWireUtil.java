package voss.nms.inventory.util;

import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.PseudowireLongIdPoolDto;
import naef.dto.mpls.PseudowireStringIdPoolDto;
import org.apache.wicket.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.database.InventoryConnector;

import java.io.IOException;
import java.rmi.RemoteException;

public class PseudoWireUtil extends voss.core.server.util.PseudoWireUtil {
    private static final Logger log = LoggerFactory.getLogger(PseudoWireUtil.class);
    public static final String KEY_POOL_ID = "pool_id";
    public static final String KEY_PSEUDOWIRE_ID = "pw_id";

    public static PseudowireLongIdPoolDto getPool(PageParameters param) throws InventoryException,
            ExternalServiceException, RemoteException, IOException {
        String poolID = param.getString(KEY_POOL_ID);
        if (poolID == null) {
            return null;
        }
        return InventoryConnector.getInstance().getPseudoWireLongIdPool(poolID);
    }

    public static PseudowireStringIdPoolDto getStringPool(PageParameters param) throws InventoryException,
            ExternalServiceException, RemoteException, IOException {
        String poolID = param.getString(KEY_POOL_ID);
        if (poolID == null) {
            return null;
        }
        return InventoryConnector.getInstance().getPseudoWireStringIdPool(poolID);
    }

    public static PageParameters getPoolParam(PseudowireLongIdPoolDto pool) {
        PageParameters param = new PageParameters();
        param.add(KEY_POOL_ID, DtoUtil.getMvoId(pool).toString());
        return param;
    }

    public static PageParameters getPoolParam(PseudowireStringIdPoolDto pool) {
        PageParameters param = new PageParameters();
        param.add(KEY_POOL_ID, DtoUtil.getMvoId(pool).toString());
        return param;
    }

    public static PageParameters getPseudoWireParam(PseudowireLongIdPoolDto pool, PseudowireDto pw) {
        PageParameters param = new PageParameters();
        param.add(KEY_POOL_ID, DtoUtil.getMvoId(pool).toString());
        param.add(KEY_PSEUDOWIRE_ID, pw.getLongId().toString());
        return param;
    }

    public static PageParameters getPseudoWireParam(PseudowireStringIdPoolDto pool, PseudowireDto pw) {
        PageParameters param = new PageParameters();
        param.add(KEY_POOL_ID, DtoUtil.getMvoId(pool).toString());
        param.add(KEY_PSEUDOWIRE_ID, pw.getStringId().toString());
        return param;
    }
}