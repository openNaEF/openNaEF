package naef.dto;

import naef.dto.ip.IpIfDto;
import naef.mvo.AbstractPort;
import tef.skelton.AttributeType;
import tef.skelton.NamedModel;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PortDto extends NodeElementDto implements NamedModel {

    public static final AttributeType<PortDto> TYPE = new AttributeType.Adapter<PortDto>();

    public static class ExtAttr {

        /**
         * @deprecated {@link #BOUND_IPIFS} に置き換えられました.
         */
        public static final SingleRefAttr<IpIfDto, PortDto> PRIMARY_IPIF
            = new SingleRefAttr<IpIfDto, PortDto>("primary-ipif");

        /**
         * @deprecated {@link #BOUND_IPIFS} に置き換えられました.
         */
        public static final SingleRefAttr<IpIfDto, PortDto> SECONDARY_IPIF
            = new SingleRefAttr<IpIfDto, PortDto>("secondary-ipif");

        /**
         * {@link naef.mvo.AbstractPort.Attr#BOUND_IPIFS} のDTO転写属性です.
         */
        public static final MapKeyRefAttr<IpIfDto, Integer, PortDto> BOUND_IPIFS
            = new MapKeyRefAttr<IpIfDto, Integer, PortDto>(AbstractPort.Attr.BOUND_IPIFS.getName());

        public static final SingleRefAttr<PortDto, PortDto> CONTAINER
            = new SingleRefAttr<PortDto, PortDto>("naef.dto.port.container");
        public static final SetRefAttr<PortDto, PortDto> PARTS
            = new SetRefAttr<PortDto, PortDto>("naef.dto.port.parts");
        public static final SetRefAttr<PortDto, PortDto> UPPER_LAYERS
            = new SetRefAttr<PortDto, PortDto>("naef.dto.port.upper-layers");
        public static final SetRefAttr<PortDto, PortDto> LOWER_LAYERS
            = new SetRefAttr<PortDto, PortDto>("naef.dto.port.lower-layers");
        public static final SetRefAttr<PortDto, PortDto> CROSS_CONNECTIONS
            = new SetRefAttr<PortDto, PortDto>("naef.dto.port.cross-connections");
        public static final SetRefAttr<NetworkDto, PortDto> NETWORKS
            = new SetRefAttr<NetworkDto, PortDto>("naef.dto.port.networks");
        public static final MapKeyValueRefAttr<PortDto, CustomerInfoDto, PortDto> PORT_CUSTOMERINFOS
            = new MapKeyValueRefAttr<PortDto, CustomerInfoDto, PortDto>("naef.dto.port.port-customerinfos");

        public static final SetRefAttr<PortDto, PortDto> ALIASES
            = new SetRefAttr<PortDto, PortDto>("naef.dto.port.aliases");
        public static final SingleRefAttr<PortDto, PortDto> ALIAS_SOURCE
            = new SingleRefAttr<PortDto, PortDto>("naef.dto.port.alias-source");
        public static final SingleRefAttr<PortDto, PortDto> ALIAS_ROOT_SOURCE
            = new SingleRefAttr<PortDto, PortDto>("naef.dto.port.alias-root-source");
    }

    public PortDto() {
    }

    public Desc<NodeDto> getNodeRef() {
        return NodeElementDto.ExtAttr.NODE.get(this);
    }

    public String getIfname() {
        return get(AbstractPort.Attr.IFNAME);
    }

    public Long getIfindex() {
        return get(AbstractPort.Attr.IFINDEX);
    }

    public Set<NetworkDto> getNetworks() {
        return ExtAttr.NETWORKS.deref(this);
    }

    public <T extends NetworkDto> T getLink(Class<T> klass) {
        T result = null;
        for (NetworkDto network : getNetworks()) {
            if (klass.isInstance(network)) {
                return (T) network;
            }
        }
        return null;
    }

    /**
     * network/link の検索を行います。
     * <p>
     * この port に接続された network/link のうち第1引数で指定された属性の値が第2引数であるものを
     * 一つ返します。該当するものが複数ある場合、どれが返されるかは不定です。
     **/
    public NetworkDto selectNetwork(String attrName, Object value) {
        for (NetworkDto network : getNetworks()) {
            Object o = network.getValue(attrName);
            if (value == null ? o == null : value.equals(o)) {
                return network;
            }
        }
        return null;
    }

    /**
     * network/link の検索を行います。
     * <p>
     * この port に接続された network/link のうち第1引数で指定された属性の値が第2引数であるものの
     * 集合を返します。
     **/
    public Set<NetworkDto> selectNetworks(String attrName, Object value) {
        Set<NetworkDto> result = new HashSet<NetworkDto>();
        for (NetworkDto network : getNetworks()) {
            Object o = network.getValue(attrName);
            if (value == null ? o == null : value.equals(o)) {
                result.add(network);
            }
        }
        return result;
    }

    public IpIfDto getPrimaryIpIf() {
        return ExtAttr.PRIMARY_IPIF.deref(this);
    }

    public IpIfDto getSecondaryIpIf() {
        return ExtAttr.SECONDARY_IPIF.deref(this);
    }

    public PortDto getContainer() {
        return ExtAttr.CONTAINER.deref(this);
    }

    public Set<PortDto> getParts() {
        return ExtAttr.PARTS.deref(this);
    }

    public Set<PortDto> getUpperLayers() {
        return ExtAttr.UPPER_LAYERS.deref(this);
    }

    public Set<PortDto> getLowerLayers() {
        return ExtAttr.LOWER_LAYERS.deref(this);
    }

    public Set<PortDto> getCrossConnections() {
        return ExtAttr.CROSS_CONNECTIONS.deref(this);
    }

    public Map<Desc<PortDto>, Desc<CustomerInfoDto>> getPortCustomerInfos() {
        return ExtAttr.PORT_CUSTOMERINFOS.get(this);
    }

    public Set<PortDto> getPortCustomerInfoKeys(CustomerInfoDto customerinfo) {
        Map<Desc<PortDto>, Desc<CustomerInfoDto>> map = ExtAttr.PORT_CUSTOMERINFOS.get(this);
        Set<Desc<PortDto>> result = new HashSet<Desc<PortDto>>();
        for (Desc<PortDto> key : map.keySet()) {
            Desc<CustomerInfoDto> value = map.get(key);
            if (value != null & value.oid().equals(customerinfo.getOid())) {
                result.add(key);
            }
        }
        return toDtosSet(result);
    }

    public CustomerInfoDto getPortCustomerInfo(PortDto port) {
        if (port == null) {
            return null;
        }

        Map<Desc<PortDto>, Desc<CustomerInfoDto>> map = ExtAttr.PORT_CUSTOMERINFOS.get(this);
        for (Desc<PortDto> key : map.keySet()) {
            if (port.getOid().equals(key.oid())) {
                return toDto(map.get(key));
            }
        }
        return null;
    }

    public boolean isAliasSourceable() {
        Boolean value = get(AbstractPort.Attr.ALIAS_SOURCEABLE);
        return value == null ? false : value.booleanValue();
    }

    public Set<PortDto> getAliases() {
        return ExtAttr.ALIASES.deref(this);
    }

    public boolean isAlias() {
        return ExtAttr.ALIAS_SOURCE.get(this) != null;
    }

    public PortDto getAliasSource() {
        return ExtAttr.ALIAS_SOURCE.deref(this);
    }

    public PortDto getAliasRootSource() {
        return ExtAttr.ALIAS_ROOT_SOURCE.deref(this);
    }
}
