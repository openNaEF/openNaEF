package voss.nms.inventory.database;

import naef.dto.LocationDto;
import naef.dto.NodeDto;
import naef.ui.NaefDtoFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;
import voss.core.server.database.CoreConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.LocationUtil;
import voss.core.server.util.NodeElementComparator;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class InventoryConnector extends CoreConnector {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(InventoryConnector.class);
    private static InventoryConnector instance = null;

    public synchronized static InventoryConnector getInstance() throws IOException {
        if (instance == null) {
            instance = new InventoryConnector();
        }
        return instance;
    }

    public synchronized static void renew() {
        instance = null;
    }

    protected InventoryConnector() throws IOException {
    }

    public List<String> getStatusList() throws ExternalServiceException {
        try {
            List<String> result = getConstants(MPLSNMS_ATTR.OPER_STATUS);
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public List<String> getAdminStatusList() throws ExternalServiceException {
        try {
            List<String> result = getConstants(MPLSNMS_ATTR.ADMIN_STATUS);
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public List<String> getSnmpMethodList() throws ExternalServiceException {
        try {
            List<String> result = getConstants(MPLSNMS_ATTR.SNMP_MODE);
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public List<String> getConsoleTypeList() throws ExternalServiceException {
        try {
            List<String> result = getConstants(MPLSNMS_ATTR.CLI_MODE);
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public List<NodeDto> getActiveNodes() throws ExternalServiceException {
        try {
            List<NodeDto> result = new ArrayList<NodeDto>();
            NaefDtoFacade facade = getDtoFacade();
            Set<NodeDto> nodes = facade.getNodes();
            for (NodeDto node : nodes) {
                if (node == null) {
                    continue;
                }
                LocationDto loc = LocationUtil.getLocation(node);
                if (loc == null) {
                    continue;
                } else if (LocationUtil.isTrash(loc)) {
                    continue;
                } else if (node.getName().contains(ATTR.DELETED)) {
                    continue;
                }
                result.add(node);
            }
            Collections.sort(result, new NodeElementComparator());
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }
}