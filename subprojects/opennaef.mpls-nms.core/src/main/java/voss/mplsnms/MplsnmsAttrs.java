package voss.mplsnms;

import java.util.HashSet;
import java.util.Set;

import naef.dto.LocationDto;
import naef.dto.NodeDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.mpls.RsvpLspDto;
import naef.mvo.CustomerInfo;
import naef.mvo.Location;
import naef.mvo.Node;
import naef.mvo.NodeGroup;
import naef.mvo.ip.IpIf;
import naef.mvo.ip.IpSubnet;
import naef.mvo.ip.IpSubnetAddress;
import naef.mvo.mpls.RsvpLsp;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.ConfigurationException;
import tef.skelton.ValueException;
import tef.skelton.ValueResolver;
import tef.skelton.dto.EntityDto;

public class MplsnmsAttrs {

    public static class CustomerInfoAttr {

        public static final Attribute.SingleBoolean<CustomerInfo>
            SAKUJYO_FLAG = new Attribute.SingleBoolean<CustomerInfo>("削除フラグ")
        {
            @Override public void validateValue(CustomerInfo model, Boolean value)
                throws ValueException, ConfigurationException
            {
                super.validateValue(model, value);

                if (Boolean.TRUE.equals(value)) {
                    if (CustomerInfo.Attr.REFERENCES.snapshot(model).size() > 0) {
                        throw new ConfigurationException("設定オブジェクトが存在するため削除できません.");
                    }
                }
            }
        };
    }

    public static class NodeAttr {

        public static final Attribute.SingleBoolean<Node>
            SAKUJYO_FLAG = new Attribute.SingleBoolean<Node>("削除フラグ")
        {
            @Override public void validateValue(Node model, Boolean value)
                throws ValueException, ConfigurationException
            {
                super.validateValue(model, value);

                if (Boolean.TRUE.equals(value)) {
                    if (model.getHereafterSubElements().size() > 0) {
                        throw new ConfigurationException("子要素が存在するため削除できません.");
                    }
                }
            }
        };

        public static final Attribute.SingleModel<Location, Node>
            SHUUYOU_FLOOR = new Attribute.SingleModel<Location, Node>("収容フロア", Location.class);
    }

    public static class NodeDtoAttr {

        public static final EntityDto.SingleRefAttr<LocationDto, NodeDto>
            SHUUYOU_FLOOR = new EntityDto.SingleRefAttr<LocationDto, NodeDto>("収容フロア");
    }

    public static class NodeGroupAttr {

        public static final Attribute.SingleBoolean<NodeGroup>
            SAKUJYO_FLAG = new Attribute.SingleBoolean<NodeGroup>("削除フラグ")
        {
            @Override public void validateValue(NodeGroup model, Boolean value)
                throws ValueException, ConfigurationException
            {
                super.validateValue(model, value);

                if (Boolean.TRUE.equals(value)) {
                    if (NodeGroup.Attr.MEMBERS.snapshot(model).size() > 0) {
                        throw new ConfigurationException("子要素が存在するため削除できません.");
                    }
                }
            }
        };
    }

    public static class RsvpLspAttr {

        public static final Attribute.SingleModel<RsvpLsp, RsvpLsp>
            COUPLING_PAIR = new Attribute.SingleModel<RsvpLsp, RsvpLsp>("対向LSP", RsvpLsp.class);
    }

    public static class RsvpLspDtoAttr {

        public static final EntityDto.SingleRefAttr<RsvpLspDto, RsvpLspDto>
            COUPLING_PAIR = new EntityDto.SingleRefAttr<RsvpLspDto, RsvpLspDto>("対向LSP");
    }

    public static class IpSubnetAddressAttr {

        public static final Attribute.SingleLong<IpSubnetAddress>
            ADDRESS_JIDOU_HARAIDASHI_BLOCK_SIZE
            = new Attribute.SingleLong<IpSubnetAddress>("アドレス自動払出ブロックサイズ");
    }

    public static class IpIfAttr {

        public static final Attribute.MapAttr<IpSubnet, Double, IpIf>
            UKAIRO = new Attribute.MapAttr<IpSubnet, Double, IpIf>(
                "迂回路",
                new AttributeType.MvoMapType<IpSubnet, Double>(
                    new ValueResolver.Model<IpSubnet>(IpSubnet.class),
                    ValueResolver.DOUBLE));
        public static final Attribute.MapAttr<IpSubnet, Boolean, IpIf>
            UKAIRO_HOUKOU = new Attribute.MapAttr<IpSubnet, Boolean, IpIf>(
                "迂回路方向",
                new AttributeType.MvoMapType<IpSubnet, Boolean>(
                    new ValueResolver.Model<IpSubnet>(IpSubnet.class),
                    ValueResolver.BOOLEAN));
    }

    public static class IpIfDtoAttr {

        public static final EntityDto.MapKeyRefAttr<IpSubnetDto, Double, IpIfDto>
            UKAIRO = new EntityDto.MapKeyRefAttr<IpSubnetDto, Double, IpIfDto>("迂回路");
        public static final EntityDto.MapKeyRefAttr<IpSubnetDto, Boolean, IpIfDto>
            UKAIRO_HOUKOU = new EntityDto.MapKeyRefAttr<IpSubnetDto, Boolean, IpIfDto>("迂回路方向");
    }

    public static class LocationAttr {

        public static final Attribute.SingleBoolean<Location>
            SAKUJYO_FLAG = new Attribute.SingleBoolean<Location>("削除フラグ")
        {
            @Override public void validateValue(Location model, Boolean value)
                throws ValueException, ConfigurationException
            {
                super.validateValue(model, value);

                if (Boolean.TRUE.equals(value)) {
                    if (model.getChildren().size() > 0) {
                        throw new ConfigurationException("子要素が存在するため削除できません.");
                    }

                    Set<Node> nodes = new HashSet<Node>();
                    for (Node node : Node.home.list()) {
                        if (MplsnmsAttrs.NodeAttr.SHUUYOU_FLOOR.get(node) == model) {
                            throw new ConfigurationException(
                                MplsnmsAttrs.NodeAttr.SHUUYOU_FLOOR.getName()
                                    + " に設定されているため削除できません: " + node.getName());
                        }
                    }
                }
            }
        };

        public static final Attribute.MapAttr<IpSubnet, Double, Location>
            USER_TRAFFIC_BUNPAI_HIRITSU_JIDOU = new Attribute.MapAttr<IpSubnet, Double, Location>(
                "ユーザトラフィック分配比率(自動)",
                new AttributeType.MvoMapType<IpSubnet, Double>(
                    new ValueResolver.Model<IpSubnet>(IpSubnet.class),
                    ValueResolver.DOUBLE));

        public static final Attribute.MapAttr<IpSubnet, Double, Location>
            USER_TRAFFIC_BUNPAI_HIRITSU_SHUDOU = new Attribute.MapAttr<IpSubnet, Double, Location>(
                "ユーザトラフィック分配比率(手動)",
                new AttributeType.MvoMapType<IpSubnet, Double>(
                    new ValueResolver.Model<IpSubnet>(IpSubnet.class),
                    ValueResolver.DOUBLE));
    }

    public static class LocationDtoAttr {

        public static final EntityDto.MapKeyRefAttr<IpSubnetDto, Double, LocationDto>
            USER_TRAFFIC_BUNPAI_HIRITSU_JIDOU = new EntityDto.MapKeyRefAttr<IpSubnetDto, Double, LocationDto>(
                "ユーザトラフィック分配比率(自動)");

        public static final EntityDto.MapKeyRefAttr<IpSubnetDto, Double, LocationDto>
            USER_TRAFFIC_BUNPAI_HIRITSU_SHUDOU = new EntityDto.MapKeyRefAttr<IpSubnetDto, Double, LocationDto>(
                "ユーザトラフィック分配比率(手動)");
    }
}
