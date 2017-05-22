package opennaef.builder;

import naef.dto.of.OfPatchLinkDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.util.NameUtil;

import java.io.IOException;

public class OfPatchLinkPatchIdPoolCommandBuilder extends AbstractCommandBuilder {
    public static final Class<OfPatchLinkDto.PatchIdPoolDto> TARGET_CLASS = OfPatchLinkDto.PatchIdPoolDto.class;
    public static final String OBJECT_TYPE = ATTR.POOL_TYPE_OF_PATCH_LINK_PATCH_ID;

    private static final String NAME = "name";

    private final OfPatchLinkDto.PatchIdPoolDto _pool;
    private String _name;

    public OfPatchLinkPatchIdPoolCommandBuilder(String poolName, String editorName) {
        super(TARGET_CLASS, null, editorName);
        setConstraint(TARGET_CLASS);
        _pool = null;
        name(poolName);
    }

    public OfPatchLinkPatchIdPoolCommandBuilder(OfPatchLinkDto.PatchIdPoolDto pool, String editorName) {
        super(TARGET_CLASS, pool, editorName);
        setConstraint(TARGET_CLASS);
        _pool = pool;
        name(pool.getName());
    }

    public OfPatchLinkPatchIdPoolCommandBuilder name(String poolName) {
        if (poolName == null) {
            throw new IllegalArgumentException("name is null.");
        }
        String pre = _pool == null ? null : _pool.getName();
        if (isRecordChange(NAME, pre, poolName)) {
            _name = poolName;
        }
        return this;
    }

    private boolean isRecordChange(String attr, String pre, String next) {
        if (_pool == null) {
            recordChange(attr, pre, next);
            return true;
        } else {
            if (!pre.equals(next)) {
                recordChange(attr, pre, next);
                return true;
            }
        }
        return false;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, ExternalServiceException, InventoryException {
        if (_pool == null) {
            InventoryBuilder.buildHierarchicalModelCreationCommand(cmd, ATTR.POOL_TYPE_OF_PATCH_LINK_PATCH_ID, _name);
            InventoryBuilder.translate(cmd, CMD.POOL_RANGE_ALLOCATE, CMD.POOL_RANGE_ALLOCATE_ARG1, "-~");
        } else {
            InventoryBuilder.changeContext(cmd, _pool);
            if (isChanged(NAME)) {
                InventoryBuilder.buildRenameCommands(cmd, _name);
            }
        }
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, ExternalServiceException, InventoryException {
        InventoryBuilder.translate(cmd, CMD.REMOVE_ELEMENT,
                "_TYPE_", _pool.getObjectTypeName(),
                "_NAME_", _pool.getName());
        recordChange(target.getObjectTypeName(), NameUtil.getName(this.target), null);
        return BuildResult.SUCCESS;
    }

    @Override
    public String getObjectType() {
        return OBJECT_TYPE;
    }
}