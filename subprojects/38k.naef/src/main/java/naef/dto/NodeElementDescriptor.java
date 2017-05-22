package naef.dto;

import naef.mvo.Node;
import naef.mvo.NodeElement;
import naef.mvo.Port;
import tef.MVO;
import tef.TefService;
import tef.skelton.ResolveException;

import java.io.Serializable;

public abstract class NodeElementDescriptor<T extends NodeElement> implements Serializable {

    public static class MvoId extends NodeElementDescriptor<NodeElement> {

        public final MVO.MvoId mvoid;

        public MvoId(MVO.MvoId mvoid) {
            if (mvoid == null) {
                throw new IllegalArgumentException();
            }
            this.mvoid = mvoid;
        }

        @Override public NodeElement resolve() throws ResolveException {
            MVO mvo = TefService.instance().getMvoRegistry().get(mvoid);
            if (mvo == null) {
                throw new ResolveException("object が見つかりません, mvoid:" + mvoid.getLocalStringExpression());
            }

            if (! (mvo instanceof NodeElement)) {
                throw new ResolveException(
                    "指定された object は node-element ではありません, mvoid:" + mvoid.getLocalStringExpression());
            }

            return (NodeElement) mvo;
        }

        @Override public String toString() {
            return "mvoid:" + mvoid.getLocalStringExpression();
        }

        @Override public int hashCode() {
            return mvoid.hashCode();
        }

        @Override public boolean equals(Object obj) {
            return obj != null && obj.getClass() == this.getClass()
                && mvoid.equals(((MvoId) obj).mvoid);
        }
    }

    public static class IfIndex extends NodeElementDescriptor<Port> {

        public final String nodeName;
        public final Long ifIndex;

        public IfIndex(String nodeName, Long ifIndex) {
            if (nodeName == null || ifIndex == null) {
                throw new IllegalArgumentException("null は指定できません.");
            }
            this.nodeName = nodeName;
            this.ifIndex = ifIndex;
        }

        @Override public Port resolve() throws ResolveException {
            Port result = Node.Attr.IFINDEX_MAP.get(resolveNode(nodeName), ifIndex);
            if (result == null) {
                throw new ResolveException("port が見つかりません, node:" + nodeName + ", ifIndex:" + ifIndex);
            }
            return result;
        }

        @Override public String toString() {
            return "node:" + nodeName + ",ifIndex:" + ifIndex;
        }

        @Override public int hashCode() {
            return nodeName.hashCode() + ifIndex.hashCode();
        }

        @Override public boolean equals(Object obj) {
            return obj != null && obj.getClass() == this.getClass()
                && nodeName.equals(((IfIndex) obj).nodeName)
                && ifIndex.equals(((IfIndex) obj).ifIndex);
        }
    }

    public static class IfName extends NodeElementDescriptor<Port> {

        public final String nodeName;
        public final String ifName;

        public IfName(String nodeName, String ifName) {
            if (nodeName == null || ifName == null) {
                throw new IllegalArgumentException("null は指定できません.");
            }
            this.nodeName = nodeName;
            this.ifName = ifName;
        }

        @Override public Port resolve() throws ResolveException {
            Port result = Node.Attr.IFNAME_MAP.get(resolveNode(nodeName), ifName);
            if (result == null) {
                throw new ResolveException("port が見つかりません, node:" + nodeName + ", ifName:" + ifName);
            }
            return result;
        }

        @Override public String toString() {
            return "node:" + nodeName + ",ifName" + ifName;
        }

        @Override public int hashCode() {
            return nodeName.hashCode() + ifName.hashCode();
        }

        @Override public boolean equals(Object obj) {
            return obj != null && obj.getClass() == this.getClass()
                && nodeName.equals(((IfName) obj).nodeName)
                && ifName.equals(((IfName) obj).ifName);
        }
    }

    protected static Node resolveNode(String nodeName) throws ResolveException {
        Node result = Node.home.getByName(nodeName);
        if (result == null) {
            throw new ResolveException("node が見つかりません: " + nodeName);
        }
        return result;
    }

    public abstract T resolve() throws ResolveException;
}
