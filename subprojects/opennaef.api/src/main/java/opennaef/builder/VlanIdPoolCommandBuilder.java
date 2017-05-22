package opennaef.builder;

import naef.dto.vlan.VlanIdPoolDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.constant.ModelConstant;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.NameUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * VlanIdPool builderConfig
 */
public class VlanIdPoolCommandBuilder extends AbstractCommandBuilder {
    private VlanIdPoolDto target;
    private String name;
    private String range;

    /**
     * Create VLAN POOL
     *
     * @param poolName
     * @param poolRange
     * @param editorName
     */
    public VlanIdPoolCommandBuilder(String poolName, String poolRange, String editorName) {
        super(VlanIdPoolDto.class, null, editorName);
        if (poolName == null) {
            throw new IllegalArgumentException("name is null.");
        }
        if (poolRange == null) {
            throw new IllegalArgumentException("range is null.");
        }
        setConstraint(VlanIdPoolDto.class);
        name = poolName;
        range = poolRange;
    }

    /**
     * Update or Delete VLAN POOL
     *
     * @param pool
     * @param editorName
     */
    public VlanIdPoolCommandBuilder(VlanIdPoolDto pool, String editorName) {
        super(VlanIdPoolDto.class, pool, editorName);
        if (pool == null) {
            throw new IllegalArgumentException("pool is null.");
        }
        setConstraint(VlanIdPoolDto.class);
        target = pool;
        name = pool.getName();
        range = pool.getConcatenatedIdRangesStr();
    }

    public void setPurpose(String purpose) {
        setValue(ATTR.PURPOSE, purpose);
    }

    public void setNote(String note) {
        setValue(ATTR.NOTE, note);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, ExternalServiceException, InventoryException {
        if (this.target != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (target == null) {
            // Create
            cmd.addCommand(InventoryBuilder.translate(CMD.CONTEXT_RESET));
            InventoryBuilder.buildHierarchicalModelCreationCommand(cmd, ATTR.POOL_TYPE_VLAN_DOT1Q, this.name);
            cmd.addCommand(InventoryBuilder.translate(CMD.POOL_RANGE_ALLOCATE,
                    CMD.POOL_RANGE_ALLOCATE_ARG1, range));
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
        } else {
            // Update
            InventoryBuilder.changeContext(cmd, target);
            // rename
            if (!this.name.equals(this.target.getName())) {
                InventoryBuilder.buildRenameCommands(this.cmd, this.name);
            }
            InventoryBuilder.buildAttributeUpdateCommand(cmd, target, attributes);
        }
        cmd.addLastEditCommands();
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, ExternalServiceException, InventoryException {
        if(this.target.getUsers().size() > 0) {
            throw new IllegalArgumentException("this pool have VLAN ID, Please remove all VLAN ID at first.");
        }
        InventoryBuilder.changeContext(cmd, target);
        InventoryBuilder.translate(cmd, CMD.POOL_RANGE_RELEASE, CMD.POOL_RANGE_RELEASE_ARG1, range);
        InventoryBuilder.buildHierarchicalModelParentChangeCommand(cmd, ModelConstant.VLAN_POOL_TRASH);
        recordChange(target.getObjectTypeName(), NameUtil.getName(this.target), null);
        return BuildResult.SUCCESS;
    }

    @Override
    public String getObjectType() {
        return DiffObjectType.VLANPOOL.getCaption();
    }
}
