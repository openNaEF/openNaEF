package voss.nms.inventory.diff.network;

import naef.dto.InterconnectionIfDto;
import naef.dto.NodeDto;
import naef.dto.NodeElementDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vrf.VrfIfDto;
import naef.ui.NaefDtoFacade;
import naef.ui.NaefDtoFacade.SearchMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdBuilder;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.util.*;
import voss.model.*;
import voss.nms.inventory.builder.complement.IpSubnetAddressComplementCommands;
import voss.nms.inventory.config.DiffConfiguration;
import voss.nms.inventory.constants.IpAddressModel;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.constants.SystemUserConstants;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.*;
import voss.nms.inventory.diff.network.diffunitbuilder.*;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

public class NetworkDiffCalculator {
    private static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);
    private final Map<NodeDto, Device> results;
    private final Set<NodeDto> discoveredNodes = new HashSet<NodeDto>();
    private final Set<Device> virtualDevices = new HashSet<Device>();
    private final Map<Port, String> portToAbsoluteNameMap = new HashMap<Port, String>();
    private final IpAddressDB ipDB = new IpAddressDB();
    private final InventoryConnector conn;
    private final NaefDtoFacade facade;
    private final DiffSet set;
    private final DiffConfiguration config;
    private final DiffPolicy policy;
    private String userName = SystemUserConstants.USER_SYSTEM;
    private final List<PolicyViolation> policyViolations = new ArrayList<PolicyViolation>();

    public NetworkDiffCalculator(Map<NodeDto, Device> results, DiffCategory category)
            throws IOException, InventoryException, ExternalServiceException {
        this.results = results;
        this.set = new DiffSet(category.name());
        this.config = DiffConfiguration.getInstance();
        this.policy = this.config.getDiffPolicy();
        this.conn = InventoryConnector.getInstance();
        this.facade = this.conn.getDtoFacade();
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public IpAddressDB getIpAddressDB() {
        return this.ipDB;
    }

    public Map<NodeDto, Device> getResult() {
        return new HashMap<NodeDto, Device>(this.results);
    }

    public void execute() throws IOException, InventoryException, ExternalServiceException {
        prepare();
        calculateVlanIdDiff();
        calculateIpSubnetDiff();
        calculateNodeDiff();
        calculatePhysicalLinkDiff();
        calculateLspDiff();
        if (this.policy.isPseudoWireStringIdType()) {
            calculatePseudoWireStringTypeDiff();
        } else {
            calculatePseudoWireDiff();
        }
        calculateNodePipeDiff();
        calculateVplsDiff();
        calculateVrfDiff();
        calculateVirtualNodeDiff();
        createIpAddressComplementTransactionDiffUnit();
    }

    private void prepare() {
        for (Map.Entry<NodeDto, Device> result : results.entrySet()) {
            NodeDto node = result.getKey();
            Device device = result.getValue();
            if (device instanceof VMwareServer) {
                VMwareServer vms = VMwareServer.class.cast(device);
                for (EthernetSwitch vSwitch : vms.getVSwitches()) {
                    this.virtualDevices.add(vSwitch);
                }
                for (VirtualServerDevice vm : vms.getVirtualHosts()) {
                    this.virtualDevices.add(vm);
                }
                continue;
            } else {
                this.virtualDevices.addAll(device.getVirtualDevices());
            }
            try {
                prepareInvnetoryIp(node);
                if (device instanceof ErrorDummyDevice) {
                    processErrorDevice(node, (ErrorDummyDevice) device);
                    continue;
                }
                prepareNetworkIp(device);
                prepareDeviceName(device, node);
                this.discoveredNodes.add(node);
            } catch (RemoteException e) {
                throw ExceptionUtils.throwAsRuntime(e);
            } catch (ExternalServiceException e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
        this.ipDB.setDiscoveredNodes(this.discoveredNodes);
        this.policyViolations.addAll(this.ipDB.checkViolation());
        this.policy.setCalculator(this);
    }

    private void prepareInvnetoryIp(NodeDto node) throws RemoteException {
        for (IpIfDto ip : this.facade.selectNodeElements(node, IpIfDto.class, SearchMethod.REGEXP, MPLSNMS_ATTR.IP_ADDRESS, ".*")) {
            if (ip.getOwner() instanceof VrfIfDto) {
                continue;
            }
            String vpnPrefix = DtoUtil.getStringOrNull(ip, ATTR.VPN_PREFIX);
            String ipAddr = DtoUtil.getStringOrNull(ip, MPLSNMS_ATTR.IP_ADDRESS);
            String maskLengthValue = DtoUtil.getStringOrNull(ip, MPLSNMS_ATTR.MASK_LENGTH);
            if (ipAddr == null || maskLengthValue == null) {
                log.debug("empty ip-address/mask found: " + ip.getAbsoluteName());
                continue;
            }
            Integer maskLength = Integer.valueOf(maskLengthValue);
            IpAddressHolder holder = this.ipDB.getIpAddress(vpnPrefix, ipAddr);
            if (holder == null) {
                holder = new IpAddressHolder(vpnPrefix, ipAddr);
                holder.setMaskLength(maskLength);
                this.ipDB.addIpAddress(holder);
            }
            holder.addIpPort(ip);
        }
    }

    private void prepareNetworkIp(Device device) {
        for (Map.Entry<CidrAddress, Port> entry : device.getIpAddressesWithMask().entrySet()) {
            Port port = entry.getValue();
            CidrAddress cidr = entry.getKey();
            String vpnPrefix = this.config.getVpnDriver().getVpnPrefix(port);
            String ipAddr = cidr.getAddress().getHostAddress();
            int maskLength = cidr.getSubnetMaskLength();
            IpAddressHolder holder = this.ipDB.getIpAddress(vpnPrefix, ipAddr);
            if (holder == null) {
                holder = new IpAddressHolder(vpnPrefix, ipAddr);
                holder.setMaskLength(Integer.valueOf(maskLength));
                this.ipDB.addIpAddress(holder);
            }
            holder.addNetworkPort(entry.getValue());
        }
    }

    private void prepareDeviceName(Device device, NodeDto node) {
        if (!device.getDeviceName().equals(node.getName())) {
            log.warn("device name isn't same as inventory. DB[" + node.getName()
                    + "]->NW[" + device.getDeviceName() + "]");
            log.warn("change device-name to [" + node.getName() + "]");
            device.setDeviceName(node.getName());
        }
    }

    private void processErrorDevice(NodeDto node, ErrorDummyDevice device) throws ExternalServiceException {
        try {
            Map<String, NodeElementDto> wholeNodeElements = new HashMap<String, NodeElementDto>();
            NodeElementDiffUnitBuilder.prepareNodeElements(node, wholeNodeElements);
            DiffUnit unit = NetworkDiffUtil.createDummyDiffUnit(node, device);
            set.addDiffUnit(unit);
            log.warn("discovery failed: node=" + node.getName());
            PolicyViolation violation = new PolicyViolation(
                    InventoryIdCalculator.getId(node),
                    node.getName(),
                    DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.MANAGEMENT_IP),
                    "Discovery failed (Node not respond)",
                    device.getMessage());
            policyViolations.add(violation);
        } catch (IOException e) {
            log.error("diff-unit build failed: node=" + node.getName(), e);
        } catch (InventoryException e) {
            log.error("diff-unit build failed: node=" + node.getName(), e);
        } catch (DuplicationException e) {
            log.error("duplicated diff-unit: node=" + node.getName(), e);
        }
    }

    private void calculateNodeDiff() throws IOException, InventoryException, ExternalServiceException {
        NodeDiffUnitBuilder builder = new NodeDiffUnitBuilder(this.set, this.policy, this.ipDB,
                this.portToAbsoluteNameMap, false, this.userName);
        Set<NodeDto> nodes = new HashSet<NodeDto>(results.keySet());
        List<Device> devices = new ArrayList<Device>();
        for (NodeDto node : nodes) {
            Device device = results.get(node);
            if (device instanceof ErrorDummyDevice) {
                continue;
            } else if (device instanceof VMwareServer) {
                continue;
            }
            devices.add(device);
        }
        builder.buildDiffUnits(devices, nodes);
    }

    private void calculatePhysicalLinkDiff() throws IOException, InventoryException, ExternalServiceException {
        if (IpAddressModel.P2P != this.policy.getIpAddressModel()) {
            return;
        }
        List<PolicyViolation> linkEvents = new ArrayList<PolicyViolation>();
        Map<String, IpSubnetDto> dbMap = IpLinkDiffUnitBuilder.getInventorySubnets(this.policy, this.discoveredNodes, linkEvents);
        Map<String, IpSubnet> networkMap = IpLinkDiffUnitBuilder.getNetworkSubnets(this.ipDB);
        DiffUnitBuilder<IpSubnet, IpSubnetDto> builder = new IpLinkDiffUnitBuilder(this.set, this.userName, this.ipDB);
        builder.buildDiffUnits(networkMap, dbMap);
    }

    private void calculateIpSubnetDiff() throws InventoryException, ExternalServiceException, IOException {
        if (IpAddressModel.SUBNET != this.policy.getIpAddressModel()) {
            return;
        }
        List<PolicyViolation> linkEvents = new ArrayList<PolicyViolation>();
        Map<String, IpSubnetDto> dbMap = IpSubnetDiffUnitBuilder.getInventorySubnets(this.policy, this.discoveredNodes, linkEvents);
        Map<String, IpSubnet> networkMap = IpSubnetDiffUnitBuilder.getNetworkSubnets(this.ipDB);
        DiffUnitBuilder<IpSubnet, IpSubnetDto> builder = new IpSubnetDiffUnitBuilder(this.set, this.userName, this.ipDB);
        builder.buildDiffUnits(networkMap, dbMap);
    }

    private void calculateLspDiff() throws IOException, InventoryException, ExternalServiceException {
        String eventMatter = "LSP policy violation found (diff).";
        List<RsvpLspDto> lsps1 = RsvpLspUtil.getRsvpLsps();
        Map<String, RsvpLspDto> dbLsps = new HashMap<String, RsvpLspDto>();
        for (RsvpLspDto lsp : lsps1) {
            String id = null;
            try {
                id = InventoryIdCalculator.getId(lsp);
            } catch (Exception e) {
                log.error("unexpected exception on " + lsp.getName(), e);
                continue;
            }
            if (lsp.getHopSeries1() == null) {
                log.warn("no primary-hop-series: " + lsp.getName());
                PolicyViolation violation = new PolicyViolation(id, "unknown", "0.0.0.0", eventMatter, "no main path. (Inventory)");
                policyViolations.add(violation);
                continue;
            }
            dbLsps.put(id, lsp);
        }

        List<MplsTunnel> lsps2 = ConfigModelUtil.getWholeRsvpLsps(this.results.values());
        Map<String, MplsTunnel> networkLsps = new HashMap<String, MplsTunnel>();
        Set<String> deviceNames = new HashSet<String>();
        for (MplsTunnel lsp : lsps2) {
            if (!policy.isEdgeAwareDevice(lsp.getDevice())) {
                String deviceName = lsp.getDevice().getDeviceName();
                if (!deviceNames.contains(deviceName)) {
                    String msg = "regulation error: found LSP from [" + deviceName + "] that is not edge device.";
                    log.warn(msg);
                    String deviceID = InventoryIdCalculator.getId(lsp.getDevice());
                    createLspPolicyViolation(deviceID, lsp, eventMatter, msg);
                    deviceNames.add(lsp.getDevice().getDeviceName());
                }
                continue;
            }
            List<String> errors = new ArrayList<String>();
            String id = InventoryIdCalculator.getId(lsp);
            errors.addAll(policy.getLspRegulationErrors(lsp));
            if (errors.size() == 0) {
                for (LabelSwitchedPathEndPoint path : lsp.getMemberLsps().values()) {
                    errors.addAll(policy.getPathRegulationErrors(path));
                }
            }
            if (errors.size() > 0) {
                for (String error : errors) {
                    log.warn("regulation error[" + id + "]: " + error);
                    createLspPolicyViolation(id, lsp, eventMatter, error);
                }
                continue;
            } else {
                log.debug("finished without error[" + id + "]");
            }
            networkLsps.put(id, lsp);
        }
        LspDiffUnitBuilder builder = new LspDiffUnitBuilder(this.set, this.policy, this.ipDB, userName);
        builder.buildDiffUnits(networkLsps, dbLsps);
    }

    private void createLspPolicyViolation(String id, MplsTunnel lsp, String eventMatter, String msg) {
        PolicyViolation violation = new PolicyViolation(
                id,
                lsp.getDevice().getDeviceName(),
                lsp.getDevice().getIpAddress(),
                eventMatter, msg);
        policyViolations.add(violation);
    }

    private void calculatePseudoWireDiff() throws InventoryException, IOException, ExternalServiceException {
        String eventMatter = "PseudoWire policy violation found (diff).";
        Set<PseudowireDto> pseudoWires1 = PseudoWireUtil.getPseudoWires();
        Map<String, PseudowireDto> dbPseudoWires = new HashMap<String, PseudowireDto>();
        for (PseudowireDto pw : pseudoWires1) {
            String id = InventoryIdCalculator.getId(pw);
            dbPseudoWires.put(id, pw);
        }

        List<PseudoWirePort> pseudoWires2 = ConfigModelUtil.getWholePseudoWires(this.results.values());
        Map<String, NetworkPseudoWire> networkPseudoWires = PseudoWireDiffUnitBuilder.getNetworkPseudoWires(pseudoWires2);
        Map<String, NetworkPseudoWire> networkPseudoWires2 = new HashMap<String, NetworkPseudoWire>();
        for (Map.Entry<String, NetworkPseudoWire> entry : networkPseudoWires.entrySet()) {
            List<String> errors = policy.getPseudoWireRegulationErrors(entry.getValue());
            if (errors.size() > 0) {
                for (String error : errors) {
                    log.warn("regulation error[" + entry.getKey() + "]: " + error);
                    NetworkPseudoWire pw = entry.getValue();
                    if (pw.getAc1() != null) {
                        Device d1 = pw.getAc1().getDevice();
                        String id1 = InventoryIdBuilder.getPseudoWireID(d1.getDeviceName(), pw.getID());
                        PolicyViolation violation = new PolicyViolation(id1,
                                d1.getDeviceName(), d1.getIpAddress(), eventMatter, error);
                        policyViolations.add(violation);
                    }
                    if (pw.getAc2() != null) {
                        Device d2 = pw.getAc2().getDevice();
                        String id2 = InventoryIdBuilder.getPseudoWireID(d2.getDeviceName(), pw.getID());
                        PolicyViolation violation = new PolicyViolation(id2,
                                d2.getDeviceName(), d2.getIpAddress(), eventMatter, error);
                        policyViolations.add(violation);
                    }
                }
                continue;
            }
            networkPseudoWires2.put(entry.getKey(), entry.getValue());
        }
        PseudoWireDiffUnitBuilder builder = new PseudoWireDiffUnitBuilder(this.set,
                this.portToAbsoluteNameMap, this.policy, this.userName);
        builder.buildDiffUnits(networkPseudoWires2, dbPseudoWires);
    }

    private void calculatePseudoWireStringTypeDiff() throws InventoryException, ExternalServiceException, IOException {
        String eventMatter = "PseudoWire policy violation found (diff).";
        Set<PseudowireDto> pseudoWires1 = PseudoWireUtil.getPseudoWires();
        Map<String, PseudowireDto> dbPseudoWires = new HashMap<String, PseudowireDto>();
        for (PseudowireDto pw : pseudoWires1) {
            String id = InventoryIdCalculator.getId(pw);
            dbPseudoWires.put(id, pw);
        }

        List<PseudoWirePort> pseudoWires2 = ConfigModelUtil.getWholePseudoWires(this.results.values());
        Map<String, NetworkPseudoWire> networkPseudoWires = PseudoWireDiffUnitBuilder.getNetworkPseudoWires(pseudoWires2);
        Map<String, NetworkPseudoWire> networkPseudoWires2 = new HashMap<String, NetworkPseudoWire>();
        for (Map.Entry<String, NetworkPseudoWire> entry : networkPseudoWires.entrySet()) {
            List<String> errors = policy.getPseudoWireRegulationErrors(entry.getValue());
            if (errors.size() > 0) {
                for (String error : errors) {
                    log.warn("regulation error[" + entry.getKey() + "]: " + error);
                    NetworkPseudoWire pw = entry.getValue();
                    if (pw.getAc1() != null) {
                        Device d1 = pw.getAc1().getDevice();
                        String id1 = InventoryIdBuilder.getPseudoWireID(d1.getDeviceName(), pw.getID());
                        PolicyViolation violation = new PolicyViolation(id1,
                                d1.getDeviceName(), d1.getIpAddress(), eventMatter, error);
                        policyViolations.add(violation);
                    }
                    if (pw.getAc2() != null) {
                        Device d2 = pw.getAc2().getDevice();
                        String id2 = InventoryIdBuilder.getPseudoWireID(d2.getDeviceName(), pw.getID());
                        PolicyViolation violation = new PolicyViolation(id2,
                                d2.getDeviceName(), d2.getIpAddress(), eventMatter, error);
                        policyViolations.add(violation);
                    }
                }
                continue;
            }
            networkPseudoWires2.put(entry.getKey(), entry.getValue());
        }
        PseudoWireStringTypeDiffUnitBuilder builder = new PseudoWireStringTypeDiffUnitBuilder(this.set,
                this.portToAbsoluteNameMap, this.policy, this.userName);
        builder.buildDiffUnits(networkPseudoWires2, dbPseudoWires);
    }

    private void calculateNodePipeDiff() throws InventoryException, ExternalServiceException, IOException {
        String eventMatter = "NodePipe policy violation found (diff).";
        Map<String, InterconnectionIfDto> dbPipes = new HashMap<String, InterconnectionIfDto>();
        Map<String, NodePipe<?>> nwPipes = new HashMap<String, NodePipe<?>>();
        NaefDtoFacade facade = this.conn.getDtoFacade();
        Map<NodeDto, Set<InterconnectionIfDto>> map = facade.getNodeElementsMap(this.discoveredNodes, InterconnectionIfDto.class);
        for (Map.Entry<NodeDto, Set<InterconnectionIfDto>> entry : map.entrySet()) {
            NodeDto node = entry.getKey();
            Set<InterconnectionIfDto> pipes = entry.getValue();
            if (pipes == null) {
                continue;
            }
            for (InterconnectionIfDto pipe : pipes) {
                String id = InventoryIdCalculator.getId(pipe);
                dbPipes.put(id, pipe);
            }
            Device device = this.results.get(node);
            for (NodePipe<?> pipe : device.selectPorts(NodePipe.class)) {
                String id = InventoryIdCalculator.getId(pipe);
                int foundAc = 0;
                if (pipe.getAttachmentCircuit1() != null) {
                    foundAc++;
                }
                if (pipe.getAttachmentCircuit2() != null) {
                    foundAc++;
                }
                if (foundAc != 2) {
                    createPipePolicyViolation(id, pipe, eventMatter,
                            "broken pipe: number of attachment port must be 2, but " + foundAc);
                    continue;
                }
                nwPipes.put(id, pipe);
            }
        }
        NodePipeDiffUnitBuilder builder = new NodePipeDiffUnitBuilder(this.set, this.portToAbsoluteNameMap,
                this.policy, this.userName);
        builder.buildDiffUnits(nwPipes, dbPipes);
    }

    private void createPipePolicyViolation(String id, NodePipe<?> pipe, String eventMatter, String msg) {
        PolicyViolation violation = new PolicyViolation(
                id,
                pipe.getDevice().getDeviceName(),
                pipe.getDevice().getIpAddress(),
                eventMatter, msg);
        policyViolations.add(violation);
    }

    private void calculateVlanIdDiff() throws InventoryException, ExternalServiceException, IOException {
        if (this.config.getDiffPolicy().getDefaultVlanPoolName() == null) {
            return;
        }
        Map<String, VlanDto> dbVlans = VlanIdDiffUnitBuilder.getDbVlans();
        Map<String, VlanIf> networkVplsIfs = VlanIdDiffUnitBuilder.getNetworkVlanIfs(this.results.values());
        VlanIdDiffUnitBuilder builder = new VlanIdDiffUnitBuilder(this.set, this.policy, this.userName);
        builder.buildDiffUnits(networkVplsIfs, dbVlans);
    }

    private void calculateVplsDiff() throws InventoryException, ExternalServiceException, IOException {
        Map<String, VplsIfDto> dbVplsIfs = VplsDiffUnitBuilder.getDbVplsIfs(conn.getActiveNodes());
        Map<String, VplsInstance> networkVplsIfs = VplsDiffUnitBuilder.getNetworkVplsIfs(this.results.values());
        VplsDiffUnitBuilder builder = new VplsDiffUnitBuilder(this.set, this.policy, this.userName);
        builder.buildDiffUnits(networkVplsIfs, dbVplsIfs);
    }

    private void calculateVrfDiff() throws InventoryException, ExternalServiceException, IOException {
        Map<String, VrfIfDto> dbVrfIfs = VrfDiffUnitBuilder.getDbVrfIfs(conn.getActiveNodes());
        Map<String, VrfInstance> networkVrfIfs = VrfDiffUnitBuilder.getNetworkVrfIfs(this.results.values());
        VrfDiffUnitBuilder builder = new VrfDiffUnitBuilder(this.set, this.policy, this.userName);
        builder.buildDiffUnits(networkVrfIfs, dbVrfIfs);
    }

    private void calculateVirtualNodeDiff() throws IOException, InventoryException, ExternalServiceException {
        List<NodeDto> virtualNodes = NodeUtil.getVirtualNodes();
        VirtualNodeDiffUnitBuilder builder = new VirtualNodeDiffUnitBuilder(this.set, this.policy, this.ipDB,
                this.portToAbsoluteNameMap, this.userName);
        builder.buildDiffUnits(virtualDevices, virtualNodes);

        VirtualLinkDiffUnitBuilder builder2 = new VirtualLinkDiffUnitBuilder(this.set, this.userName);
        builder2.buildDiffUnits(virtualDevices, virtualNodes);
    }

    private void createIpAddressComplementTransactionDiffUnit() throws IOException, InventoryException,
            ExternalServiceException {
        DiffUnit complement = new DiffUnit("IP Address Binding", DiffOperationType.UPDATE);
        IpSubnetAddressComplementCommands cmd = new IpSubnetAddressComplementCommands(userName);
        complement.addDiffs(cmd);
        complement.addShellCommands(cmd);
        complement.setStatus(DiffStatus.INITIAL);
        complement.setDepth(DiffConstants.ipIfAndSubnetComplementalDiff);
        complement.setDescription("IP Address clean-up.");
        complement.setSourceSystem(DiffCategory.DISCOVERY.name());
        complement.setTypeName("misc.");
        log.debug("ip-address complement transaction builder added.");
        try {
            this.set.addDiffUnit(complement);
        } catch (Exception e) {
            log.debug("ip-address complement transaction builder duplicated.", e);
        }
    }

    public DiffSet getDiffSet() {
        this.set.sortDiffUnit();
        return this.set;
    }

    public List<PolicyViolation> getIllegalEvents() {
        List<PolicyViolation> result = new ArrayList<PolicyViolation>();
        result.addAll(this.policyViolations);
        return result;
    }
}