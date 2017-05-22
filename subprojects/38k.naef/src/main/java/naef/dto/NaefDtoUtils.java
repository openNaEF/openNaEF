package naef.dto;

import naef.NaefTefService;
import naef.mvo.CrossConnection;
import naef.mvo.NaefMvoUtils;
import naef.mvo.Network;
import naef.mvo.Port;
import tef.skelton.dto.EntityDto;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NaefDtoUtils {

    private NaefDtoUtils() {
    }

    public static <T extends Object & Comparable<T>> long getTotalNumberOfIds(Collection<? extends IdRange<T>> ranges) {
        long result = 0;
        for (IdRange<?> range : ranges) {
            result += range.getNumberOfIds();
        }
        return result;
    }

    public static <T extends Object & Comparable<T>> String getConcatenatedIdRangesStr(
        List<? extends IdRange<T>> ranges)
    {
        Collections.sort(ranges);
        StringBuilder result = new StringBuilder();
        for (IdRange<?> range : ranges) {
            result.append(result.length() == 0 ? "" : ", ");
            result.append(range.lowerBound.equals(range.upperBound)
                ? range.lowerBound.toString()
                : range.lowerBound.toString() + "-" + range.upperBound.toString());
        }
        return result.toString();
    }

    public static <T extends Comparable<T>> int compare(T o1, T o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return 1;
        }
        if (o2 == null) {
            return -1;
        }
        return o1.compareTo(o2);
    }

    public static Set<Network> getPortNetworks(Port port) {
        Set<Network> mvoNetworks = new HashSet<Network>();
        mvoNetworks.addAll(NaefMvoUtils.getPortNetworks(port));
        for (Port xconnectedPort : port.getCurrentCrossConnectedPorts()) {
            mvoNetworks.addAll(NaefMvoUtils.getPortNetworks(xconnectedPort));
        }

        Set<Network> result = new HashSet<Network>();
        for (Network network : mvoNetworks) {
            if (network instanceof CrossConnection) {
                continue;
            }


            Class<? extends EntityDto> dtoClass
                = NaefTefService.instance().getMvoDtoMapping().getDtoClass(network.getClass());
            if (dtoClass != null && NetworkDto.class.isAssignableFrom(dtoClass)) {
                result.add(network);
            }
        }
        return result;
    }
}
