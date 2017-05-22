package pasaran.util;

import naef.dto.NodeDto;
import naef.mvo.AbstractPort;
import naef.mvo.NaefMvoUtils;
import naef.mvo.Node;
import naef.mvo.NodeElement;
import tef.TransactionContext;
import tef.TransactionId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class NodeUtil {
    private NodeUtil() { }

    /**
     * node が仮想ノードである場合に true を返す
     *
     * Node の '仮想ノード'属性 が true である場合に true を返す
     * '仮想ノード' が null である場合は false を返す
     * @param node Node
     * @return Node の '仮想ノード'属性 が true である場合に true
     */
    public static boolean isVirtualNode(Node node) {
        Object isVirtualNode = node.getValue("仮想ノード");
        if(isVirtualNode == null) {
            return false;
        }
        if(isVirtualNode instanceof Boolean) {
            return (boolean) isVirtualNode;
        } else {
            throw new IllegalArgumentException("仮想ノード判定に失敗. 不明な型:" + isVirtualNode.getClass().getSimpleName());
        }
    }

    public static boolean isVirtualNode(NodeDto node) {
        return node.getVirtualizationHostNode() != null;
    }

    /**
     * node が vSwitch である場合に true を返す
     * @param node NodeDto
     * @return node が vSwitch である場合に true
     */
    public static boolean isVSwitchNode(NodeDto node) {
        return node.getName().contains("vSwitch");
    }

    public static boolean isVSwitchNode(Node node) {
        return node.getName().contains("vSwitch");
    }

    public static List<Node> getActiveNodes() {
        TransactionId.W targetVersion = TransactionContext.getTargetVersion();
        long targetTime = TransactionContext.getTargetTime();
        return Node.home.list()
                .stream()
                .filter(node -> node.getInitialVersion().compareTo(targetVersion) <= 0)
                .filter(node -> {
                    Long initialTime = (Long) node.getValue("initial-time");
                    return initialTime == null || initialTime <= targetTime;
                })
                .sorted(Comparator.comparing((Node::getName)))
                .collect(Collectors.toList());
    }

    public static List<AbstractPort> getPorts(NodeElement e) {
        List<AbstractPort> ports = new ArrayList<>(NaefMvoUtils.getCurrentSubElements(e, AbstractPort.class, true));
        ports.sort(Comparator.comparing((AbstractPort::getName)));
        return ports;
    }
}
