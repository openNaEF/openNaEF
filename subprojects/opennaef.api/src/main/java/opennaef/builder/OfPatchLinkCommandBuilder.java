package opennaef.builder;

import naef.dto.PortDto;
import naef.dto.of.OfPatchLinkDto;
import opennaef.builder.opennaef.util.AlphanumericComparator;
import opennaef.builder.opennaef.util.ShortestPaths;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OfPatchLinkCommandBuilder extends AbstractCommandBuilder {
    public static final Class<OfPatchLinkDto> TARGET_CLASS = OfPatchLinkDto.class;
    public static final String OBJECT_TYPE = ATTR.TYPE_OF_PATCH_LINK;
    private static final String PATCH_ID = "patch-id";
    private static final String PORT1 = "port1";
    private static final String PORT2 = "port2";

    private final OfPatchLinkDto _ofPatchLink;

    private String _patchId;
    private OfPatchLinkDto.PatchIdPoolDto _pool;
    private PortDto _port1;
    private PortDto _port2;

    public OfPatchLinkCommandBuilder(OfPatchLinkDto.PatchIdPoolDto pool, PortDto port1, PortDto port2, String editorName) {
        super(TARGET_CLASS, null, editorName);
        if (pool == null) {
            throw new IllegalStateException("patch-id-pool is null. pool=" + pool);
        }
        if (port1 == null || port2 == null) {
            throw new IllegalStateException("port1/2 is null. port1=" + port1 + ", port2=" + port2);
        }
        setConstraint(TARGET_CLASS);
        idPool(pool);
        _ofPatchLink = null;
        port1(port1);
        port2(port2);
        patchId(generatePatchId());
    }

    public OfPatchLinkCommandBuilder(OfPatchLinkDto target, String editorName) {
        super(TARGET_CLASS, target, editorName);
        if (target == null) {
            throw new IllegalStateException("of-patch-link is null. target=" + target);
        }
        setConstraint(TARGET_CLASS);
        idPool(target.getPatchIdPool());
        _ofPatchLink = target;
        port1(target.getPatchPort1());
        port2(target.getPatchPort2());
    }

    private OfPatchLinkCommandBuilder patchId(String patchId) {
        String pre = _ofPatchLink == null ? null : _ofPatchLink.getPatchId();
        if (isRecordChange(PATCH_ID, pre, patchId)) {
            _patchId = patchId;
        }
        return this;
    }

    private String generatePatchId() {
        List<String> patchPorts = new ArrayList<>();
        patchPorts.add(_port1.getNode().getName() + ":" + _port1.getIfname());
        patchPorts.add(_port2.getNode().getName() + ":" + _port2.getIfname());
        patchPorts.sort(AlphanumericComparator.INSTANCE);
        return patchPorts.get(0) + "/" + patchPorts.get(1);
    }

    private OfPatchLinkCommandBuilder idPool(OfPatchLinkDto.PatchIdPoolDto pool) {
        if (pool == null) throw new IllegalArgumentException("pool is null.");
        if (_pool != null) throw new IllegalArgumentException("It can not change id-pool.");
        _pool = pool;
        return this;
    }

    public OfPatchLinkCommandBuilder port1(PortDto port) {
        if (port == null) throw new IllegalArgumentException("port1 is null.");
        String pre = _ofPatchLink == null ? null : _ofPatchLink.getPatchPort1().getAbsoluteName();
        if (isRecordChange(PORT1, pre, port.getAbsoluteName())) {
            _port1 = port;
        }
        return this;
    }

    public OfPatchLinkCommandBuilder port2(PortDto port) {
        if (port == null) throw new IllegalArgumentException("port2 is null.");
        String pre = _ofPatchLink == null ? null : _ofPatchLink.getPatchPort2().getAbsoluteName();
        if (isRecordChange(PORT2, pre, port.getAbsoluteName())) {
            _port2 = port;
        }
        return this;
    }

    private boolean isRecordChange(String attr, String pre, String next) {
        if (_ofPatchLink == null) {
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
        if (!hasChange()) {
            return BuildResult.NO_CHANGES;
        }

        checkReachable(_port1, _port2);

        if (_ofPatchLink == null) {
            InventoryBuilder.changeContext(cmd, _pool);
            InventoryBuilder.translate(cmd,
                    CMD.OF_PATCH_LINK_CREATE,
                    "_NAME_", _patchId);
            InventoryBuilder.translate(cmd,
                    CMD.OF_PATCH_LINK_CONFIGURE_SET_PORT,
                    "_OP_", CMD.OF_PATCH_LINK_SET_PORT1,
                    "_NAME_", _port1.getAbsoluteName());
            InventoryBuilder.translate(cmd,
                    CMD.OF_PATCH_LINK_CONFIGURE_SET_PORT,
                    "_OP_", CMD.OF_PATCH_LINK_SET_PORT2,
                    "_NAME_", _port2.getAbsoluteName());
        } else {
            InventoryBuilder.changeContext(cmd, _ofPatchLink.getAbsoluteName());
            if (isChanged(PORT1)) {
                InventoryBuilder.translate(cmd,
                        CMD.OF_PATCH_LINK_CONFIGURE_RESET_PORT,
                        "_OP_", CMD.OF_PATCH_LINK_RESET_PORT1);
                InventoryBuilder.translate(cmd,
                        CMD.OF_PATCH_LINK_CONFIGURE_SET_PORT,
                        "_OP_", CMD.OF_PATCH_LINK_SET_PORT1,
                        "_NAME_", _port1.getAbsoluteName());
            }
            if (isChanged(PORT2)) {
                InventoryBuilder.translate(cmd,
                        CMD.OF_PATCH_LINK_CONFIGURE_RESET_PORT,
                        "_OP_", CMD.OF_PATCH_LINK_RESET_PORT2);
                InventoryBuilder.translate(cmd,
                        CMD.OF_PATCH_LINK_CONFIGURE_SET_PORT,
                        "_OP_", CMD.OF_PATCH_LINK_SET_PORT2,
                        "_NAME_", _port2.getAbsoluteName());
            }
        }
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
        cmd.addLastEditCommands();
        return BuildResult.SUCCESS;
    }

    private static void checkReachable(PortDto from, PortDto to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("The specified port can not be found. " + from + "->" + to);
        }
        boolean isReachable = ShortestPaths.isReachable(from.getNode(), to.getNode());
        if (!isReachable) {
            throw new IllegalArgumentException("It is unreachable. " + from + "->" + to);
        }
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, ExternalServiceException, InventoryException {
        if (_ofPatchLink == null) {
            return BuildResult.NO_CHANGES;
        }
        InventoryBuilder.changeContext(cmd, _ofPatchLink);
        InventoryBuilder.buildNetworkIDReleaseCommand(cmd,
                ATTR.ATTR_OF_PATCH_LINK_PATCH_ID,
                ATTR.ATTR_OF_PATCH_LINK_PATCH_ID_POOL);
        InventoryBuilder.translate(cmd,
                CMD.OF_PATCH_LINK_CONFIGURE_RESET_PORT,
                "_OP_", CMD.OF_PATCH_LINK_RESET_PORT1);
        InventoryBuilder.translate(cmd,
                CMD.OF_PATCH_LINK_CONFIGURE_RESET_PORT,
                "_OP_", CMD.OF_PATCH_LINK_RESET_PORT2);
        recordChange("deleted", this._patchId, "deleted");  // dummy
        return BuildResult.SUCCESS;
    }

    @Override
    public String getObjectType() {
        return OBJECT_TYPE;
    }
}