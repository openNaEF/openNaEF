package voss.nms.inventory.diff.network.diffunitbuilder;

import naef.dto.*;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.atm.AtmPvpIfDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.pos.PosApsIfDto;
import naef.dto.serial.TdmSerialIfDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vlan.VlanSegmentGatewayIfDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vrf.VrfIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.builder.CommandUtil;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.util.ConfigModelUtil;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.NodeUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.DiffOperationType;
import voss.nms.inventory.diff.DiffStatus;
import voss.nms.inventory.diff.DiffUnit;
import voss.nms.inventory.diff.network.DiffPolicy;
import voss.nms.inventory.diff.network.DiffUnitEntityType;
import voss.nms.inventory.diff.network.IpAddressDB;
import voss.nms.inventory.diff.network.NetworkDiffUtil;
import voss.nms.inventory.diff.network.analyzer.*;
import voss.nms.inventory.diff.network.builder.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NodeElementDiffUnitBuilder {
    private static final Logger log = LoggerFactory.getLogger(NodeElementDiffUnitBuilder.class);
    private final IpAddressDB ipDB;
    private final DiffPolicy policy;

    public NodeElementDiffUnitBuilder(DiffPolicy policy, IpAddressDB ipDB) {
        this.ipDB = ipDB;
        this.policy = policy;
    }

    public DiffUnit buildDiffUnit(String id, Renderer discovered, NodeElementDto onDatabase,
                                  DiffOperationType opType, String userName) throws IOException, InventoryException, ExternalServiceException {
        BuilderFactory factory = getBuilderFactory(id, onDatabase, discovered, userName);
        if (factory == null) {
            throw new IllegalStateException("unsupported object-type: " + onDatabase.getObjectTypeName()
                    + " (" + onDatabase.getAbsoluteName() + ")");
        } else if (factory instanceof AbstractPortBuilderFactory) {
            ((AbstractPortBuilderFactory) factory).setIpDB(this.ipDB);
        }
        DiffUnit unit = new DiffUnit(id, opType);
        unit.setSourceSystem(DiffCategory.DISCOVERY.name());
        unit.setStatus(DiffStatus.INITIAL);
        CommandBuilder builder = factory.getBuilder();
        if (builder == null) {
            return null;
        }
        builder.setVersionCheck(false);
        unit.setTypeName(builder.getObjectType());
        policy.setExtraAttributes(opType, builder, RendererUtil.getModel(discovered), onDatabase);
        BuildResult result = null;
        switch (opType) {
            case ADD:
                result = builder.buildCommand();
                if (result == BuildResult.NO_CHANGES) {
                    return null;
                }
                unit.addShellCommands(builder.getCommand());
                CommandUtil.logCommands(builder);
                NetworkDiffUtil.fillDiffContents(unit, builder);
                unit.setDepth(discovered.getDepth());
                unit.setNodeName(ConfigModelUtil.getDeviceName(discovered.getModel()));
                unit.setLocalName(ConfigModelUtil.getLocalName(discovered.getModel()));
                return unit;
            case UPDATE:
                result = builder.buildCommand();
                if (result == BuildResult.NO_CHANGES) {
                    return null;
                }
                NetworkDiffUtil.setVersionCheckValues(builder);
                unit.addShellCommands(builder.getCommand());
                CommandUtil.logCommands(builder);
                NetworkDiffUtil.fillDiffContents(unit, builder);
                unit.setDepth(discovered.getDepth());
                unit.setNodeName(ConfigModelUtil.getDeviceName(discovered.getModel()));
                unit.setLocalName(ConfigModelUtil.getLocalName(discovered.getModel()));
                return unit;
            case REMOVE:
                String source = DtoUtil.getStringOrNull(onDatabase, MPLSNMS_ATTR.SOURCE);
                if (source != null && source.equals(DiffCategory.INVENTORY.name())) {
                    return null;
                }
                int depth = NodeUtil.getDepth(onDatabase);
                unit.setDepth(depth);
                unit.setNodeName(onDatabase.getNode().getName());
                unit.setLocalName(onDatabase.getName());
                result = builder.buildDeleteCommand();
                unit.addShellCommands(builder.getCommand());
                CommandUtil.logCommands(builder);
                return unit;
            case INFORMATION:
                break;
        }
        if (result != BuildResult.SUCCESS) {
            return null;
        }
        return unit;
    }

    private static BuilderFactory getBuilderFactory(String id, NodeElementDto onDatabase, Renderer renderer, String editorName) {
        if (onDatabase == null && renderer == null) {
            throw new IllegalArgumentException();
        }
        if (onDatabase == null) {
            log.trace("getBuilderFactory(): onDatabase is null.");
        }
        DiffUnitEntityType type = NetworkDiffUtil.getEntityType(id, onDatabase, renderer);
        switch (type) {
            case NODE:
                return new NodeBuilderFactory((NodeDto) onDatabase, (DeviceRenderer) renderer, editorName);
            case VIRTUAL_NODE:
                return new VirtualNodeBuilderFactory((NodeDto) onDatabase, (DeviceRenderer) renderer, editorName);
            case CHASSIS:
                return new ChassisBuilderFactory((ChassisDto) onDatabase, (ChassisRenderer) renderer, editorName);
            case SLOT:
                return new SlotBuilderFactory((SlotDto) onDatabase, (SlotRenderer) renderer, editorName);
            case MODULE:
                return new ModuleBuilderFactory((ModuleDto) onDatabase, (ModuleRenderer) renderer, editorName);
            case PHYSICAL_PORT:
                return new PhysicalPortBuilderFactory((HardPortDto) onDatabase, (AbstractHardPortRenderer<?>) renderer, editorName);
            case ALIAS_PORT:
                return new AliasPortBuilderFactory((PortDto) onDatabase, (AliasPortRenderer) renderer, editorName);
            case VM_NIC:
                return new VMEthernetPortBuilderFactory((EthPortDto) onDatabase, (VMEthernetPortRenderer) renderer, editorName);
            case LAG:
                return new EthernetLAGBuilderFactory((EthLagIfDto) onDatabase, (EthernetLAGRenderer) renderer, editorName);
            case EPS:
                return new EthernetEPSBuilderFactory((EthLagIfDto) onDatabase, (EthernetEPSRenderer) renderer, editorName);
            case TAG_CHANGER:
                return new TagChangerBuilderFactory((VlanSegmentGatewayIfDto) onDatabase, (TagChangerRenderer) renderer, editorName);
            case ATM_APS:
                return new ATMAPSBuilderFactory((AtmApsIfDto) onDatabase, (ATMAPSRenderer) renderer, editorName);
            case POS_APS:
                return new POSAPSBuilderFactory((PosApsIfDto) onDatabase, (POSAPSRenderer) renderer, editorName);
            case LOOPBACK:
                return new LoopbackPortBuilderFactory((IpIfDto) onDatabase, (LoopbackPortRenderer) renderer, editorName);
            case VLAN_IF:
                return new VlanIfBuilderFactory((VlanIfDto) onDatabase, (VlanIfRenderer) renderer, editorName);
            case VLAN_IF_BINDING:
                return new VlanIfBindingBuilderFactory((VlanIfDto) onDatabase, (VlanIfBindingRenderer) renderer, editorName);
            case ATM_VP:
                return new AtmVpBuilderFactory((AtmPvpIfDto) onDatabase, (AtmVpRenderer) renderer, editorName);
            case ATM_PVC:
                return new AtmPvcIfBuilderFactory((AtmPvcIfDto) onDatabase, (AtmPvcIfRenderer) renderer, editorName);
            case CHANNEL:
                return new TdmSerialIfBuilderFactory((TdmSerialIfDto) onDatabase, (TdmSerialIfRenderer) renderer, editorName);
            default:
                throw new IllegalStateException("unknown combination. " + type.name());
        }
    }


    public static void prepareNodeElements(NodeDto node, Map<String, NodeElementDto> subElements)
            throws InventoryException, ExternalServiceException, IOException {
        List<NodeElementDto> elements = new ArrayList<NodeElementDto>();
        elements.add(node);
        NodeUtil.getSubNodeElements(elements, node);
        for (NodeElementDto element : elements) {
            if (!isTarget(element)) {
                continue;
            } else if (element instanceof JackDto) {
                continue;
            } else if (element instanceof IpIfDto) {
                IpIfDto ipIf = (IpIfDto) element;
                if (NodeUtil.isLoopback(ipIf)) {
                    String id = InventoryIdCalculator.getId(ipIf);
                    subElements.put(id, element);
                }
                continue;
            }
            String id = InventoryIdCalculator.getId(element);
            log.debug("[DB] inventory-id=" + id + ", absolute-name=" + element.getAbsoluteName());
            subElements.put(id, element);
            if (VlanIfDto.class.isInstance(element)) {
                VlanIfDto vif = VlanIfDto.class.cast(element);
                if (!vif.isAlias()) {
                    String bindingID = InventoryIdCalculator.getId(element) + VlanIfBindingRenderer.SUFFIX;
                    subElements.put(bindingID, element);
                }
            }
        }
    }

    private static boolean isTarget(NodeElementDto element) {
        if (element instanceof VrfIfDto) {
            return false;
        } else if (element instanceof VplsIfDto) {
            return false;
        } else if (element instanceof InterconnectionIfDto) {
            return false;
        }
        return true;
    }
}