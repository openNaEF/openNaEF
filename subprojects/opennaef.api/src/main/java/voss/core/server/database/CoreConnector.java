package voss.core.server.database;

import naef.dto.*;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import naef.dto.mpls.PseudowireLongIdPoolDto;
import naef.dto.mpls.PseudowireStringIdPoolDto;
import naef.dto.mpls.RsvpLspHopSeriesIdPoolDto;
import naef.dto.mpls.RsvpLspIdPoolDto;
import naef.dto.vlan.VlanIdPoolDto;
import naef.dto.vpls.VplsIntegerIdPoolDto;
import naef.dto.vpls.VplsStringIdPoolDto;
import naef.dto.vrf.VrfIntegerIdPoolDto;
import naef.dto.vrf.VrfStringIdPoolDto;
import naef.ui.NaefDtoFacade;
import naef.ui.NaefDtoFacade.SearchMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.MVO.MvoId;
import tef.TransactionId;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.AttributeType.MvoCollectionType;
import tef.skelton.AttributeType.MvoMapType;
import tef.skelton.AuthenticationException;
import tef.skelton.ResolveException;
import tef.skelton.dto.EntityDto;
import voss.core.server.config.CoreConfiguration;
import voss.core.server.constant.ModelConstant;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.AttributeMeta;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.NodeElementComparator;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

public class CoreConnector {
    private static final Logger log = LoggerFactory.getLogger(CoreConnector.class);
    private static CoreConnector instance = null;

    public synchronized static CoreConnector getInstance() throws IOException {
        if (instance == null) {
            instance = new CoreConnector();
        }
        return instance;
    }

    public synchronized static void renew() {
        instance = null;
    }

    CoreConfiguration config = null;

    protected CoreConnector() throws IOException {
        this.config = CoreConfiguration.getInstance();
    }

    public NaefDtoFacade getDtoFacade() throws IOException, RemoteException, ExternalServiceException {
        return this.config.getBridge().getDtoFacade();
    }

    public <T extends EntityDto> T getMvoDto(String id, Class<T> cls) throws IOException, InventoryException, ExternalServiceException {
        return getMvoDto(id, null, cls);
    }

    public <T extends EntityDto> T getMvoDto(String id, String version, Class<T> cls) throws IOException, InventoryException, ExternalServiceException {
        try {
            CoreConnector conn = CoreConnector.getInstance();
            NaefDtoFacade facade = conn.getDtoFacade();
            MvoId mvoId = facade.toMvoId(id);
            TransactionId.W mvoVersion = null;
            if (version != null) {
                mvoVersion = (TransactionId.W) TransactionId.getInstance(version);
            }
            EntityDto dto = facade.getMvoDto(mvoId, mvoVersion);
            try {
                return cls.cast(dto);
            } catch (ClassCastException e) {
                throw new IllegalStateException("cannot cast: " + dto.getClass().getName() + "->" + cls.getName());
            }
        } catch (AuthenticationException e) {
            throw ExceptionUtils.getExternalServiceException(e);
        } catch (ClassCastException e) {
            throw new InventoryException(e);
        }
    }

    public List<SystemUserDto> getSystemUsers() throws IOException, ExternalServiceException {
        List<SystemUserDto> result = new ArrayList<SystemUserDto>();
        try {
            Set<SystemUserDto> users = getDtoFacade().getRootObjects(SystemUserDto.class);
            if (users != null) {
                result.addAll(users);
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public SystemUserDto getSystemUser(String userName) throws IOException, ExternalServiceException {
        try {
            Set<SystemUserDto> users = getDtoFacade().getRootObjectsByName(
                    SystemUserDto.class, SearchMethod.EXACT_MATCH, userName);
            if (users == null || users.size() == 0) {
                return null;
            } else if (users.size() == 1) {
                return users.iterator().next();
            } else if (users.size() > 1) {
                throw new IllegalStateException("duplicated user-name found: " + userName);
            } else {
                return null;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public CustomerInfoDto getCustomerInfoByName(String name) throws IOException, ExternalServiceException {
        try {
            String absoluteName = ATTR.TYPE_CUSTOMER_INFO + ATTR.NAME_DELIMITER_SECONDARY + name;
            EntityDto dto = getMvoDtoByAbsoluteName(absoluteName);
            if (dto == null) {
                return null;
            }
            if (CustomerInfoDto.class.isInstance(dto)) {
                return CustomerInfoDto.class.cast(dto);
            }
            throw new IllegalStateException("non customer-info object: " + absoluteName);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause != null && ResolveException.class.isInstance(cause)) {
                return null;
            }
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public List<String> getConstants(String constName) throws IOException, RemoteException, ExternalServiceException {
        List<String> result = new ArrayList<String>();
        try {
            List<String> constants = getDtoFacade().getConstants(constName);
            if (constants == null) {
                return Collections.emptyList();
            }
            result.addAll(constants);
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public Set<NodeDto> getNodes() throws ExternalServiceException, RemoteException, IOException {
        try {
            NaefDtoFacade facade = getDtoFacade();
            Set<NodeDto> nodes = facade.getNodes();
            return nodes;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public List<NodeDto> getNodesByPartialName(String partialName) throws ExternalServiceException, RemoteException, IOException {
        try {
            List<NodeDto> result = new ArrayList<NodeDto>();
            NaefDtoFacade facade = getDtoFacade();
            String key = ".*(?i)" + partialName + ".*";
            Set<NodeDto> nodes = facade.getRootObjectsByName(NodeDto.class, SearchMethod.REGEXP, key);
            result.addAll(nodes);
            Collections.sort(result, new NodeElementComparator());
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public NodeDto getNodeDto(String nodeName) throws ExternalServiceException, RemoteException, IOException {
        try {
            NaefDtoFacade facade = getDtoFacade();
            Set<NodeDto> nodes = facade.getRootObjectsByName(NodeDto.class, SearchMethod.EXACT_MATCH, nodeName);
            for (NodeDto node : nodes) {
                if (node.getName().equals(nodeName)) {
                    return node;
                }
            }
            return null;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public List<RsvpLspIdPoolDto> getRsvpLspIdPool() throws ExternalServiceException, RemoteException, IOException {
        try {
            NaefDtoFacade facade = getDtoFacade();
            Set<RsvpLspIdPoolDto> roots = facade.getRootIdPools(RsvpLspIdPoolDto.class);
            if (roots == null) {
                throw new IllegalStateException("not initializd. no rsvp-lsp id pool exist.");
            }
            List<RsvpLspIdPoolDto> pools = new ArrayList<RsvpLspIdPoolDto>(roots);
            return pools;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public RsvpLspIdPoolDto getRsvpLspIdPool(String poolId) throws ExternalServiceException, RemoteException, IOException {
        try {
            NaefDtoFacade facade = getDtoFacade();
            Set<RsvpLspIdPoolDto> roots = facade.getRootIdPools(RsvpLspIdPoolDto.class);
            if (roots == null) {
                throw new IllegalStateException("not initializd. no rsvp-lsp id pool exist.");
            }
            List<RsvpLspIdPoolDto> pools = new ArrayList<RsvpLspIdPoolDto>(roots);
            return getRsvpLspIdPoolDto(pools, poolId);
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    private RsvpLspIdPoolDto getRsvpLspIdPoolDto(Collection<RsvpLspIdPoolDto> pools, String poolId) {
        if (pools == null || pools.size() == 0) {
            return null;
        }
        for (RsvpLspIdPoolDto pool : pools) {
            log.debug("mvoID=" + DtoUtil.getMvoId(pool).toString() + ", name=" + pool.getName());
            if (DtoUtil.getMvoId(pool).toString().equals(poolId)) {
                return pool;
            } else if (pool.getName().equals(poolId)) {
                return pool;
            } else {
                RsvpLspIdPoolDto subPool = getRsvpLspIdPoolDto(pool.getChildren(), poolId);
                if (subPool != null) {
                    return subPool;
                }
            }
        }
        return null;
    }

    public List<RsvpLspHopSeriesIdPoolDto> getRsvpLspHopSeriesIdPool() throws ExternalServiceException, RemoteException, IOException {
        try {
            NaefDtoFacade facade = getDtoFacade();
            Set<RsvpLspHopSeriesIdPoolDto> roots = facade.getRootIdPools(RsvpLspHopSeriesIdPoolDto.class);
            if (roots == null) {
                throw new IllegalStateException("not initializd. no rsvp-lsp id pool exist.");
            }
            List<RsvpLspHopSeriesIdPoolDto> pools = new ArrayList<RsvpLspHopSeriesIdPoolDto>(roots);
            return pools;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public RsvpLspHopSeriesIdPoolDto getRsvpLspHopSeriesIdPool(String poolId) throws ExternalServiceException, RemoteException, IOException {
        try {
            NaefDtoFacade facade = getDtoFacade();
            Set<RsvpLspHopSeriesIdPoolDto> roots = facade.getRootIdPools(RsvpLspHopSeriesIdPoolDto.class);
            if (roots == null) {
                throw new IllegalStateException("not initializd. no rsvp-lsp id pool exist.");
            }
            List<RsvpLspHopSeriesIdPoolDto> pools = new ArrayList<RsvpLspHopSeriesIdPoolDto>(roots);
            return getRsvpLspHopSeriesIdPoolDto(pools, poolId);
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    private RsvpLspHopSeriesIdPoolDto getRsvpLspHopSeriesIdPoolDto(Collection<RsvpLspHopSeriesIdPoolDto> pools, String poolId) {
        if (pools == null || pools.size() == 0) {
            return null;
        }
        for (RsvpLspHopSeriesIdPoolDto pool : pools) {
            log.debug("mvoID=" + DtoUtil.getMvoId(pool).toString() + ", name=" + pool.getName());
            if (DtoUtil.getMvoId(pool).toString().equals(poolId)) {
                return pool;
            } else if (pool.getName().equals(poolId)) {
                return pool;
            } else {
                RsvpLspHopSeriesIdPoolDto subPool = getRsvpLspHopSeriesIdPoolDto(pool.getChildren(), poolId);
                if (subPool != null) {
                    return subPool;
                }
            }
        }
        return null;
    }

    public List<PseudowireLongIdPoolDto> getPseudoWireLongIdPools() throws ExternalServiceException, RemoteException,
            IOException {
        try {
            PseudowireLongIdPoolDto nationWide = getPseudoWireLongIdPool("whole-pseudowires");
            if (nationWide == null) {
                return null;
            }
            List<PseudowireLongIdPoolDto> result = new ArrayList<PseudowireLongIdPoolDto>(nationWide.getChildren());
            Collections.sort(result, new Comparator<PseudowireLongIdPoolDto>() {
                @Override
                public int compare(PseudowireLongIdPoolDto o1, PseudowireLongIdPoolDto o2) {
                    return o1.getConcatenatedIdRangesStr().compareTo(o2.getConcatenatedIdRangesStr());
                }
            });
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public List<PseudowireStringIdPoolDto> getPseudoWireStringIdPools() throws ExternalServiceException, RemoteException,
            IOException {
        try {
            PseudowireStringIdPoolDto nationWide = getPseudoWireStringIdPool("whole-pseudowires");
            if (nationWide == null) {
                return null;
            }
            List<PseudowireStringIdPoolDto> result = new ArrayList<PseudowireStringIdPoolDto>(nationWide.getChildren());
            Collections.sort(result, new Comparator<PseudowireStringIdPoolDto>() {
                @Override
                public int compare(PseudowireStringIdPoolDto o1, PseudowireStringIdPoolDto o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public PseudowireStringIdPoolDto getPseudoWireStringIdPool(String poolId) throws ExternalServiceException, RemoteException, IOException {
        try {
            NaefDtoFacade facade = getDtoFacade();
            Set<PseudowireStringIdPoolDto> roots = facade.getRootIdPools(PseudowireStringIdPoolDto.class);
            if (roots == null) {
                throw new IllegalStateException("no root pseudowire.id-pool present.");
            }
            List<PseudowireStringIdPoolDto> pools = new ArrayList<PseudowireStringIdPoolDto>(roots);
            return getPseudowireStringIdPoolDto(pools, poolId);
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public PseudowireLongIdPoolDto getPseudoWireLongIdPool(String poolId) throws ExternalServiceException, RemoteException, IOException {
        try {
            NaefDtoFacade facade = getDtoFacade();
            Set<PseudowireLongIdPoolDto> roots = facade.getRootIdPools(PseudowireLongIdPoolDto.class);
            if (roots == null) {
                throw new IllegalStateException("no root pseudowire.id-pool present.");
            }
            List<PseudowireLongIdPoolDto> pools = new ArrayList<PseudowireLongIdPoolDto>(roots);
            return getPseudowireLongIdPoolDto(pools, poolId);
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    private PseudowireStringIdPoolDto getPseudowireStringIdPoolDto(Collection<PseudowireStringIdPoolDto> pools, String poolId) {
        if (pools == null || pools.size() == 0) {
            return null;
        }
        for (PseudowireStringIdPoolDto pool : pools) {
            if (DtoUtil.getMvoId(pool).toString().equals(poolId)) {
                return pool;
            } else if (pool.getName().equals(poolId)) {
                return pool;
            } else {
                PseudowireStringIdPoolDto subPool = getPseudowireStringIdPoolDto(pool.getChildren(), poolId);
                if (subPool != null) {
                    return subPool;
                }
            }
        }
        return null;
    }

    private PseudowireLongIdPoolDto getPseudowireLongIdPoolDto(Collection<PseudowireLongIdPoolDto> pools, String poolId) {
        if (pools == null || pools.size() == 0) {
            return null;
        }
        for (PseudowireLongIdPoolDto pool : pools) {
            if (DtoUtil.getMvoId(pool).toString().equals(poolId)) {
                return pool;
            } else if (pool.getName().equals(poolId)) {
                return pool;
            } else {
                PseudowireLongIdPoolDto subPool = getPseudowireLongIdPoolDto(pool.getChildren(), poolId);
                if (subPool != null) {
                    return subPool;
                }
            }
        }
        return null;
    }

    public List<InterconnectionIfDto> getNodePipeDtos() throws IOException, ExternalServiceException {
        List<InterconnectionIfDto> results = new ArrayList<InterconnectionIfDto>();
        NaefDtoFacade facade = getDtoFacade();
        Map<NodeDto, Set<InterconnectionIfDto>> map = facade.getNodeElementsMap(getNodes(), InterconnectionIfDto.class);
        for (Map.Entry<NodeDto, Set<InterconnectionIfDto>> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            } else if (entry.getValue().size() == 0) {
                continue;
            }
            results.addAll(entry.getValue());
        }
        return results;
    }

    public Set<LocationDto> getAllLocationDtos() throws ExternalServiceException {
        try {
            return getDtoFacade().getLocations();
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public Set<String> getSupportedAttributeNames(EntityDto dto)
            throws ExternalServiceException, RemoteException, IOException {
        if (dto == null) {
            throw new IllegalArgumentException();
        }
        @SuppressWarnings("unchecked")
        Class<EntityDto> cls = (Class<EntityDto>) dto.getClass();
        return getSupportedAttributeNames(cls);
    }

    public Set<String> getSupportedAttributeNames(Class<? extends EntityDto> cls)
            throws ExternalServiceException, RemoteException, IOException {
        Set<String> result = new HashSet<String>();
        if (cls == null) {
            throw new IllegalArgumentException();
        }
        NaefDtoFacade facade = getDtoFacade();
        for (Attribute<?, ?> attr : facade.getDeclaredAttributes(cls)) {
            String name = attr.getName();
            if (config.getAttributePolicy().isExcludeAttr(name)) {
                continue;
            }
            result.add(attr.getName());
        }
        return result;
    }

    public List<AttributeMeta> getSupportedAttributeMeta(Class<? extends EntityDto> cls)
            throws ExternalServiceException, RemoteException, IOException {
        List<AttributeMeta> result = new ArrayList<AttributeMeta>();
        if (cls == null) {
            throw new IllegalArgumentException("no entity-dto class.");
        }
        NaefDtoFacade facade = getDtoFacade();
        for (Attribute<?, ?> attr : facade.getDeclaredAttributes(cls)) {
            String name = attr.getName();
            if (config.getAttributePolicy().isExcludeAttr(name)) {
                continue;
            }
            AttributeType<?> t = attr.getType();
            AttributeMeta meta;
            if (Attribute.SetAttr.class.isInstance(attr)) {
                MvoCollectionType<?, ?> collectionAttr = MvoCollectionType.class.cast(t);
                Class<?> elementType = collectionAttr.getCollectionType();
                meta = new AttributeMeta(name, Set.class, elementType);
            } else if (Attribute.ListAttr.class.isInstance(attr)) {
                MvoCollectionType<?, ?> collectionAttr = MvoCollectionType.class.cast(t);
                Class<?> elementType = collectionAttr.getCollectionType();
                meta = new AttributeMeta(name, List.class, elementType);
            } else if (Attribute.MapAttr.class.isInstance(attr)) {
                MvoMapType<?, ?> mapAttr = MvoMapType.class.cast(t);
                Class<?> keyType = mapAttr.getKeyType();
                Class<?> valueType = mapAttr.getValueType();
                meta = new AttributeMeta(name, Map.class, keyType, valueType);
            } else {
                Class<?> type = t.getJavaType();
                if (type == null) {
                    meta = new AttributeMeta(name, type);
                } else {
                    meta = new AttributeMeta(name, Object.class);
                }
            }
            result.add(meta);
        }
        return result;
    }

    public Class<?> getElementType(Class<? extends EntityDto> cls, String attrName)
            throws ExternalServiceException, RemoteException, IOException {
        if (cls == null) {
            throw new IllegalArgumentException("no entity-dto class.");
        } else if (attrName == null) {
            throw new IllegalArgumentException("no attribute name.");
        }
        NaefDtoFacade facade = getDtoFacade();
        for (Attribute<?, ?> attr : facade.getDeclaredAttributes(cls)) {
            String name = attr.getName();
            if (!attrName.equals(name)) {
                continue;
            }
            if (config.getAttributePolicy().isExcludeAttr(name)) {
                continue;
            }
            AttributeType<?> t = attr.getType();
            if (!MvoCollectionType.class.isInstance(t)) {
                continue;
            }
            MvoCollectionType<?, ?> _type = MvoCollectionType.class.cast(t);
            if (_type == null) {
                continue;
            }
            return _type.getCollectionType();
        }
        return null;
    }

    public IpSubnetNamespaceDto getActiveRootIpSubnetNamespace() throws IOException, ExternalServiceException {
        List<IpSubnetNamespaceDto> subnets = getActiveRootIpSubnetNamespaces();
        if (subnets.size() != 1) {
            throw new IllegalStateException("Unexpected number of root ip-subnet-namespace: " + subnets.size());
        }
        return subnets.iterator().next();
    }

    public List<IpSubnetNamespaceDto> getActiveRootIpSubnetNamespaces() throws IOException, ExternalServiceException {
        NaefDtoFacade facade = getDtoFacade();
        Set<IpSubnetNamespaceDto> pools = facade.getRootIdPools(IpSubnetNamespaceDto.class);
        if (pools == null) {
            throw new IllegalStateException("ipsubnet-namespace is not set up.");
        }
        List<IpSubnetNamespaceDto> result = new ArrayList<IpSubnetNamespaceDto>();
        for (IpSubnetNamespaceDto pool : pools) {
            if (pool.getName().contains(ATTR.DELETED)) {
                continue;
            }
            result.add(pool);
        }
        return result;
    }

    public IpSubnetNamespaceDto getActiveRootIpSubnetNamespace(String name) throws IOException, ExternalServiceException {
        if (name == null) {
            return null;
        }
        List<IpSubnetNamespaceDto> subnets = getActiveRootIpSubnetNamespaces();
        for (IpSubnetNamespaceDto pool : subnets) {
            if (pool.getName().equals(name)) {
                return pool;
            }
        }
        return null;
    }

    public IpSubnetNamespaceDto getActiveRootIpSubnetNamespaceByVpn(String vpnPrefix) throws IOException, ExternalServiceException {
        List<IpSubnetNamespaceDto> subnets = getActiveRootIpSubnetNamespaces();
        for (IpSubnetNamespaceDto subnet : subnets) {
            if (subnet.getName().equals(ModelConstant.IP_SUBNET_NAMESPACE_TRASH_NAME)) {
                continue;
            }
            String vpn = DtoUtil.getStringOrNull(subnet, ATTR.VPN_PREFIX);
            if (vpn == null && vpnPrefix == null) {
                return subnet;
            }
            if (vpn != null && vpn.equals(vpnPrefix)) {
                return subnet;
            }
        }
        return null;
    }

    public VlanIdPoolDto getVlanPool(String poolId) throws ExternalServiceException {
        try {
            NaefDtoFacade facade = getDtoFacade();
            Set<VlanIdPoolDto> rootPools = facade.getRootIdPools(VlanIdPoolDto.class);
            VlanIdPoolDto result = getPoolByName(rootPools, poolId);
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    protected <T extends IdPoolDto<?, ?, ?>> T getPoolByName(Collection<T> pools, String name) {
        for (T pool : pools) {
            if (pool.getName().equals(name)) {
                return pool;
            }
            List<T> subPools = toList(pool.getChildren());
            T result = getPoolByName(subPools, name);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends IdPoolDto<?, ?, ?>> List<T> toList(Collection<?> list) {
        List<T> subPools = new ArrayList<T>();
        for (Object child : list) {
            if (IdPoolDto.class.isInstance(child)) {
                subPools.add((T) child);
            }
        }
        return subPools;
    }

    public VplsStringIdPoolDto getVplsStringIpPool(String poolName) throws IOException, ExternalServiceException {
        NaefDtoFacade facade = getDtoFacade();
        Set<VplsStringIdPoolDto> pools = facade.getRootIdPools(VplsStringIdPoolDto.class);
        if (pools.size() == 0) {
            throw new IllegalStateException();
        }
        for (VplsStringIdPoolDto pool : pools) {
            if (pool.getName() == null && poolName == null) {
                return pool;
            } else if (pool.getName() != null && poolName != null && poolName.equals(pool.getName())) {
                return pool;
            }
        }
        return null;
    }

    public VplsIntegerIdPoolDto getVplsIntegerIpPool(String poolName) throws IOException, ExternalServiceException {
        NaefDtoFacade facade = getDtoFacade();
        Set<VplsIntegerIdPoolDto> pools = facade.getRootIdPools(VplsIntegerIdPoolDto.class);
        if (pools.size() == 0) {
            throw new IllegalStateException();
        }
        for (VplsIntegerIdPoolDto pool : pools) {
            if (pool.getName() == null && poolName == null) {
                return pool;
            } else if (pool.getName() != null && poolName != null && poolName.equals(pool.getName())) {
                return pool;
            }
        }
        return null;
    }

    public VrfStringIdPoolDto getVrfStringIpPool(String poolName) throws IOException, ExternalServiceException {
        NaefDtoFacade facade = getDtoFacade();
        Set<VrfStringIdPoolDto> pools = facade.getRootIdPools(VrfStringIdPoolDto.class);
        if (pools.size() == 0) {
            throw new IllegalStateException();
        }
        for (VrfStringIdPoolDto pool : pools) {
            if (pool.getName() == null && poolName == null) {
                return pool;
            } else if (pool.getName() != null && poolName != null && poolName.equals(pool.getName())) {
                return pool;
            }
        }
        return null;
    }

    public VrfIntegerIdPoolDto getVrfIntegerIpPool(String poolName) throws IOException, ExternalServiceException {
        NaefDtoFacade facade = getDtoFacade();
        Set<VrfIntegerIdPoolDto> pools = facade.getRootIdPools(VrfIntegerIdPoolDto.class);
        if (pools.size() == 0) {
            throw new IllegalStateException();
        }
        for (VrfIntegerIdPoolDto pool : pools) {
            if (pool.getName() == null && poolName == null) {
                return pool;
            } else if (pool.getName() != null && poolName != null && poolName.equals(pool.getName())) {
                return pool;
            }
        }
        return null;
    }

    public List<IpSubnetAddressDto> getRootIpSubnetAddresses() throws InventoryException, ExternalServiceException {
        try {
            List<IpSubnetAddressDto> result = new ArrayList<IpSubnetAddressDto>();
            Set<IpSubnetAddressDto> addresses = getDtoFacade().getRootIdPools(IpSubnetAddressDto.class);
            for (IpSubnetAddressDto address : addresses) {
                if (address.getName().contains(ATTR.DELETED)) {
                    continue;
                }
                result.add(address);
            }
            return result;
        } catch (IOException e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public IpSubnetAddressDto getRootIpSubnetAddress(String name) throws InventoryException, ExternalServiceException {
        if (name == null) {
            return null;
        }
        List<IpSubnetAddressDto> rootSubnets = getRootIpSubnetAddresses();
        for (IpSubnetAddressDto subnet : rootSubnets) {
            if (subnet.getName().equals(name)) {
                return subnet;
            }
        }
        return null;
    }

    public IpSubnetAddressDto getRootIpSubnetAddressByVpn(String vpnPrefix) throws InventoryException, ExternalServiceException {
        List<IpSubnetAddressDto> rootSubnets = getRootIpSubnetAddresses();
        for (IpSubnetAddressDto subnet : rootSubnets) {
            String vpn = DtoUtil.getStringOrNull(subnet, ATTR.VPN_PREFIX);
            if (subnet.getName().equals(ModelConstant.IP_SUBNET_ADDRESS_TRASH_NAME)) {
                continue;
            }
            if (vpn == null && vpnPrefix == null) {
                return subnet;
            }
            if (vpn != null && vpn.equals(vpnPrefix)) {
                return subnet;
            }
        }
        return null;
    }

    public EntityDto getMvoDtoByMvoId(String id) throws IOException, ExternalServiceException {
        if (id == null) {
            return null;
        }
        MvoId mvoID = getDtoFacade().toMvoId(id);
        return getMvoDtoByMvoId(mvoID);
    }

    public EntityDto getMvoDtoByMvoId(MvoId mvoID) throws IOException, ExternalServiceException {
        if (mvoID == null) {
            return null;
        }
        return getDtoFacade().getMvoDto(mvoID, null);
    }

    public EntityDto getMvoDtoByAbsoluteName(String name) throws IOException, InventoryException, ExternalServiceException {
        if (name == null) {
            return null;
        }
        NaefDtoFacade facade = getDtoFacade();
        try {
            MvoId mvoID = facade.resolveMvoId(name);
            if (mvoID == null) {
                return null;
            }
            log.debug("mvo-id: " + mvoID);
            return facade.getMvoDto(mvoID, null);
        } catch (ResolveException e) {
            log.warn("cannot resolve:" + name + " (" + e.getMessage() + ")");
            log.trace("cannot resolve (detail): " + name, e);
            return null;
        }
    }

}