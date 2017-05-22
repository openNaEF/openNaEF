package naef.mvo;

import tef.skelton.AbstractHierarchicalModel;
import tef.skelton.NamedModel;
import tef.skelton.SkeltonTefService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NaefMvoUtils {

    private NaefMvoUtils() {
    }

    public static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    public static <T extends NamedModel> String getNames(Collection<T> models) {
        StringBuilder result = new StringBuilder();
        for (T model : models) {
            result.append(result.length() == 0 ? "" : ",");
            result.append(model.getName());
        }
        return result.toString();
    }

    public static String getName(NodeElement obj) {
        return isUnnamed(obj)
            ? getName(obj.getOwner())
            : obj.getName();
    }

    private static boolean isUnnamed(NodeElement obj) {
        return obj.getName().equals("");
    }

    public static String getSqn(NodeElement obj) {
        return SkeltonTefService.instance().uiTypeNames().getName(obj.getClass()) + ":" + getSqnBody(obj);
    }

    private static String getSqnBody(NodeElement obj) {
        if (obj.getOwner() == null) { 
            return obj.getName();
        }

        String ownerSqnBody = getSqnBody(((AbstractNodeElement) obj.getOwner()));
        return ownerSqnBody + obj.getNode().getSqnDelimiter() + obj.getName();
    }

    public static String getNodeLocalName(NodeElement obj) {
        if (obj.getOwner() == null) { 
            return null;
        }

        Node node = obj.getNode();
        String nodeName = node.getFqn();
        String objName = obj.getFqn();
        return objName.substring(nodeName.length() + node.getFqnPrimaryDelimiter().length());
    }

    public static <T extends NodeElement> Set<T> getCurrentSubElements(
        NodeElement obj, Class<T> klass, boolean recursive)
    {
        Set<T> result = new LinkedHashSet<T>();
        for (NodeElement element : obj.getCurrentSubElements()) {
            if (klass.isInstance(element)) {
                result.add(klass.cast(element));
            }
            if (recursive) {
                result.addAll(getCurrentSubElements(element, klass, true));
            }
        }
        return result;
    }

    public static <T extends NodeElement> List<T> getHereafterSubElements(
        NodeElement obj, Class<T> klass, boolean recursive)
    {
        List<T> result = new ArrayList<T>();
        for (NodeElement element : obj.getHereafterSubElements()) {
            if (klass.isInstance(element)) {
                result.add(klass.cast(element));
            }
            if (recursive) {
                result.addAll(getHereafterSubElements(element, klass, true));
            }
        }
        return result;
    }

    public static <S extends PathHop<S, T, U>, T extends Port, U extends Network.PathHopSeries<S, T, U>> List<S>
        getHops(S lastHop)
    {
        List<S> result = new ArrayList<S>();
        for (S hop = lastHop; hop != null; hop = hop.getPrevious()) {
            result.add(hop);
        }
        Collections.reverse(result);
        return result;
    }

    public static <S extends PathHop<S, T, U>, T extends Port, U extends Network.PathHopSeries<S, T, U>> S getFirstHop(
        S lastHop)
    {
        return lastHop == null
            ? null
            : getHops(lastHop).get(0);
    }

    public static Set<Network> getPortNetworks(Port port) {
        Set<Network> result = new HashSet<Network>();
        for (Network network : port.getCurrentNetworks(Network.class)) {
            result.add(network);
            result.addAll(getOuterUpperNetworks(network));
        }
        return result;
    }

    public static Set<Network> getOuterUpperNetworks(Network network) {
        Set<Network> result = new HashSet<Network>();
        if (network instanceof Network.Containee) {
            Network container = ((Network.Containee) network).getCurrentContainer();
            if (container != null) {
                result.add(container);
                result.addAll(getOuterUpperNetworks(container));
            }
        }
        if (network instanceof Network.LowerStackable) {
            for (Network upperlayer : ((Network.LowerStackable) network).getCurrentUpperLayers(false)) {
                result.add(upperlayer);
                result.addAll(getOuterUpperNetworks(upperlayer));
            }
        }
        return result;
    }

    public static <T extends NamedModel> List<T> sort(List<T> list) {
        Collections.sort(list, new Comparator<NamedModel>() {

            public int compare(NamedModel o1, NamedModel o2) {
                String name1 = o1.getName();
                String name2 = o2.getName();
                if (name1 == null && name2 == null) {
                    return 0;
                }
                if (o1 == null) {
                    return 1;
                }
                if (o2 == null) {
                    return -1;
                }
                return name1.compareTo(name2);
            }
        });
        return list;
    }

    public static <T extends AbstractHierarchicalModel> List<T> selectRoots(List<T> pools) {
        List<T> result = new ArrayList<T>();
        for (T pool : pools) {
            if (pool.getParent() == null) {
                result.add(pool);
            }
        }
        return result;
    }

    public static Set<CustomerInfo> getUsers(Port port) {
        Set<CustomerInfo> result = new HashSet<CustomerInfo>();
        getUsersImpl(result, new HashSet<Port>(), new HashSet<Network>(), port);
        return result;
    }

    public static Set<CustomerInfo> getUsers(Network network) {
        Set<CustomerInfo> result = new HashSet<CustomerInfo>();
        getUsersImpl(result, new HashSet<Port>(), new HashSet<Network>(), network);
        return result;
    }

    private static void getUsersImpl(
        Set<CustomerInfo> result, Set<Port> knownPorts, Set<Network> knownNetworks, Port port)
    {
        if (port == null) {
            return;
        }
        if (knownPorts.contains(port)) {
            return;
        }
        knownPorts.add(port);

        result.addAll(NaefAttributes.CUSTOMER_INFOS.snapshot(port));

        for (Network network : port.getCurrentNetworks(Network.class)) {
            getUsersImpl(result, knownPorts, knownNetworks, network);
        }
        for (Port upperlayer : port.getUpperLayerPorts()) {
            getUsersImpl(result, knownPorts, knownNetworks, upperlayer);
        }
        getUsersImpl(result, knownPorts, knownNetworks, port.getContainer());
        for (NodeElement subelem : port.getCurrentSubElements()) {
            if (subelem instanceof Port) {
                getUsersImpl(result, knownPorts, knownNetworks, (Port) subelem);
            }
        }
        getUsersImpl(result, knownPorts, knownNetworks, AbstractPort.Attr.PRIMARY_IPIF.get((AbstractPort) port));
        getUsersImpl(result, knownPorts, knownNetworks, AbstractPort.Attr.SECONDARY_IPIF.get((AbstractPort) port));
    }

    private static void getUsersImpl(
        Set<CustomerInfo> result, Set<Port> knownPorts, Set<Network> knownNetworks, Network network)
    {
        if (network == null) {
            return;
        }
        if (knownNetworks.contains(network)) {
            return;
        }
        knownNetworks.add(network);

        result.addAll(NaefAttributes.CUSTOMER_INFOS.snapshot(network));

        for (Port port : network.getCurrentMemberPorts()) {
            getUsersImpl(result, knownPorts, knownNetworks, port);
        }
        for (Port port : network.getCurrentAttachedPorts()) {
            getUsersImpl(result, knownPorts, knownNetworks, port);
        }
        if (network instanceof Network.LowerStackable) {
            for (Network upperlayer : ((Network.LowerStackable) network).getCurrentUpperLayers(false)) {
                getUsersImpl(result, knownPorts, knownNetworks, upperlayer);
            }
        }
        if (network instanceof Network.Containee) {
            getUsersImpl(result, knownPorts, knownNetworks, ((Network.Containee) network).getCurrentContainer());
        }
    }
}
