package pasaran.api;


import naef.NaefTefService;
import naef.mvo.*;
import naef.mvo.eth.EthLag;
import naef.mvo.eth.EthLagIf;
import naef.mvo.eth.EthPort;
import naef.mvo.ip.IpIf;
import naef.mvo.vlan.VlanIf;
import pasaran.pojo.PasaranLinkPOJO;
import pasaran.pojo.PasaranPOJO;
import pasaran.util.MvoUtil;
import pasaran.util.NodeUtil;
import tef.MVO;
import tef.TransactionContext;
import tef.skelton.MvoMap;
import tef.skelton.MvoSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class KeseranPOJOUtil {
    private KeseranPOJOUtil() { }

    private static PasaranPOJO createPOJO(MVO mvo) {
        PasaranPOJO pojo = new PasaranPOJO();
        setCommonPOJOAttr(pojo, mvo);
        return pojo;
    }

    private static PasaranLinkPOJO createLinkPOJO(P2pLink<?> mvo) {
        PasaranLinkPOJO pojo = new PasaranLinkPOJO();
        setCommonPOJOAttr(pojo, mvo);
        return pojo;
    }

    private static void setCommonPOJOAttr(PasaranPOJO pojo, MVO mvo) {
        pojo.mvoId = MvoUtil.toMvoId(mvo);
        pojo.readTxTime = TransactionContext.getTargetTime();
        pojo.readTxVersion = TransactionContext.getTargetVersion().getIdString();
    }

    /**
     * listの中に指定したmvoIdが存在する場合に true
     */
    public static boolean isExists(List<PasaranPOJO> list, String mvoId) {
        return list.parallelStream().anyMatch(o -> o.mvoId.equals(mvoId));
    }

    /**
     * Node の POJO を作る
     */
    public static PasaranPOJO toPOJO(Node node) {
        PasaranPOJO pojo = createPOJO(node);
        pojo.name = node.getName();
        pojo.objectType = getObjectType(node);

        pojo.attributes = new LinkedHashMap<>();
        pojo.attributes.put("vendor-name", Objects.toString(node.getValue("ベンダー名"), ""));
        Location location = (Location) node.getValue("収容フロア");
        while(true) {
            if (location == null) {
                break;
            }
            String locationName = Objects.toString(location.getValue("表示名"));
            String locationType = Objects.toString(location.getValue("種別"));

            if ("Area".equals(locationType)) {
                pojo.area = locationName;
            }
            else if ("Country".equals(locationType)) {
                pojo.country = locationName;
            }
            else if ("City".equals(locationType)) {
                pojo.city = locationName;
            }
            location = location.getParent();
        }

        pojo.vendorName = Objects.toString(node.getValue("ベンダー名"), "");
        pojo.kisyuName = Objects.toString(node.getValue("機種"));
        pojo.purpose = Objects.toString(node.getValue("用途"));

        // ホストノード
        MvoSet<Node> hostNode = Node.Attr.VIRTUALIZATION_HOST_NODES.get(node);
        if (hostNode != null) {
            pojo.hostNode = (MvoUtil.toMvoId(hostNode.get().iterator().next()));
        }

        // ゲストノード
        MvoSet<Node> guestNodes = Node.Attr.VIRTUALIZATION_GUEST_NODES.get(node);
        if(guestNodes != null) {
            for (Node guest : guestNodes.get()) {
                if (pojo.guestNodes == null) {
                    pojo.guestNodes = new ArrayList<>();
                }
                pojo.guestNodes.add(MvoUtil.toMvoId(guest));
            }
        }
        return pojo;
    }

    /**
     * Port の POJO を作る
     */
    public static PasaranPOJO toPOJO(AbstractPort port) {
        PasaranPOJO pojo = createPOJO(port);

        pojo.name = Objects.toString(AbstractPort.Attr.IFNAME.get(port), "");
        pojo.objectType = getObjectType(port);

        if (port instanceof VlanIf) {
            pojo.vlanId = VlanIf.Attr.VLAN_ID.get((VlanIf) port);
            VlanIf vlanIfMvo = (VlanIf) port;
            pojo.taggedPorts = vlanIfMvo.getCurrentTaggedPorts().stream()
                    .map(p -> MvoUtil.toMvoId((MVO)p))
                    .collect(Collectors.toList());
            pojo.untaggedPorts = vlanIfMvo.getCurrentUntaggedPorts().stream()
                    .map(p -> MvoUtil.toMvoId((MVO)p))
                    .collect(Collectors.toList());

            MvoSet<Port> aliases = AbstractPort.Attr.ALIASES.get(port);
            if (aliases != null) {
                pojo.aliases = new ArrayList<>();
                pojo.aliases.addAll(
                        aliases.get().stream()
                                .map(alias -> MvoUtil.toMvoId((MVO) alias))
                                .collect(Collectors.toList()));
                pojo.owner = MvoUtil.toMvoId((MVO) port.getOwner());
            }
        }

        if (port instanceof EthLagIf) {
            pojo.memberPorts = new ArrayList<>();
            pojo.memberPorts.addAll(
                    port.getParts().stream()
                            .map(p -> MvoUtil.toMvoId((AbstractPort) p))
                            .collect(Collectors.toList()));
        }

        if (port instanceof IpIf) {
            MvoSet<Port> associatedPorts = IpIf.ASSOCIATED_PORTS.get((IpIf) port);
            if (associatedPorts != null) {
                pojo.associatedPorts =
                        associatedPorts.get().stream()
                                .map(associatedPort -> MvoUtil.toMvoId((MVO) associatedPort))
                                .collect(Collectors.toList());
            }

            MvoMap<Port, Integer> boundPorts = IpIf.Attr.BOUND_PORTS.get((IpIf) port);
            if (boundPorts != null) {
                pojo.boundPorts = boundPorts.getKeys().stream()
                        .collect(Collectors.toMap(
                                key -> MvoUtil.toMvoId((MVO) key),
                                boundPorts::get
                        ));
            }
        }

        return pojo;
    }

    /**
     * Link の POJO を作る
     */
    public static PasaranPOJO toPOJO(P2pLink<?> link) {
        PasaranLinkPOJO pojo = createLinkPOJO(link);
        pojo.source = MvoUtil.toMvoId((AbstractPort) link.getPort1());
        pojo.target = MvoUtil.toMvoId((AbstractPort) link.getPort2());
        pojo.objectType = getObjectType(link);
        return pojo;
    }

    /**
     * Keseran で使う object-type を決める
     */
    private static String getObjectType(MVO mvo) {
        if (mvo instanceof Node) {
            if (NodeUtil.isVSwitchNode((Node) mvo)) {
                return "virtual-switch";
            }
            if (NodeUtil.isVirtualNode((Node) mvo)) {
                return "virtual-node";
            }
            return "node";
        }
        if (mvo instanceof Port) {
            if(mvo instanceof EthPort) return "eth-port";
            if(mvo instanceof EthLagIf) return "eth-lag-if";
            if(mvo instanceof VlanIf) return "vlan-if";
            if(mvo instanceof IpIf) return "ip-if";
            return NaefTefService.instance().getResolver(mvo.getClass()).getName();
        }
        if (mvo instanceof P2pLink) {
            if(mvo instanceof EthLag) return "eth-lag";
            if(mvo instanceof L2Link) return "eth-link";
            return "link";
        }
        throw new IllegalArgumentException("未定義");
    }
}
