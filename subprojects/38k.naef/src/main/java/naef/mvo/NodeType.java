package naef.mvo;

import naef.mvo.ip.IpIf;
import tef.skelton.Attribute;
import tef.skelton.ConfigurationException;
import tef.skelton.UniquelyNamedModelHome;

/**
 * {@link Node node} の種類を表す型オブジェクトです.
 * <p>
 * {@link Node.Attr#OBJECT_TYPE} に設定します.
 * <p>
 * <dl>
 *  <dt>shell type name: <dd>"node-type"
 * </dl>
 *
 * @see Node.Attr#OBJECT_TYPE
 */
public class NodeType extends NodeElementType {

    /**
     * port と ip-if 間の参照の priority 値のパターンを表します.
     *
     * @see NodeType.Attr#PORT_IPIF_PRIORITY_PATTERN
     * @see AbstractPort.Attr#BOUND_IPIFS
     * @see naef.mvo.ip.IpIf.Attr#BOUND_PORTS
     */
    public enum PortIpifPriorityPattern {

        /**
         * 一つの priority 値に対して設定可能な ip-if を唯一に制限するパターンです.
         * <p>
         * 例えば, ip-if のインスタンス i1, i2, i3 があったとき, 
         * {@link AbstractPort.Attr#BOUND_IPIFS port から ip-if への関連} として
         * { i1:1, i2:2, i3:2 } という状態は priority 値 "2" が重複するため, 設定できません.
         */
        EXCLUSIVE {

            @Override public void validate(Port port, IpIf ipif, Integer priority) {
                if (priority == null) {
                    return;
                }

                for (IpIf boundIpif : AbstractPort.Attr.BOUND_IPIFS.getKeys(port)) {
                    if (boundIpif == ipif) {
                        continue;
                    }

                    Integer boundIpifPriority = AbstractPort.Attr.BOUND_IPIFS.get(port, boundIpif);
                    if (priority.equals(boundIpifPriority)) {
                        throw new ConfigurationException(
                            AbstractPort.Attr.BOUND_IPIFS.getName() + " には既に "
                            + boundIpif.getName() + " が " + priority + " に設定されています.");
                    }
                }

                for (Port boundPort : IpIf.Attr.BOUND_PORTS.getKeys(ipif)) {
                    if (boundPort == port) {
                        continue;
                    }

                    Integer boundPortPriority = IpIf.Attr.BOUND_PORTS.get(ipif, boundPort);
                    if (priority.equals(boundPortPriority)) {
                        throw new ConfigurationException(
                            IpIf.Attr.BOUND_PORTS.getName() + " には既に "
                            + boundPort.getName() + " が " + priority + " に設定されています.");
                    }
                }
            }
        },

        /**
         * 複数の ip-if に対して同一の priority 値を設定できるパターンです.
         * <p>
         * 実質的に制約が無いのと同じです.
         */
        ALLOW_DUPLICATION {

            @Override public void validate(Port port, IpIf ipif, Integer priority) {
            }
        };

        public abstract void validate(Port port, IpIf ipif, Integer priority)
            throws ConfigurationException;
    }

    public static class Attr {

        /**
         * この node-type における port と ip-if の priority 値のパターンを定義します.
         * <p>
         * {@link naef.mvo.AbstractPort.Attr#BOUND_IPIFS} および 
         * {@link naef.mvo.ip.IpIf.Attr#BOUND_PORTS} の値の設定の可否に影響を与えます.
         * <p>
         * <dl>
         *  <dt>attribute name: <dd>"naef.port-ipif-priority-pattern"
         *  <dt>dto transcript: <dd>[automatic]
         * </dl>
         *
         * @see naef.mvo.AbstractPort.Attr#BOUND_IPIFS
         * @see naef.mvo.ip.IpIf.Attr#BOUND_PORTS
         */
        public static final Attribute.SingleEnum<PortIpifPriorityPattern, NodeType> PORT_IPIF_PRIORITY_PATTERN
            = new Attribute.SingleEnum<PortIpifPriorityPattern, NodeType>(
                "naef.port-ipif-priority-pattern",
                PortIpifPriorityPattern.class);
    }

    public static final UniquelyNamedModelHome.SharedNamespace<NodeType> home
        = new UniquelyNamedModelHome.SharedNamespace<NodeType>(NaefObjectType.home, NodeType.class);

    public NodeType(MvoId id) {
        super(id);
    }

    public NodeType(String name) {
        super(name);
    }
}
