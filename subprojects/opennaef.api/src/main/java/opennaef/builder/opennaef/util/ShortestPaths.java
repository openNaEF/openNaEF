package opennaef.builder.opennaef.util;

import lib38k.topology.ShortestPath;
import naef.dto.DtoUtils;
import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ShortestPaths {
    private static final Logger log = LoggerFactory.getLogger(ShortestPaths.class);

    public static boolean isReachable(NodeDto from, NodeDto to) {
        checkArgs(from, to);
        ShortestPath<NodeDto> path = new NodeShortestPath(from);
        boolean isReachable = path.isReachable(to);
        log.debug("isReachable: " + isReachable + " " + from.getAbsoluteName() + " -> " + to.getAbsoluteName());
        return isReachable;
    }

    private static void checkArgs(NodeDto from, NodeDto to) {
        if (from == null || to == null)
            throw new IllegalArgumentException("from/to is null.");
    }

    public static class NodeShortestPath extends ShortestPath<NodeDto> {
        private NeighborStore _store;

        public NodeShortestPath(NodeDto from) {
            super(from);
        }

        @Override
        protected Set<NodeDto> getNeighbors(NodeDto node) {
            if (_store == null) _store = new NeighborStore();

            return _store.getNeighbors(node);
        }

        @Override
        public synchronized boolean isReachable(NodeDto to) {
            Optional<NodeDto> reachableNode = getReachableNodes().parallelStream()
                    .filter(node -> DtoUtils.isSameEntity(to, node))
                    .findFirst();
            return reachableNode.isPresent();
        }

        public synchronized List<NodeDto> getShortestPathHops(NodeDto dstNode) {
            throw new UnsupportedOperationException();
        }
    }

    public static class NeighborStore {
        private final Map<NodeDto, Set<NodeDto>> _neighbors = new HashMap<>();

        public Set<NodeDto> getNeighbors(NodeDto node) {
            if (_neighbors.containsKey(node)) {
                return _neighbors.get(node);
            }

            Set<NodeDto> neighbors = new HashSet<>();
            for (PortDto p : node.getPorts()) {
                for (NetworkDto n : p.getNetworks()) {
                    for (PortDto mp : n.getMemberPorts()) {
                        neighbors.add(mp.getNode());
                    }
                }
            }
            neighbors.remove(node);
            neighbors.remove(null);
            _neighbors.put(node, neighbors);
            return neighbors;
        }
    }
}
