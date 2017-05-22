package voss.nms.inventory.diff.network.diffunitbuilder;

import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.VlanUtil;
import voss.model.Device;
import voss.model.EthernetSwitch;
import voss.model.VMwareServer;
import voss.model.VlanIf;
import voss.nms.inventory.builder.VlanCommandBuilder;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.DiffOperationType;
import voss.nms.inventory.diff.DiffSet;
import voss.nms.inventory.diff.DiffUnit;
import voss.nms.inventory.diff.network.DiffConstants;
import voss.nms.inventory.diff.network.DiffPolicy;
import voss.nms.inventory.diff.network.NetworkDiffUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VlanIdDiffUnitBuilder extends DiffUnitBuilderImpl<VlanIf, VlanDto> {
    private static final Logger log = LoggerFactory.getLogger(VlanIdDiffUnitBuilder.class);
    private String userName;

    public VlanIdDiffUnitBuilder(DiffSet set, DiffPolicy policy, String editorName) throws InventoryException, ExternalServiceException, IOException {
        super(set, DiffObjectType.VLAN.getCaption(), DiffConstants.vlanIdDepth, editorName);
        this.userName = editorName;
    }

    @Override
    protected DiffUnit update(String inventoryID, VlanIf vif, VlanDto vlan) {
        return null;
    }

    @Override
    protected DiffUnit create(String inventoryID, VlanIf vif) throws IOException, InventoryException, ExternalServiceException {
        String poolName = NetworkDiffUtil.getPolicy().getDefaultVlanPoolName();
        if (poolName == null) {
            return null;
        }
        log.debug("create vlan: " + poolName + ":" + vif.getVlanId());
        VlanIdPoolDto pool = VlanUtil.getPool(poolName);
        VlanCommandBuilder builder = new VlanCommandBuilder(pool, userName);
        builder.setVlanID(Integer.valueOf(vif.getVlanId()));
        builder.buildCommand();
        DiffUnit unit = new DiffUnit(inventoryID, DiffOperationType.ADD);
        unit.addBuilder(builder);
        return unit;
    }

    protected DiffUnit delete(String inventoryID, VlanDto vlan) throws IOException, InventoryException, ExternalServiceException {
        String currentStatus = DtoUtil.getStringOrNull(vlan, MPLSNMS_ATTR.OPER_STATUS);
        String status = NetworkDiffUtil.getPolicy().getDefaultDeletedFacilityStatus(currentStatus);
        log.debug("lost vlan: " + vlan.getIdPool().getName() + ":" + vlan.getVlanId() + " status changed: " + status);
        VlanCommandBuilder builder = new VlanCommandBuilder(vlan, userName);
        builder.setOperStatus(status);
        builder.buildCommand();
        DiffUnit unit = new DiffUnit(inventoryID, DiffOperationType.ADD);
        unit.addBuilder(builder);
        return unit;
    }

    public static Map<String, VlanDto> getDbVlans() throws IOException, ExternalServiceException {
        Map<String, VlanDto> result = new HashMap<String, VlanDto>();
        String poolName = NetworkDiffUtil.getPolicy().getDefaultVlanPoolName();
        VlanIdPoolDto pool = InventoryConnector.getInstance().getVlanPool(poolName);
        for (VlanDto vlan : pool.getUsers()) {
            String id = String.valueOf(vlan.getVlanId());
            result.put(id, vlan);
        }
        return result;
    }

    public static Map<String, VlanIf> getNetworkVlanIfs(Collection<Device> devices) {
        Map<String, VlanIf> result = new HashMap<String, VlanIf>();
        for (Device device : devices) {
            getNetworkVlanIfs(result, device);
        }
        return result;
    }

    private static void getNetworkVlanIfs(Map<String, VlanIf> result, Device device) {
        for (VlanIf vlanIf : device.selectPorts(VlanIf.class)) {
            String id = String.valueOf(vlanIf.getVlanId());
            result.put(id, vlanIf);
        }
        if (!VMwareServer.class.isInstance(device)) {
            return;
        }
        VMwareServer vmware = VMwareServer.class.cast(device);
        for (EthernetSwitch vSwitch : vmware.getVSwitches()) {
            getNetworkVlanIfs(result, vSwitch);
        }
    }
}