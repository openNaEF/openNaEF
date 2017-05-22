package voss.nms.inventory.diff.network.diffunitbuilder;

import naef.dto.NaefDto;
import naef.dto.ip.IpSubnetDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.builder.GenericAttributeCommandBuilder;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.model.VlanModel;
import voss.nms.inventory.config.DiffConfiguration;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.*;
import voss.nms.inventory.diff.network.DiffPolicy;
import voss.nms.inventory.diff.network.NetworkDiffUtil;
import voss.nms.inventory.diff.network.OnDeleteAction;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class DiffUnitBuilderImpl<T1 extends VlanModel, T2 extends NaefDto> implements DiffUnitBuilder<T1, T2> {
    private final Logger log = LoggerFactory.getLogger(DiffUnitBuilderImpl.class);
    protected final DiffSet set;
    protected final int depth;
    protected final String typeName;
    protected final String editorName;
    protected final DiffPolicy policy;

    public DiffUnitBuilderImpl(DiffSet set, String typeName, int depth, String editorName)
            throws InventoryException, ExternalServiceException, IOException {
        this.set = set;
        this.typeName = typeName;
        this.depth = depth;
        this.editorName = editorName;
        this.policy = DiffConfiguration.getInstance().getDiffPolicy();
    }

    @Override
    public void buildDiffUnits(Map<String, T1> networkMap, Map<String, T2> dbMap) {
        Set<String> idSet = new HashSet<String>();
        idSet.addAll(dbMap.keySet());
        idSet.addAll(networkMap.keySet());
        for (String id : idSet) {
            try {
                log.debug("target:" + id);
                T1 networkSubnet = networkMap.get(id);
                T2 dbSubnet = dbMap.get(id);
                DiffUnit unit = buildDiffUnit(id, networkSubnet, dbSubnet);
                if (unit == null) {
                    continue;
                }
                set.addDiffUnit(unit);
            } catch (Exception e) {
                log.error("failed to create diffunit.", e);
            }
        }
    }

    private DiffUnit buildDiffUnit(String id, T1 network, T2 db)
            throws InventoryException, IOException, ExternalServiceException, DuplicationException {
        DiffUnit unit = null;
        if (db == null && network != null) {
            log.debug("- create:" + id);
            unit = create(id, network);
        } else if (db != null && network != null) {
            log.debug("- update:" + id);
            unit = update(id, network, db);
        } else if (db != null && network == null) {
            log.debug("- delete:" + id);
            OnDeleteAction action = this.policy.getOnDeleteAction();
            switch (action) {
                case StatusChange:
                    unit = changeDeletedStatus(id, db);
                    break;
                default:
                    unit = delete(id, db);
                    break;
            }
        } else {
            log.debug("- id: no task");
            return null;
        }
        if (unit == null) {
            log.debug("- no changes.");
            return null;
        }
        unit.setSourceSystem(DiffCategory.DISCOVERY.name());
        unit.setStatus(DiffStatus.INITIAL);
        unit.setTypeName(this.typeName);
        unit.setDepth(this.depth);
        log.debug("- diff=" + unit.getDiffContentDescription());
        return unit;
    }

    abstract protected DiffUnit create(String id, T1 network) throws IOException, InventoryException, ExternalServiceException, DuplicationException;

    abstract protected DiffUnit update(String id, T1 network, T2 db) throws IOException, InventoryException, ExternalServiceException, DuplicationException;

    abstract protected DiffUnit delete(String id, T2 db) throws IOException, InventoryException, ExternalServiceException, DuplicationException;

    protected void applyExtraAttributes(DiffOperationType opType, CommandBuilder builder, T1 network, T2 db) {
        this.policy.setExtraAttributes(opType, builder, network, db);
    }

    protected DiffUnit changeDeletedStatus(String id, T2 db) throws IOException, InventoryException, ExternalServiceException {
        String currentStatus = DtoUtil.getStringOrNull(db, MPLSNMS_ATTR.FACILITY_STATUS);
        GenericAttributeCommandBuilder builder = new GenericAttributeCommandBuilder(db, editorName);
        builder.setVersionCheck(false);
        builder.setConstraint(db.getClass());
        builder.setAttribute(MPLSNMS_ATTR.FACILITY_STATUS, policy.getDefaultDeletedFacilityStatus(currentStatus));
        BuildResult result = builder.buildCommand();
        if (result != BuildResult.SUCCESS) {
            return null;
        } else if (!builder.hasChange()) {
            return null;
        }
        String targetID = id;
        if (db instanceof IpSubnetDto) {
            targetID = ((IpSubnetDto) db).getSubnetName();
        }
        DiffUnit unit = new DiffUnit(targetID, DiffOperationType.UPDATE);
        unit.addBuilder(builder);
        unit.setSourceSystem(DiffCategory.DISCOVERY.name());
        unit.setStatus(DiffStatus.INITIAL);
        unit.setTypeName(this.typeName);
        unit.setDepth(this.depth);
        NetworkDiffUtil.fillDiffContents(unit, builder);
        NetworkDiffUtil.setVersionCheckValues(builder);
        return unit;
    }
}