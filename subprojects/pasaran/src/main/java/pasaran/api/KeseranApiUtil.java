package pasaran.api;


import naef.NaefTefService;
import naef.dto.LocationDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.eth.EthPortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.mvo.AbstractPort;
import naef.mvo.Node;
import naef.mvo.P2pLink;
import naef.mvo.Port;
import naef.mvo.vlan.VlanIf;
import naef.ui.NaefDtoFacade;
import pasaran.NaefConnector;
import pasaran.pojo.PasaranLinkPOJO;
import pasaran.pojo.PasaranPOJO;
import pasaran.util.MvoUtil;
import pasaran.util.NodeUtil;
import pasaran.util.TxUtil;
import tef.MvoRegistry;
import tef.TransactionContext;
import tef.TransactionId;
import voss.mplsnms.MplsnmsAttrs;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class KeseranApiUtil {
    /**
     * 時間とバージョンを指定してNodeを取得
     * @param time 時間
     * @param version バージョン
     * @return Nodeのリスト
     */
    public static List<PasaranPOJO> getNodes(String time, String version) throws RemoteException {
        long start = System.currentTimeMillis();

        List<PasaranPOJO> pojos = new ArrayList<>();
        try {
            // Readトランザクション開始
            TxUtil.beginReadTx(time, version);
            List<Node> nodes = NodeUtil.getActiveNodes();
            for (int i = 0; i < nodes.size(); i++) {
                long nodeStart = System.currentTimeMillis();
                Node node = nodes.get(i);

                PasaranPOJO nodePOJO = KeseranPOJOUtil.toPOJO(node);
                pojos.add(nodePOJO);

                // ポートを追加
                List<AbstractPort> ports = NodeUtil.getPorts(node);
                if (!ports.isEmpty()) {
                    nodePOJO.children = new ArrayList<>();
                    for (AbstractPort p : ports) {
                        PasaranPOJO childPortPOJO = KeseranPOJOUtil.toPOJO(p);
                        childPortPOJO.parent = nodePOJO.mvoId;
                        nodePOJO.children.add(childPortPOJO.mvoId);
                        createPOJOs(pojos, childPortPOJO, p);
                    }
                }
                long nodeEnd = System.currentTimeMillis();
                System.out.printf("[%3d] %s %s %dms.\n", i, MvoUtil.toMvoId(node), node.getName(), (nodeEnd - nodeStart));
            }
        } finally {
            TxUtil.closeTx();
        }
        long end = System.currentTimeMillis();
        System.out.printf("node end. %dms\n", (end - start));

        return pojos;
    }

    private static PasaranPOJO createPOJOs(
            List<PasaranPOJO> result,
            PasaranPOJO parent,
            AbstractPort parentPort)
    {
        result.add(parent);
        List<String> childPorts = new ArrayList<>();

        getUpperLayerPorts(parentPort).stream()
                .filter(p -> !(p instanceof VlanIf))
                .filter(p -> !KeseranPOJOUtil.isExists(result, MvoUtil.toMvoId(p)))
                .forEach(p -> {
                    PasaranPOJO pojo = KeseranPOJOUtil.toPOJO(p);
                    pojo.parent = parent.mvoId;
                    childPorts.add(pojo.mvoId);
                    createPOJOs(result, pojo, p);
                });
        parent.children = childPorts.isEmpty() ? null : childPorts;
        return parent;
    }
    private static List<AbstractPort> getUpperLayerPorts(AbstractPort port) {
        Collection<? extends Port> uppers = port.getUpperLayerPorts();
        return uppers.stream()
                .map(p -> (AbstractPort) p)
                .collect(Collectors.toList());
    }

    /**
     * 時間とバージョンを指定してLinkを取得
     * @param time 時間
     * @param version バージョン
     * @return Linkのリスト
     */
    public static List<PasaranPOJO> getLinks(String time, String version) {
        long start = System.currentTimeMillis();
        List<PasaranPOJO> pojos = new ArrayList<>();
        try {
            // Readトランザクション開始
            TxUtil.beginReadTx(time, version);
            MvoRegistry mvoRegistry = NaefTefService.instance().getMvoRegistry();
            TransactionId.W targetVersion = TransactionContext.getTargetVersion();
            long targetTime = TransactionContext.getTargetTime();

            List<PasaranPOJO> result = new ArrayList<>();
            for (Node node : NodeUtil.getActiveNodes()) {
                for (Port port : NodeUtil.getPorts(node)) {
                    Set<P2pLink> links = port.getCurrentNetworks(P2pLink.class);
                    links.stream()
                            .filter(link -> link.getInitialVersion().compareTo(targetVersion) <= 0)
                            .filter(link -> {
                                Long initialTime = (Long) link.getValue("initial-time");
                                return initialTime == null || initialTime <= targetTime;
                            })
                            .forEach(link -> {
                                result.add(KeseranPOJOUtil.toPOJO(link));
                            });
                }
            }


//            List<PasaranPOJO> links = mvoRegistry.select(P2pLink.class)
//                    .stream()
//                    .filter(link -> link.getInitialVersion().compareTo(targetVersion) <= 0)
//                    .filter(link -> {
//                        Long initialTime = (Long) link.getValue("initial-time");
//                        return initialTime == null || initialTime <= targetTime;
//                    })
//                    .map(KeseranPOJOUtil::toPOJO)
//                    .collect(Collectors.toList());
            pojos.addAll(result);
        } finally {
            TxUtil.closeTx();
        }
        long end = System.currentTimeMillis();
        System.out.printf("link end. %dms\n", (end - start));
        return pojos;
    }

    public static List<PasaranPOJO> getDummyLinks(String time, String version) throws RemoteException {
        long start = System.currentTimeMillis();
        List<PasaranPOJO> pojos = new ArrayList<>();

        NaefDtoFacade facade = NaefConnector.getInstance().getConnection().getDtoFacade();
        for (NodeDto node : facade.getNodes()) {
            LocationDto location = MplsnmsAttrs.NodeDtoAttr.SHUUYOU_FLOOR.deref(node);
            if (location == null) {
                continue;
            }
            if (! location.getName().contains("西日本")) {
                continue;
            }
            for (PortDto port : node.getPorts()) {
                if (!(port instanceof EthPortDto)) {
                    continue;
                }
                IpIfDto sourceIpIf = port.getPrimaryIpIf();
                IpIfDto targetIpIf = null;
                IpSubnetDto ipSubnet = sourceIpIf.getSubnet();
                for (PortDto memberPort : ipSubnet.getMemberIpifs()) {
                    System.out.printf("%s %s %s\n", node.getName(), port.getIfname(), memberPort.getIfname());
                    if (sourceIpIf.getOid().toString().equals(memberPort.getOid().toString())) {
                        continue;
                    }
                    targetIpIf = (IpIfDto) memberPort;
                }

                PortDto targetPort = targetIpIf.getAssociatedPorts().iterator().next();

                PasaranLinkPOJO pojo = new PasaranLinkPOJO();
                pojo.mvoId = MvoUtil.toMvoId(ipSubnet);
                pojo.source = MvoUtil.toMvoId(port);
                pojo.target = MvoUtil.toMvoId(targetPort);
                pojo.objectType = "dummy-eth-link";
                pojos.add(pojo);
            }
        }
        long end = System.currentTimeMillis();
        System.out.printf("dummy-link end. %dms\n", (end - start));

        return pojos;
    }
}
