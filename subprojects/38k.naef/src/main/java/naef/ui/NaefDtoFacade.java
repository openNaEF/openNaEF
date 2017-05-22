package naef.ui;

import lib38k.misc.CommandlineParser;
import lib38k.parser.Ast;
import lib38k.parser.ParseException;
import lib38k.parser.StringToken;
import lib38k.parser.TokenStream;
import naef.NaefTefService;
import naef.dto.CustomerInfoDto;
import naef.dto.IdPoolDto;
import naef.dto.InternodeTopology;
import naef.dto.InterportLinkDto;
import naef.dto.JackDto;
import naef.dto.LinkDto;
import naef.dto.LocationDto;
import naef.dto.NaefDtoSetBuilder;
import naef.dto.NaefDtoUtils;
import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.NodeElementDescriptor;
import naef.dto.NodeElementDto;
import naef.dto.NodeElementHierarchy;
import naef.dto.NodeGroupDto;
import naef.dto.PortDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanSegmentDto;
import naef.mvo.AbstractPort;
import naef.mvo.CustomerInfo;
import naef.mvo.Jack;
import naef.mvo.Location;
import naef.mvo.NaefAttributes;
import naef.mvo.NaefMvoUtils;
import naef.mvo.Network;
import naef.mvo.Node;
import naef.mvo.NodeElement;
import naef.mvo.NodeGroup;
import naef.mvo.P2pLink;
import naef.mvo.Port;
import naef.mvo.eth.EthLag;
import naef.mvo.eth.EthLagIf;
import naef.mvo.eth.EthLink;
import naef.mvo.eth.EthPort;
import naef.mvo.ip.IpSubnet;
import naef.mvo.mpls.MplsMvoUtils;
import naef.mvo.mpls.Pseudowire;
import naef.mvo.mpls.RsvpLsp;
import naef.mvo.vlan.Vlan;
import naef.mvo.vlan.VlanSegment;
import tef.MVO;
import tef.MvoRegistry;
import tef.TransactionContext;
import tef.TransactionId;
import tef.TransactionIdAggregator;
import tef.skelton.AbstractHierarchicalModel;
import tef.skelton.AbstractModel;
import tef.skelton.Attribute;
import tef.skelton.AttributeTask;
import tef.skelton.AuthenticationException;
import tef.skelton.Constants;
import tef.skelton.Filter;
import tef.skelton.IdPool;
import tef.skelton.Model;
import tef.skelton.NamedModel;
import tef.skelton.ObjectQueryExpression;
import tef.skelton.ObjectResolver;
import tef.skelton.ResolveException;
import tef.skelton.Resolver;
import tef.skelton.RmiDtoFacade;
import tef.skelton.SkeltonTefService;
import tef.skelton.UniquelyNamedModelHome;
import tef.skelton.UniquelyNamedModelResolver;
import tef.skelton.dto.Dto2Mvo;
import tef.skelton.dto.DtoChangeListener;
import tef.skelton.dto.DtoChanges;
import tef.skelton.dto.DtoChangesBuilder;
import tef.skelton.dto.EntityDto;
import tef.skelton.dto.MvoDtoDesc;
import tef.skelton.dto.MvoDtoMapping;
import tef.skelton.dto.MvoDtoOriginator;
import tef.skelton.dto.TaskSynthesizedDtoBuilder;
import tef.skelton.dto.TxDtoChangeListenerAdapter;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public interface NaefDtoFacade extends RmiDtoFacade {

    public static class SearchCondition implements java.io.Serializable {

        public final SearchMethod method;
        public final String attributeName;
        public final Object value;

        public SearchCondition(SearchMethod method, String attributeName, Object value) {
            this.method = method;
            this.attributeName = attributeName;
            this.value = value;
        }
    }

    public enum SearchMethod {

        EXACT_MATCH {

            @Override boolean matches(Object o1, Object o2) {
                return o1 == null ? o2 == null : o1.equals(o2);
            }
        },

        PARTIAL_MATCH {

            @Override boolean matches(Object o1, Object o2) {
                if (o1 == null && o2 == null) {
                    return true;
                }
                if (o1 == null || o2 == null) {
                    return false;
                }

                String s1 = o1.toString();
                String s2 = o2.toString();
                return s1.contains(s2);
            }
        },

        REGEXP {

            @Override boolean matches(Object o1, Object o2) {
                if (o1 == null || o2 == null) {
                    return false;
                }

                String s1 = o1.toString();
                String s2 = o2.toString();
                return s1.matches(s2);
            }
        };

        abstract boolean matches(Object o1, Object o2);
    }

    public EntityDto[][] query(String objectQueryExpression)
        throws AuthenticationException, RemoteException;

    public <T extends EntityDto> Set<T> getRootObjects(Class<T> type)
        throws AuthenticationException, RemoteException;
    public <T extends EntityDto> Set<T> getRootObjectsByName(Class<T> type, SearchMethod method, String name)
        throws AuthenticationException, RemoteException;
    public <T extends EntityDto> T getUniqueNameObject(Class<T> type, String name)
        throws AuthenticationException, RemoteException;
    public <T extends MVO & NodeElement, U extends EntityDto> List<U> getSubElements(
        NodeElementDto dto, Class<T> mvoClass, Class<U> dtoClass)
        throws AuthenticationException, RemoteException;
    public List<String> getConstantsNames() 
        throws AuthenticationException, RemoteException;
    public List<String> getConstants(String name)
        throws AuthenticationException, RemoteException;
    public Set<LocationDto> getLocations() 
        throws AuthenticationException, RemoteException;
    public Set<NodeDto> getNodes() 
        throws AuthenticationException, RemoteException;
    public List<NodeDto> getNodes(Filter<Node> filter)
        throws AuthenticationException, RemoteException;
    public List<JackDto> getNodeJacks(NodeDto node)
        throws AuthenticationException, RemoteException;
    public List<PortDto> getNodePorts(NodeDto node)
        throws AuthenticationException, RemoteException;
    public List<PortDto> getNodePorts(NodeDto node, Filter<Port> filter)
        throws AuthenticationException, RemoteException;
    public Set<NetworkDto> getPortNetworks(PortDto portDto)
        throws AuthenticationException, RemoteException;
    public <T extends IdPoolDto<?, ?, ?>> Set<T> getRootIdPools(Class<T> type)
        throws AuthenticationException, RemoteException;
    public <T extends IdPoolDto<?, ID, ?>, ID extends Object & Comparable<ID>>
        Set<T> getLeafIdPools(Class<T> type, ID id)
        throws AuthenticationException, RemoteException;
    public <T extends IdPoolDto<?, ID, ?>, ID extends Object & Comparable<ID>>
        T getLeafIdPool(T rootIdPool, ID id)
        throws AuthenticationException, RemoteException;
    public Set<LinkDto> getLinks()
        throws AuthenticationException, RemoteException;
    public Set<LinkDto> getLinks(Class<? extends P2pLink<?>> klass)
        throws AuthenticationException, RemoteException;
    public VlanSegmentDto getVlanLink(VlanDto vlan, PortDto ethport)
        throws AuthenticationException, RemoteException;
    public Set<PseudowireDto> getPseudowires()
        throws AuthenticationException, RemoteException;
    public Set<PseudowireDto> getPseudowires(NodeDto node)
        throws AuthenticationException, RemoteException;
    public Set<RsvpLspDto> getRsvpLsps() 
        throws AuthenticationException, RemoteException;
    public Set<RsvpLspDto> getRsvpLsps(NodeDto node)
        throws AuthenticationException, RemoteException;
    public Set<RsvpLspDto> getRsvpLsps(NodeDto ingressDto, NodeDto egressDto)
        throws AuthenticationException, RemoteException;
    public Set<NodeElementDto> getDescendants(NodeElementDto root, boolean includesRoot)
        throws AuthenticationException, RemoteException;
    public <T extends NodeElementDto> Set<T>selectNodeElements(
        NodeDto node, Class<T> searchTargetClass, SearchMethod method, String attributeName, Object searchValue)
        throws AuthenticationException, RemoteException;
    public InternodeTopology getInternodeTopology(Class<? extends P2pLink> linkType)
        throws AuthenticationException, RemoteException;
    public List<InterportLinkDto> getInterportLinks(NodeDto node1, NodeDto node2, Class<? extends P2pLink> linkType)
        throws AuthenticationException, RemoteException;
    public Set<IpSubnetDto> getIpSubnets()
        throws AuthenticationException, RemoteException;
    public <T extends NodeElementDto> Map<NodeDto, Set<T>> getNodeElementsMap(Set<NodeDto> nodes, Class<T> klass)
        throws AuthenticationException, RemoteException;
    public NodeElementHierarchy buildNodeElementHierarchy(Set<NodeElementDescriptor<?>> mvoids)
        throws AuthenticationException, RemoteException;

    public Set<CustomerInfoDto> getCustomerInfos(Set<PortDto> ports)
        throws AuthenticationException, RemoteException;
    public Set<CustomerInfoDto> selectCustomerInfos(SearchMethod method, String attributeName, Object searchValue)
        throws AuthenticationException, RemoteException;
    public Set<CustomerInfoDto> getUsers(PortDto port)
        throws AuthenticationException, RemoteException;
    public Set<NodeGroupDto> getNodeGroups()
        throws AuthenticationException, RemoteException;
    public <T extends EntityDto> Set<T> selectIdPoolUsers(IdPoolDto<?, ?, T> pool, List<SearchCondition> conditions)
        throws AuthenticationException, RemoteException;

    public static class Impl extends NaefRmiFacade.Impl implements NaefDtoFacade {

        private final DtoChangesBuilder dtoChangesBuilder;

        public Impl() throws RemoteException {
            dtoChangesBuilder = new DtoChangesBuilder(mvodtoOriginator());
        }

        private NaefTefService tefservice() {
            return NaefTefService.instance();
        }

        private MvoRegistry mvoRegistry() {
            return tefservice().getMvoRegistry();
        }

        private MvoDtoMapping mvodtoMapping() {
            return tefservice().getMvoDtoMapping();
        }

        public MVO toMvo(EntityDto dto) {
            return Dto2Mvo.toMvo(dto);
        }

        private MvoDtoOriginator mvodtoOriginator() {
            return new MvoDtoOriginator(this);
        }

        @Override public Class<? extends EntityDto> getDtoClass(MVO.MvoId mvoId) throws AuthenticationException {
            setupReadTransaction();

            return mvodtoMapping().getDtoClass(mvoRegistry().get(mvoId).getClass());
        }

        @Override public List<Attribute<?, ?>> getDeclaredAttributes(Class<? extends EntityDto> dtoClass)
            throws AuthenticationException
        {
            return Attribute.getAttributes(getMvoClass(dtoClass));
        }

        @Override public Attribute<?, ?> getDeclaredAttribute(Class<? extends EntityDto> dtoClass, String attributeName)
            throws AuthenticationException
        {
            return Attribute.getAttribute(
                (Class<? extends Model>) mvodtoMapping().getMvoClass( dtoClass),
                attributeName);
        }

        private Class<? extends Model> getMvoClass(Class<? extends EntityDto> dtoClass) {
            if (dtoClass == null) {
                throw new IllegalArgumentException("dto class is null.");
            }

            Class<? extends Model> result = mvodtoMapping().getMvoClass(dtoClass);
            if (result == null) {
                throw new IllegalStateException("no mvo-class mapping: " + dtoClass.getName());
            }

            return result;
        }

        @Override public List<Attribute<?, ?>> getSerializableAttributes(Class<? extends EntityDto> dtoClass)
            throws AuthenticationException
        {
            if (dtoClass == null) {
                throw new IllegalArgumentException();
            }

            Class<? extends Model> mvoClass = mvodtoMapping().getMvoClass(dtoClass);
            return Attribute.getSerializableAttributes(mvoClass);
        }

        @Override public List<TransactionId.W> getVersions(MVO.MvoId mvoId) throws AuthenticationException {
            setupReadTransaction();

            MVO mvo = mvoRegistry().get(mvoId);
            return mvo == null
                ? null
                : Arrays.asList(TransactionIdAggregator.getTransactionIds(mvo));
        }

        @Override public EntityDto getMvoDto(MVO.MvoId mvoId, TransactionId.W version) throws AuthenticationException {
            setupReadTransaction();

            return buildDto(mvoId, version);
        }

        @Override public <T extends EntityDto> T getMvoDto(EntityDto.Desc<? extends T> desc)
            throws AuthenticationException
        {
            if (desc == null || ! (desc instanceof MvoDtoDesc<?>)) {
                return null;
            }

            setupReadTransaction();

            MvoDtoDesc<?> ref = (MvoDtoDesc<?>) desc;
            return this.<T>buildDto(ref);
        }

        @Override public <T extends EntityDto> List<T> getMvoDtosList(List<? extends EntityDto.Desc<?>> descs)
            throws AuthenticationException
        {
            return buildDtoCollection(new ArrayList<T>(), descs);
        }

        @Override public <T extends EntityDto> Set<T> getMvoDtosSet(Set<? extends EntityDto.Desc<?>> descs) {
            return buildDtoCollection(new HashSet<T>(), descs);
        }

        private <T extends EntityDto, RESULT extends Collection<T>> RESULT
            buildDtoCollection(RESULT result, Collection<? extends EntityDto.Desc<?>> descs)
        {
            if (descs == null) {
                return null;
            }
            if (descs.size() == 0) {
                return result;
            }

            setupReadTransaction();

            for (EntityDto.Desc<?> desc : descs) {
                if (desc instanceof MvoDtoDesc<?>) {
                    MvoDtoDesc<?> ref = (MvoDtoDesc<?>) desc;
                    result.add(ref == null ? null : this.<T>buildDto(ref));
                }
            }
            return result;
        }

        private <T extends EntityDto> T buildDto(MvoDtoDesc<?> desc) {
            return buildDto(desc.getMvoId(), desc.getTimestamp(), desc.getTime());
        }

        private <T extends EntityDto> T buildDto(MVO.MvoId mvoId, TransactionId.W version, long time) {
            long savedTime = TransactionContext.getTargetTime();
            try {
                TransactionContext.setTargetTime(time);

                return buildDto(mvoId, version);
            } finally {
                TransactionContext.setTargetTime(savedTime);
            }
        }

        private <T extends EntityDto> T buildDto(MVO.MvoId mvoId, TransactionId.W version) {
            TransactionId.W savedVersion = TransactionContext.getTargetVersion();
            try {
                if (version != null) {
                    TransactionContext.setTargetVersion(version);
                }

                return this.<T>buildDto(mvoRegistry().get(mvoId));
            } finally {
                TransactionContext.setTargetVersion(savedVersion);
            }
        }

        @Override public Object getAttributeValue(EntityDto.Desc<?> desc, String attrName) {
            setupReadTransaction();

            return tefservice().getMvoDtoFactory().getAttributeValue((MvoDtoDesc<?>) desc, attrName);
        }

        @Override public <T> SortedMap<TransactionId.W, T> getAttributeHistory(MVO.MvoId mvoId, String attributeName)
            throws AuthenticationException
        {
            setupReadTransaction();

            if (mvoId == null) {
                throw new IllegalArgumentException("mvo-id is null.");
            }

            MVO mvo = mvoRegistry().get(mvoId);
            if (mvo == null) {
                throw new IllegalArgumentException("no such mvo.");
            }
            if (! (mvo instanceof Model)) {
                throw new IllegalArgumentException("unsupported object type.");
            }

            SortedMap<TransactionId.W, T> result = new TreeMap<TransactionId.W, T>();
            T lastValue = null;
            for (TransactionId.W txid : TransactionIdAggregator.getTransactionIds(mvo)) {
                T value;
                TransactionId.W savedVersion = TransactionContext.getTargetVersion();
                try {
                    TransactionContext.setTargetVersion(txid);

                    value = (T) ((Model) mvo).getValue(attributeName);
                } finally {
                    TransactionContext.setTargetVersion(savedVersion);
                }

                if (value != null && ! (value instanceof java.io.Serializable)) {
                    throw new IllegalArgumentException(
                        "値が直列化可能でない属性は指定できません: "
                        + SkeltonTefService.instance().uiTypeNames().getName(mvo.getClass())
                        + " " + attributeName);
                }

                if (result.size() == 0
                    || (lastValue == null ? value != null : ! lastValue.equals(value)))
                {
                    result.put(txid, value);
                }

                lastValue = value;
            }
            return result;
        }

        @Override public MVO.MvoId toMvoId(String mvoIdStr) throws AuthenticationException {
            if (mvoIdStr == null) {
                return null;
            }

            setupReadTransaction();

            return MVO.MvoId.getInstanceByLocalId(mvoIdStr);
        }

        @Override public MVO.MvoId resolveMvoId(String name) throws ResolveException, AuthenticationException {
            if (name == null) {
                return null;
            }

            setupReadTransaction();

            AbstractModel model = ObjectResolver.<AbstractModel>resolve(AbstractModel.class, null, null, name);
            return model.getMvoId();
        }

        @Override public void addDtoChangeListener(String listenerName, final DtoChangeListener listener)
            throws AuthenticationException
        {
            new TxDtoChangeListenerAdapter(mvodtoOriginator(), listenerName, listener);
        }

        protected <T extends EntityDto> T buildDto(MVO mvo) {
            return tefservice().getMvoDtoFactory().<T>build(mvodtoOriginator(), mvo);
        }

        protected <T extends EntityDto> Set<T> buildDtos(Collection<?> mvos) {
            Set<T> result = new HashSet<T>();
            for (Object mvo : mvos) {
                result.add(this.<T>buildDto((MVO) mvo));
            }
            return result;
        }

        private UniquelyNamedModelResolver<?> getUniquelyNamedModelResolver(Class<?> mvoType) {
            Resolver<?> resolver = NaefTefService.instance().getResolver(mvoType);
            if (! (resolver instanceof UniquelyNamedModelResolver<?>)) {
                throw new IllegalArgumentException(
                    SkeltonTefService.instance().uiTypeNames().getName(mvoType) + " は一意名オブジェクトではありません.");
            }
            return (UniquelyNamedModelResolver<?>) resolver;
        }

        @Override public EntityDto[][] query(String objectQueryExpression) throws AuthenticationException {
            setupReadTransaction();

            List<ObjectQueryExpression.Row> queryResultRows;
            try {
                TokenStream tokens
                    = StringToken.newTokenStream(Arrays.asList(CommandlineParser.parse(objectQueryExpression)));
                queryResultRows
                    = (List<ObjectQueryExpression.Row>) Ast
                    .parse(ObjectQueryExpression.COLLECTION, tokens)
                    .evaluate();
            } catch (ParseException pe) {
                throw new RuntimeException(pe);
            } catch (ObjectQueryExpression.EvaluationException ee) {
                throw new RuntimeException(ee);
            }

            List<EntityDto[]> result = new ArrayList<EntityDto[]>();
            for (ObjectQueryExpression.Row queryResultRow : queryResultRows) {
                List<EntityDto> row = new ArrayList<EntityDto>();
                for (AbstractModel obj : queryResultRow.columns()) {
                    row.add(this.<EntityDto>buildDto(obj));
                }
                result.add(row.toArray(new EntityDto[0]));
            }
            return result.toArray(new EntityDto[0][]);
        }

        @Override public <T extends EntityDto> Set<T> getRootObjects(Class<T> type) throws AuthenticationException {
            setupReadTransaction();

            Set<T> result = new HashSet<T>();
            for (Model model : getUniquelyNamedModelResolver(mvodtoMapping().getMvoClass(type)).getHome().list()) {
                result.add(this.<T>buildDto((MVO) model));
            }
            return result;
        }

        @Override public <T extends EntityDto> Set<T> getRootObjectsByName(
            Class<T> type, SearchMethod method, String name)
            throws AuthenticationException
        {
            setupReadTransaction();

            Set<T> result = new HashSet<T>();
            for (Model model : getUniquelyNamedModelResolver(mvodtoMapping().getMvoClass(type)).getHome().list()) {
                if (method.matches(((NamedModel) model).getName(), name)) {
                    result.add(this.<T>buildDto((MVO) model));
                }
            }
            return result;
        }

        @Override public <T extends EntityDto> T getUniqueNameObject(Class<T> type, String name)
            throws AuthenticationException
        {
            setupReadTransaction();

            UniquelyNamedModelHome<?> home = getUniquelyNamedModelResolver(mvodtoMapping().getMvoClass(type)).getHome();
            return this.<T>buildDto((MVO) home.getByName(name));
        }

        @Override public <T extends MVO & NodeElement, U extends EntityDto> List<U> getSubElements(
            NodeElementDto dto, Class<T> mvoClass, Class<U> dtoClass)
            throws AuthenticationException
        {
            setupReadTransaction();

            NodeElement mvo = (NodeElement) toMvo(dto);
            List<U> result = new ArrayList<U>();
            for (T elem : NaefMvoUtils.<T>getCurrentSubElements(mvo, mvoClass, true)) {
                result.add(this.<U>buildDto(elem));
            }
            return result;
        }

        @Override public List<String> getConstantsNames() throws AuthenticationException {
            setupReadTransaction();

            List<String> result = new ArrayList<String>();
            for (Constants c : Constants.home.list()) {
                result.add(c.getName());
            }
            return result;
        }

        @Override public List<String> getConstants(String name) throws AuthenticationException {
            setupReadTransaction();

            Constants constants = Constants.home.getByName(name);
            return constants == null
                ? null
                : constants.getValues();
        }

        @Override public Set<LocationDto> getLocations() throws AuthenticationException {
            setupReadTransaction();

            Set<LocationDto> result = new HashSet<LocationDto>();
            for (Location location : Location.home.list()) {
                result.add(this.<LocationDto>buildDto(location));
            }
            return result;
        }

        @Override public Set<NodeDto> getNodes() throws AuthenticationException {
            setupReadTransaction();

            Set<NodeDto> result = new LinkedHashSet<NodeDto>();
            for (Node node : NaefMvoUtils.sort(Node.home.list())) {
                result.add(this.<NodeDto>buildDto(node));
            }
            return result;
        }

        @Override public List<NodeDto> getNodes(Filter<Node> filter) throws AuthenticationException {
            setupReadTransaction();

            NaefDtoSetBuilder builder = new NaefDtoSetBuilder(mvodtoOriginator());
            List<NodeDto> result = new ArrayList<NodeDto>();
            for (Node mvoNode : Node.home.list()) {
                if (filter == null || filter.accept(mvoNode)) {
                    result.add(builder.getNode(mvoNode));
                }
            }
            return result;
        }

        @Override public List<JackDto> getNodeJacks(NodeDto node) throws AuthenticationException {
            setupReadTransaction();

            List<JackDto> result = new ArrayList<JackDto>();
            for (Jack jack : NaefMvoUtils.getCurrentSubElements((Node) toMvo(node), Jack.class, true)) {
                result.add(this.<JackDto>buildDto(jack));
            }
            return result;
        }

        @Override public List<PortDto> getNodePorts(NodeDto node) throws AuthenticationException {
            setupReadTransaction();

            List<PortDto> result = new ArrayList<PortDto>();
            for (Port port : NaefMvoUtils.getCurrentSubElements((Node) toMvo(node), Port.class, true)) {
                result.add(this.<PortDto>buildDto((MVO) port));
            }
            return result;
        }

        @Override public List<PortDto> getNodePorts(NodeDto node, Filter<Port> filter) throws AuthenticationException {
            setupReadTransaction();

            List<PortDto> result = new ArrayList<PortDto>();
            for (Port mvoPort : NaefMvoUtils.getCurrentSubElements((Node) toMvo(node), Port.class, true)) {
                if (filter == null || filter.accept(mvoPort)) {
                    result.add(this.<PortDto>buildDto((MVO) mvoPort));
                }
            }
            Collections.sort(result, new Comparator<PortDto>() {

                @Override public int compare(PortDto o1, PortDto o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            return result;
        }

        @Override public Set<NetworkDto> getPortNetworks(PortDto portDto) throws AuthenticationException {
            setupReadTransaction();

            return this.<NetworkDto>buildDtos(NaefDtoUtils.getPortNetworks((Port) toMvo(portDto)));
        }


        @Override public <T extends IdPoolDto<?, ?, ?>> Set<T> getRootIdPools(Class<T> type)
            throws AuthenticationException
        {
            setupReadTransaction();

            Class<?> mvoType = mvodtoMapping().getMvoClass(type);
            String typename = SkeltonTefService.instance().uiTypeNames().getName(mvoType);

            if (! AbstractHierarchicalModel.class.isAssignableFrom(mvoType)) {
                throw new IllegalArgumentException("マップされたMVO型は階層モデルではありません: " + typename);
            }
            Resolver<?> r = NaefTefService.instance().getResolver(mvoType);
            if (r == null) {
                throw new IllegalStateException("name resolver が未定義です: " + typename);
            }
            if (! (r instanceof UniquelyNamedModelResolver<?>)) {
                throw new IllegalStateException("name resolver が非対応の型です: " + typename);
            }

            UniquelyNamedModelResolver<?> resolver = (UniquelyNamedModelResolver<?>) r;

            Set<T> result = new HashSet<T>();
            for (Model model : resolver.getHome().list()) {
                if (((AbstractHierarchicalModel) model).getParent() != null) {
                    continue;
                }

                Object o = this.<EntityDto>buildDto((MVO) model);
                if (! type.isInstance(o)) {
                    throw new IllegalStateException(
                        "生成されたDTOの型が異なります, "
                        + typename + ": " + type.getName() + ", " + o.getClass().getName());
                }
                result.add((T) o);
            }
            return result;
        }

        @Override public <T extends IdPoolDto<?, ID, ?>, ID extends Object & Comparable<ID>>
            Set<T> getLeafIdPools(Class<T> type, ID id)
            throws AuthenticationException
        {
            setupReadTransaction();

            Class<?> mvoType = mvodtoMapping().getMvoClass(type);
            String typename = SkeltonTefService.instance().uiTypeNames().getName(mvoType);

            if (! AbstractHierarchicalModel.class.isAssignableFrom(mvoType)) {
                throw new IllegalArgumentException("マップされたMVO型は階層モデルではありません: " + typename);
            }
            Resolver<?> r = NaefTefService.instance().getResolver(mvoType);
            if (r == null) {
                throw new IllegalStateException("name resolver が未定義です: " + typename);
            }
            if (! (r instanceof UniquelyNamedModelResolver<?>)) {
                throw new IllegalStateException("name resolver が非対応の型です: " + typename);
            }

            UniquelyNamedModelResolver<?> resolver = (UniquelyNamedModelResolver<?>) r;

            Map<IdPool<?, ?, ?>, IdPool<?, ?, ?>> idpools = new HashMap<IdPool<?, ?, ?>, IdPool<?, ?, ?>>();
            for (Model model : resolver.getHome().list()) {
                IdPool<?, ?, ?> pool = (IdPool<?, ?, ?>) model;
                if (! ((IdPool<?, ID, ?>) pool).isInRange(id)) {
                    continue;
                }

                IdPool<?, ?, ?> existing = idpools.get(pool.getRoot());
                if (existing == null) {
                    idpools.put((IdPool<?, ?, ?>) pool.getRoot(), pool);
                } else {
                    if (pool.getAncestors().contains(existing)) {
                        idpools.put((IdPool<?, ?, ?>) pool.getRoot(), pool);
                    }
                }
            }

            Set<T> result = new HashSet<T>();
            for (IdPool<?, ?, ?> root : idpools.keySet()) {
                Object o = this.<EntityDto>buildDto((MVO) idpools.get(root));
                if (! type.isInstance(o)) {
                    throw new IllegalStateException(
                        "生成されたDTOの型が異なります, "
                        + typename + ": " + type.getName() + ", " + o.getClass().getName());
                }
                result.add((T) o);
            }
            return result;
        }

        @Override public <T extends IdPoolDto<?, ID, ?>, ID extends Object & Comparable<ID>>
            T getLeafIdPool(T rootIdPool, ID id)
            throws AuthenticationException, RemoteException
        {
            setupReadTransaction();

            return this.<T>buildDto(
                this.<ID>getMostLeafsideIdPool((IdPool.SingleMap<?, ID, ?>) toMvo(rootIdPool), id));
        }

        private <ID extends Object & Comparable<ID>> IdPool.SingleMap<?, ID, ?>
            getMostLeafsideIdPool(IdPool.SingleMap<?, ID, ?> pool, ID id)
        {
            if (! pool.isInRange(id)) {
                return null;
            }

            for (IdPool.SingleMap<?, ID, ?> child : pool.getChildren()) {
                if (child.isInRange(id)) {
                    return this.<ID>getMostLeafsideIdPool(child, id);
                }
            }
            return pool;
        }

        @Override public Set<LinkDto> getLinks() throws AuthenticationException {
            return getLinks(null);
        }

        @Override public Set<LinkDto> getLinks(Class<? extends P2pLink<?>> klass) throws AuthenticationException {
            setupReadTransaction();

            Set<LinkDto> result = new HashSet<LinkDto>();
            for (Node node : Node.home.list()) {
                for (Port port : NaefMvoUtils.getCurrentSubElements(node, Port.class, true)) {
                    for (P2pLink<?> link
                        : port.getCurrentNetworks((Class<? extends P2pLink>)(klass == null ? P2pLink.class : klass)))
                    {
                        result.add(this.<LinkDto>buildDto(link));
                    }
                }
            }
            return result;
        }

        @Override public VlanSegmentDto getVlanLink(VlanDto vlanDto, PortDto ethportDto)
            throws AuthenticationException
        {
            setupReadTransaction();

            Vlan vlan = (Vlan) toMvo(vlanDto);
            AbstractPort ethport = (AbstractPort) toMvo(ethportDto);

            Set<Network> links = new HashSet<Network>();
            if (ethport instanceof EthPort) {
                for (EthLink ethlink : ethport.getCurrentNetworks(EthLink.class)) {
                    links.add(ethlink.getCurrentContainer() != null
                        ? ethlink.getCurrentContainer()
                        : ethlink);
                }
            } else if (ethport instanceof EthLagIf) {
                links.addAll(ethport.getCurrentNetworks(EthLag.class));
            } else {
                throw new IllegalArgumentException(ethport.getFqn() + " は eth-port/eth-lag-if ではありません.");
            }

            Set<VlanSegment> vlansegments = new HashSet<VlanSegment>();
            for (Network link : links) {
                VlanSegment vlansegment = getVlanSegment(vlan, link);
                if (vlansegment != null) {
                    vlansegments.add(vlansegment);
                }
            }

            if (vlansegments.size() == 0) {
                return null;
            } else if (vlansegments.size() == 1) {
                return this.<VlanSegmentDto>buildDto(vlansegments.iterator().next());
            } else {
                throw new IllegalStateException(
                    "複数のvlan-segmentが見つかりました: " + vlan.getMvoId() + ", " + ethport.getMvoId());
            }
        }

        private VlanSegment getVlanSegment(Vlan vlan, Network link) {
            for (VlanSegment vlansegment : vlan.getCurrentParts(false)) {
                if (vlansegment.getCurrentLowerLayers(false).contains(link)) {
                    return vlansegment;
                }
            }
            return null;
        }

        @Override public Set<PseudowireDto> getPseudowires() throws AuthenticationException {
            setupReadTransaction();

            Set<PseudowireDto> result = new HashSet<PseudowireDto>();
            for (Pseudowire pw : MplsMvoUtils.getPseudowires()) {
                result.add(this.<PseudowireDto>buildDto(pw));
            }
            return result;
        }

        @Override public Set<PseudowireDto> getPseudowires(NodeDto nodeDto) throws AuthenticationException {
            setupReadTransaction();

            Set<PseudowireDto> result = new HashSet<PseudowireDto>();
            for (Port port : NaefMvoUtils.getCurrentSubElements((Node) toMvo(nodeDto), Port.class, true)) {
                for (Pseudowire pw : port.getCurrentNetworks(Pseudowire.class)) {
                    result.add(this.<PseudowireDto>buildDto(pw));
                }
            }
            return result;
        }

        @Override public Set<RsvpLspDto> getRsvpLsps() throws AuthenticationException {
            setupReadTransaction();

            Set<RsvpLspDto> result = new HashSet<RsvpLspDto>();
            for (RsvpLsp lsp : MplsMvoUtils.getRsvpLsps()) {
                result.add(this.<RsvpLspDto>buildDto(lsp));
            }
            return result;
        }

        @Override public Set<RsvpLspDto> getRsvpLsps(NodeDto nodeDto) throws AuthenticationException {
            setupReadTransaction();

            Node node = (Node) toMvo(nodeDto);
            Set<RsvpLspDto> result = new HashSet<RsvpLspDto>();
            for (RsvpLsp lsp : MplsMvoUtils.getRsvpLsps()) {
                if (lsp.getIngressNode() == node) {
                    result.add(this.<RsvpLspDto>buildDto(lsp));
                }
            }
            return result;
        }

        @Override public Set<RsvpLspDto> getRsvpLsps(NodeDto ingressDto, NodeDto egressDto)
            throws AuthenticationException
        {
            setupReadTransaction();

            Node ingress = (Node) toMvo(ingressDto);
            Node egress = (Node) toMvo(egressDto);

            Set<RsvpLspDto> result = new HashSet<RsvpLspDto>();
            for (RsvpLsp lsp : MplsMvoUtils.getRsvpLsps()) {
                if (lsp.getIngressNode() == ingress && lsp.getEgressNode() == egress) {
                    result.add(this.<RsvpLspDto>buildDto(lsp));
                }
            }
            return result;
        }

        @Override public Set<NodeElementDto> getDescendants(NodeElementDto rootDto, boolean includesRoot)
            throws AuthenticationException
        {
            setupReadTransaction();

            NodeElement root = (NodeElement) toMvo(rootDto);
            Set<NodeElementDto> result = new HashSet<NodeElementDto>();
            if (includesRoot) {
                result.add(this.<NodeElementDto>buildDto((MVO) root));
            }
            for (NodeElement descendant : NaefMvoUtils.getCurrentSubElements(root, NodeElement.class, true)) {
                result.add(this.<NodeElementDto>buildDto((MVO) descendant));
            }
            return result;
        }

        @Override public <T extends NodeElementDto> Set<T> selectNodeElements(
            NodeDto nodeDto, Class<T> searchTargetClass, SearchMethod method, String attrName, Object searchValue)
            throws AuthenticationException
        {
            assertMandatoryArg("対象ノード", nodeDto);
            assertMandatoryArg("対象クラス", searchTargetClass);

            setupReadTransaction();

            Set<? extends NodeElement> elems = NaefMvoUtils.getCurrentSubElements(
                (Node) toMvo(nodeDto),
                (Class<? extends NodeElement>) mvodtoMapping().getMvoClass(searchTargetClass),
                true);
            return this.<T>select(elems, method, attrName, searchValue);
        }

        @Override public InternodeTopology getInternodeTopology(Class<? extends P2pLink> linkType)
            throws AuthenticationException 
        {
            setupReadTransaction();

            return new NaefDtoSetBuilder(mvodtoOriginator()).buildInternodeTopology(linkType);
        }

        @Override public List<InterportLinkDto> getInterportLinks(
            NodeDto node1, NodeDto node2, Class<? extends P2pLink> linkType)
            throws AuthenticationException
        {
            setupReadTransaction();

            return new NaefDtoSetBuilder(mvodtoOriginator())
                .buildInterportLinks((Node) toMvo(node1), (Node) toMvo(node2), linkType);
        }

        @Override public Set<IpSubnetDto> getIpSubnets() throws AuthenticationException {
            setupReadTransaction();

            Set<IpSubnetDto> result = new HashSet<IpSubnetDto>();
            for (IpSubnet ipsubnet : IpSubnet.home.list()) {
                if (IpSubnet.Attr.NAMESPACE.get(ipsubnet) != null) {
                    result.add(this.<IpSubnetDto>buildDto(ipsubnet));
                }
            }
            return result;
        }

        @Override public <T extends NodeElementDto> Map<NodeDto, Set<T>> getNodeElementsMap(
            Set<NodeDto> nodes, Class<T> klass)
            throws AuthenticationException, RemoteException
        {
            setupReadTransaction();

            Class<? extends Model> mvoClass = mvodtoMapping().getMvoClass(klass);
            if (! NodeElement.class.isAssignableFrom(mvoClass)) {
                throw new RuntimeException(
                    "mapped mvo class is not suitable, dto:" + klass.getName() + ", mvo:" + mvoClass.getName());
            }
            Class<? extends NodeElement> nodeelemClass = (Class<? extends NodeElement>) mvoClass;

            Map<NodeDto, Set<T>> result = new HashMap<NodeDto, Set<T>>();
            for (NodeDto node : nodes) {
                Node mvoNode = (Node) toMvo(node);
                Set<T> elems = new HashSet<T>();
                for (NodeElement mvoElem : NaefMvoUtils.getCurrentSubElements(mvoNode, nodeelemClass, true)) {
                    elems.add((T) buildDto((MVO) mvoElem));
                }
                result.put(node, elems);
            }
            return result;
        }

        @Override public String getTaskName(EntityDto dto) throws AuthenticationException {
            setupReadTransaction();

            Model mvo = (Model) toMvo(dto);
            AttributeTask task = AttributeTask.ATTRIBUTE.get(mvo);
            return task == null ? null : task.getName();
        }

        @Override public EntityDto buildTaskSynthesizedDto(EntityDto dto) throws AuthenticationException {
            setupReadTransaction();

            Model mvo = (Model) toMvo(dto);
            AttributeTask task = AttributeTask.ATTRIBUTE.get(mvo);
            try {
                return task == null
                    ? dto
                    : tefservice().getTaskSynthesizedDtoBuilder().build(mvodtoOriginator(), task);
            } catch (TaskSynthesizedDtoBuilder.SynthesizeException se) {
                throw new RuntimeException(se);
            }
        }

        @Override public NodeElementHierarchy buildNodeElementHierarchy(Set<NodeElementDescriptor<?>> descriptors)
            throws AuthenticationException
        {
            setupReadTransaction();

            return new NodeElementHierarchy(descriptors);
        }

        @Override public Set<CustomerInfoDto> getCustomerInfos(Set<PortDto> portDtos) throws AuthenticationException {
            setupReadTransaction();

            Set<CustomerInfo> result = null;
            for (PortDto portDto : portDtos) {
                AbstractPort port = (AbstractPort) toMvo(portDto);
                Collection<CustomerInfo> customerInfos = NaefAttributes.CUSTOMER_INFOS.snapshot(port);
                if (result == null) {
                    result = new HashSet<CustomerInfo>();
                    result.addAll(customerInfos);
                } else {
                    result.retainAll(customerInfos);
                }
            }

            return result == null
                ? Collections.<CustomerInfoDto>emptySet()
                : this.<CustomerInfoDto>buildDtos(result);
        }

        @Override public Set<CustomerInfoDto> selectCustomerInfos(
            SearchMethod method, String attrName, Object searchValue)
            throws AuthenticationException
        {
            setupReadTransaction();

            return this.<CustomerInfoDto>select(CustomerInfo.home.list(), method, attrName, searchValue);
        }

        @Override public Set<CustomerInfoDto> getUsers(PortDto port) throws AuthenticationException {
            setupReadTransaction();

            return this.<CustomerInfoDto>buildDtos(NaefMvoUtils.getUsers(port == null ? null : (Port) toMvo(port)));
        }

        @Override public Set<NodeGroupDto> getNodeGroups() throws AuthenticationException {
            setupReadTransaction();

            Set<NodeGroupDto> result = new HashSet<NodeGroupDto>();
            for (NodeGroup nodegroup : NodeGroup.home.list()) {
                result.add(this.<NodeGroupDto>buildDto(nodegroup));
            }
            return result;
        }

        @Override public <T extends EntityDto> Set<T> selectIdPoolUsers(
            IdPoolDto<?, ?, T> poolDto, List<SearchCondition> conditions)
            throws AuthenticationException, RemoteException
        {
            setupReadTransaction();

            IdPool.SingleMap<?, ?, ?> pool = (IdPool.SingleMap<?, ?, ?>) toMvo(poolDto);
            Set<T> result = new HashSet<T>();
            for (Object user : pool.getUsers()) {
                if (user instanceof Model
                    && isMatch((Model) user, conditions))
                {
                    result.add(this.<T>buildDto((MVO) user));
                }
            }
            return result;
        }

        private boolean isMatch(Model obj, List<SearchCondition> conditions) {
            for (SearchCondition condition : conditions) {
                Object value = obj.getValue(condition.attributeName);
                if (! condition.method.matches(value, condition.value)) {
                    return false;
                }
            }
            return true;
        }

        protected <T extends EntityDto> Set<T> select(
            Collection<? extends Model> models,
            SearchMethod method,
            String attrName,
            Object searchValue)
        {
            assertMandatoryArg("検索メソッド", method);
            assertMandatoryArg("属性名", attrName);

            Set<T> result = new HashSet<T>();
            for (Model model : models) {
                Object attrValue = model.getValue(attrName);
                if (method.matches(attrValue, searchValue)) {
                    result.add(this.<T>buildDto((MVO) model));
                }
            }
            return result;
        }

        private void assertMandatoryArg(String argName, Object argValue) {
            if (argValue == null) {
                throw new IllegalArgumentException(argName + " に null は指定できません.");
            }
        }

        @Override public DtoChanges getDtoChanges(
            final TransactionId.W lowerBound,
            final boolean lowerInclusive,
            final TransactionId.W upperBound,
            final boolean upperInclusive)
            throws AuthenticationException, RemoteException
        {
            setupReadTransaction();

            return dtoChangesBuilder.buildDtoChanges(lowerBound, lowerInclusive, upperBound, upperInclusive);
        }
    }
}
