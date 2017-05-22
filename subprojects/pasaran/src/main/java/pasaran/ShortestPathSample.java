package pasaran;

import lib38k.topology.ShortestPath;
import naef.mvo.*;
import pasaran.util.NodeUtil;
import pasaran.util.TxUtil;
import voss.mplsnms.MplsnmsNaefService;

import java.util.*;
import java.util.stream.Collectors;

public class ShortestPathSample {
    public static void main(String[] args) throws Exception {
        // Naef 設定
        System.setProperty("voss.mplsnms.rmi-service-name", "mplsnms");
        System.setProperty("running_mode", "console");
        System.setProperty("tef-working-directory", Objects.toString(System.getProperty("tef-working-directory"), "C:/NAEFService"));

        // Naef 起動
        new MplsnmsNaefService().start();

        try {
            Map<Node, ShortestPath<Node>> shortestPaths = new HashMap<>();

            TxUtil.beginReadTx(null, null);
            List<Node> nodes = NodeUtil.getActiveNodes()
                    .stream()
                    .filter(n -> !NodeUtil.isVirtualNode(n))
                    .collect(Collectors.toList());

            long startShortestPathObjCreate = System.currentTimeMillis();
            for (Node node : nodes) {
                System.out.println("create shortest-path-obj " + node.getName());
                shortestPaths.put(node, new MvoShortestPath(node));
            }
            long endShortestPathObjCreate = System.currentTimeMillis();

            long startAddUpDistance = System.currentTimeMillis();
            Map<Node, Integer> result = new HashMap<>();
            for (Map.Entry<Node, ShortestPath<Node>> sp : shortestPaths.entrySet()) {
                Node srcNode = sp.getKey();
                System.out.println("add-up-distance " + srcNode.getName());
                for (Node dstNode : nodes) {
                    Integer total = result.getOrDefault(srcNode, 0);
                    Integer distance = sp.getValue().getDistance(dstNode);
                    if(distance == null) { continue; }
                    result.put(srcNode, total + distance);
                }
            }
            long endAddUpDistance = System.currentTimeMillis();

            for (Map.Entry<Node, Integer> entry : result.entrySet().stream().sorted((s1, s2) -> s1.getValue() - s2.getValue()).collect(Collectors.toList())) {
                System.out.printf("%s : %s\n", entry.getKey().getName(), entry.getValue());
            }

            System.out.println("shortest-path-obj create: " + (endShortestPathObjCreate - startShortestPathObjCreate) + "ms.");
            System.out.println("add-up-distance: " + (endAddUpDistance - startAddUpDistance) + "ms.");

        } finally {
            TxUtil.closeTx();
        }
    }

    private static class MvoShortestPath extends ShortestPath<Node> {
        public MvoShortestPath(Node srcNode) {
            super(srcNode);
        }

        @Override protected Set<Node> getNeighbors(Node node) {
            return NeighborStore.getInstance().getNeighbors(node);
        }

    }

    private static class NeighborStore {
        private static final NeighborStore instance = new NeighborStore();
        private NeighborStore(){}
        public static NeighborStore getInstance(){
            return NeighborStore.instance;
        }

        private Map<Node, Set<Node>> _neighbors = new HashMap<>();

        public Set<Node> getNeighbors(Node node) {
            if(_neighbors.containsKey(node)) {
                return _neighbors.get(node);
            }

            Set<Node> neighbors = new HashSet<>();
            for (AbstractPort p: NodeUtil.getPorts(node)) {
                for (Network n: p.getCurrentNetworks(P2pLink.class)) {
                    for (Port mp: n.getCurrentMemberPorts()) {
                        neighbors.add(mp.getNode());
                    }
                }
            }
            neighbors.remove(node);
            _neighbors.put(node, neighbors);
            return neighbors;
        }
    }
}
