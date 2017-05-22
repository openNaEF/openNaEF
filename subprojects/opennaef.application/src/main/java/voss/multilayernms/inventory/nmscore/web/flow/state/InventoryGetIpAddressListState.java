package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.resistvlansubnet.ResistVlanSubnetModel;
import jp.iiga.nmt.core.model.resistvlansubnet.VlanIdAndSubnetAddress;
import naef.dto.IdRange;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import naef.mvo.ip.IpAddress;
import naef.mvo.ip.IpAddress.SubnetAddressUtils;
import naef.mvo.ip.Ipv4Address;
import naef.mvo.ip.Ipv6Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.*;
import java.util.Map.Entry;

public class InventoryGetIpAddressListState extends UnificUIViewState {

    private final static Logger log = LoggerFactory.getLogger(InventoryGetIpAddressListState.class);

    public InventoryGetIpAddressListState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {
        try {
            ResistVlanSubnetModel model = (ResistVlanSubnetModel) Operation.getTargets(context);
            int maskLength = model.getMaskLength();

            Map<String, Integer> required = calcRequiredNum(model);

            Map<String, List<String>> result = new HashMap<String, List<String>>();

            for (Entry<String, Integer> entry : required.entrySet()) {
                String targetMvoId = entry.getKey();
                IpSubnetNamespaceDto namespace = MplsNmsInventoryConnector.getInstance().getMvoDto(targetMvoId, IpSubnetNamespaceDto.class);
                List<String> addressList = findAllocatableSpace(namespace, maskLength, entry.getValue());
                result.put(targetMvoId, addressList);
            }

            setXmlObject(result);
            super.execute(context);
        } catch (InventoryException e) {
            log.error("" + e);
            throw e;
        } catch (ExternalServiceException e) {
            log.error("" + e);
            throw e;
        } catch (IOException e) {
            log.error("" + e);
            throw e;
        } catch (RuntimeException e) {
            log.error("" + e);
            throw e;
        } catch (ServletException e) {
            log.error("", e);
            throw e;
        }
    }

    private Map<String, Integer> calcRequiredNum(ResistVlanSubnetModel model) {
        Map<String, Integer> list = new HashMap<String, Integer>();
        for (Entry<String, VlanIdAndSubnetAddress> entry : model.getList().entrySet()) {
            String targetMvoId = entry.getValue().getMasterSubnetMvoId();
            if (!list.containsKey(targetMvoId)) {
                list.put(targetMvoId, 0);
            }
            list.put(targetMvoId, list.get(targetMvoId) + 1);
        }
        return list;
    }

    public List<String> findAllocatableSpace(IpSubnetNamespaceDto namespace, int maskLength, int required) {
        List<String> result = new ArrayList<String>();
        if (namespace == null) return result;
        IpSubnetAddressDto masterIpSubnetAddress = namespace.getIpSubnetAddress();
        Set<IdRange<IpAddress>> masterRanges = masterIpSubnetAddress.getIdRanges();
        IpAddress mastarLowerBound;
        IpAddress masterUpperBound;

        for (IdRange<IpAddress> masterRange : masterRanges) {
            mastarLowerBound = masterRange.lowerBound;
            masterUpperBound = masterRange.upperBound;
            IpAddress lowerBound = mastarLowerBound;
            IpAddress upperBound = masterUpperBound;

            List<IpSubnetAddressDto> ranges = getSortedIpSubetAddressList(masterIpSubnetAddress);

            for (IpSubnetAddressDto subnetAddress : ranges) {
                for (IdRange<IpAddress> range : subnetAddress.getIdRanges()) {
                    if (!range.lowerBound.equals(lowerBound)) {
                        upperBound = range.lowerBound.offset(new BigInteger("-1"));
                        result.addAll(outputAllocatableIpSubnet(lowerBound, upperBound, maskLength, required - result.size()));
                        if (result.size() >= required) return result;
                    }
                    lowerBound = range.upperBound.offset(BigInteger.ONE);
                }
            }
            if (!lowerBound.equals(masterUpperBound)) {
                result.addAll(outputAllocatableIpSubnet(lowerBound, masterUpperBound, maskLength, required - result.size()));
                if (result.size() >= required) return result;
            }
        }

        return result;
    }

    private List<IpSubnetAddressDto> getSortedIpSubetAddressList(IpSubnetAddressDto ipSubnetAddress) {
        List<IpSubnetAddressDto> result = new ArrayList<IpSubnetAddressDto>();
        if (ipSubnetAddress == null) return result;

        result.addAll(ipSubnetAddress.getChildren());
        Collections.sort(result, new Comparator<IpSubnetAddressDto>() {
            @Override
            public int compare(IpSubnetAddressDto o1, IpSubnetAddressDto o2) {
                return o1.getAddress().compareTo(o2.getAddress());
            }
        });

        return result;
    }

    private static List<String> outputAllocatableIpSubnet(IpAddress lowerBound, IpAddress upperBound, int maskLength, int required) {
        List<String> result = new ArrayList<String>();
        if (required <= 0) return result;

        BigInteger nextStartOffset = new BigInteger(1, new byte[]{0, 0, 0, 1}).shiftLeft(lowerBound.ipVersionBitLength() - maskLength);

        IpAddress start = getIpRangeMin(lowerBound, maskLength);
        if (start.compareTo(lowerBound) < 0) {
            start = start.offset(nextStartOffset);
        }

        while (true) {
            IpAddress end = SubnetAddressUtils.endAddress(start, maskLength);
            if (upperBound.compareTo(end) >= 0) {
                result.add(start.toString());
                if (result.size() >= required) return result;
            } else {
                break;
            }
            start = start.offset(nextStartOffset);
        }

        return result;
    }

    private static IpAddress getIpRangeMin(IpAddress ipAddress, int maskLength) {
        BigInteger ip = ipAddress.toBigInteger();
        BigInteger subnetMask = new BigInteger(
                1,
                new byte[]{-1, -1, -1, -1})
                .shiftRight(maskLength)
                .xor(new BigInteger(1, new byte[]{-1, -1, -1, -1}));

        BigInteger networkAddress = ip.and(subnetMask);

        if (ipAddress instanceof Ipv4Address) {
            return Ipv4Address.gain(networkAddress.intValue());
        } else if (ipAddress instanceof Ipv6Address) {
            throw new UnsupportedAddressTypeException();
        } else {
            throw new IllegalArgumentException("Address acquisition failure");
        }
    }
}