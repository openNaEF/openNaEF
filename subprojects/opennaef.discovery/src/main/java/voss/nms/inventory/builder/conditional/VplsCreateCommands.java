package voss.nms.inventory.builder.conditional;

import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vpls.VplsStringIdPoolDto;
import naef.ui.NaefDtoFacade;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.NodeUtil;
import voss.model.VplsInstance;
import voss.nms.inventory.builder.SimpleNodeBuilder;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.network.NetworkDiffUtil;
import voss.nms.inventory.util.VplsUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VplsCreateCommands extends ConditionalCommands<VplsIfDto> {
    private static final long serialVersionUID = 1L;
    private final List<String> memberPorts = new ArrayList<String>();
    private final String vplsName;
    private final String nodeName;

    public VplsCreateCommands(VplsInstance vpls, String editorName) {
        super(editorName);
        this.vplsName = vpls.getVplsID();
        this.nodeName = vpls.getDevice().getDeviceName();
    }

    public void addMemberPorts(String ifName) {
        if (!memberPorts.contains(ifName)) {
            memberPorts.add(ifName);
        }
    }

    @Override
    public void evaluateDiffInner(ShellCommands cmd) {
        try {
            String ifAbsoluteName = nodeName + ATTR.NAME_DELIMITER_PRIMARY + ATTR.TYPE_VPLS_IF
                    + ATTR.NAME_DELIMITER_SECONDARY + vplsName;
            NodeDto node = NodeUtil.getNode(nodeName);
            VplsDto vpls = getNetwork();
            VplsStringIdPoolDto pool = getIdPool();
            if (vpls == null) {
                InventoryBuilder.changeContext(cmd, pool);
                InventoryBuilder.buildNetworkIDCreationCommand(cmd, ATTR.NETWORK_TYPE_VPLS,
                        ATTR.ATTR_VPLS_ID_STRING, vplsName,
                        ATTR.ATTR_VPLS_POOL_STRING, pool.getName());
                InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.VPLS_NAME, vplsName);
                cmd.addLastEditCommands();
                recordChange(MPLSNMS_ATTR.VPLS_NAME, null, vplsName);
            }
            VplsIfDto vplsIf = VplsUtil.getVplsIf(node, vplsName);
            if (vplsIf == null) {
                InventoryBuilder.changeContext(cmd, ATTR.TYPE_NODE, nodeName);
                SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_VPLS_IF, vplsName);
                InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.IFNAME, vplsName);
                cmd.addLastEditCommands();
            }
            InventoryBuilder.changeContext(cmd, pool, ATTR.NETWORK_TYPE_ID, vplsName);
            InventoryBuilder.buildBindPortToNetworkCommands(cmd, ifAbsoluteName);
            cmd.addLastEditCommands();

            NaefDtoFacade facade = InventoryConnector.getInstance().getDtoFacade();
            List<PortDto> addedPorts = super.getAddedPortsByIfName(memberPorts, node, facade);
            InventoryBuilder.changeContext(cmd, ifAbsoluteName);
            for (PortDto added : addedPorts) {
                InventoryBuilder.buildConnectPortToNetworkIfCommands(cmd, ifAbsoluteName, added.getAbsoluteName());
                recordChange("attachement port", null, DtoUtil.getIfName(added));
            }
            clearAssertions();
            addAssertion(pool);
            NetworkDto network = getNetwork();
            if (network != null) {
                addAssertion(network);
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    protected VplsDto getNetwork() throws IOException, InventoryException, ExternalServiceException {
        VplsStringIdPoolDto targetPool = getIdPool();
        if (targetPool == null) {
            return null;
        }
        for (VplsDto vpls : targetPool.getUsers()) {
            if (vpls.getStringId().equals(this.vplsName)) {
                return vpls;
            }
        }
        return null;
    }

    protected String getIdPoolName() throws IOException, InventoryException, ExternalServiceException {
        VplsStringIdPoolDto pool = getIdPool();
        return pool.getAbsoluteName();
    }

    protected VplsStringIdPoolDto getIdPool() throws IOException, InventoryException, ExternalServiceException {
        String vplsDomainName = NetworkDiffUtil.getPolicy().getDefaultVplsPoolName();
        NaefDtoFacade facade = InventoryConnector.getInstance().getDtoFacade();
        Set<VplsStringIdPoolDto> pools = facade.getRootIdPools(VplsStringIdPoolDto.class);
        for (VplsStringIdPoolDto pool : pools) {
            if (pool.getName().equals(vplsDomainName)) {
                return pool;
            }
        }
        throw new IllegalStateException("pool not exist:" + vplsDomainName);
    }
}