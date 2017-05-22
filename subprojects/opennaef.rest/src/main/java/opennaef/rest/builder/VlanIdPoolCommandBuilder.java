package opennaef.rest.builder;

import naef.dto.vlan.VlanIdPoolDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.NameUtil;

import java.io.IOException;

/**
 * VlanIdPool builderConfig
 */
public class VlanIdPoolCommandBuilder extends AbstractCommandBuilder {
    private VlanIdPoolDto target;
    private String name;

    /**
     * 新規作成
     *
     * @param poolName
     * @param editorName
     */
    public VlanIdPoolCommandBuilder(String poolName, String editorName) {
        super(VlanIdPoolDto.class, null, editorName);
        if (poolName == null) {
            throw new IllegalArgumentException("name is null.");
        }
        setConstraint(VlanIdPoolDto.class);
        name = poolName;
    }

    /**
     * 更新、削除
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
    }

    public String setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null.");
        }
        this.name = name;
        setValue(MPLSNMS_ATTR.NAME, this.name);
        return this.name;
    }


    @Override
    protected BuildResult buildCommandInner() throws IOException, ExternalServiceException, InventoryException {
//        new-hierarchical-model vlan.id-pool.dot1q "vlans"
//          configure-id-pool allocate-range 1-4095
//        context

        if (target == null) {
            // 新規作成
            InventoryBuilder.buildHierarchicalModelCreationCommand(cmd, ATTR.POOL_TYPE_VLAN_DOT1Q, this.name);
            cmd.addCommand(InventoryBuilder.translate(CMD.POOL_RANGE_ALLOCATE,
                    CMD.POOL_RANGE_ALLOCATE_ARG1, "1-4095"));
        } else {
            // 更新
            InventoryBuilder.changeContext(cmd, target);
            // rename
            if (!this.name.equals(this.target.getName())) {
                InventoryBuilder.buildRenameCommands(this.cmd, this.name);
            }
        }

        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, ExternalServiceException, InventoryException {
        cmd.addCommand(InventoryBuilder.translate(CMD.REMOVE_ELEMENT,
                "_TYPE_", target.getObjectTypeName(),
                "_NAME_", target.getName()));
        recordChange(target.getObjectTypeName(), NameUtil.getName(this.target), null);
        return BuildResult.SUCCESS;
    }

    @Override
    public String getObjectType() {
        return DiffObjectType.VLANPOOL.getCaption();
    }
}
