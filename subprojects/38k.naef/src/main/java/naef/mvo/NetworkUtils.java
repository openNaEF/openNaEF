package naef.mvo;

import tef.skelton.ResolveException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetworkUtils {

    public static <T extends Network> T resolveHereafterNetwork(Class<T> klass, Port... memberPorts)
        throws ResolveException
    {
        return resolveHereafterNetwork(klass, Arrays.<Port>asList(memberPorts));
    }

    public static <T extends Network> T resolveHereafterNetwork(Class<T> klass, List<Port> memberPorts)
        throws ResolveException
    {
        if (memberPorts.size() == 0) {
            return null;
        }

        List<Set<T>> portNetworksList = new ArrayList<Set<T>>();
        for (Port port : memberPorts) {
            portNetworksList.add(port.getHereafterNetworks(klass));
        }

        Set<T> result = new HashSet<T>(portNetworksList.get(0));
        for (Set<T> portNetwork : portNetworksList) {
            result.retainAll(portNetwork);
        }

        if (result.size() > 1) {
            throw new ResolveException("一意に決定できませんでした.");
        }

        return result.size() == 0
            ? null
            : result.iterator().next();
    }

    public static <T extends Network> T getExclusiveLink(Class<T> type, Port port) {
        Set<T> networks = port.<T>getCurrentNetworks(type);
        if (networks.size() == 0) {
            return null;
        } else if (networks.size() == 1) {
            return networks.iterator().next();
        } else {
            throw new IllegalStateException(port.getFqn() + "," + type.getName());
        }
    }
}
