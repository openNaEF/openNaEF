package voss.multilayernms.inventory.nmscore.inventory.accessor;

import jp.iiga.nmt.core.expressions.IMatcher;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.SubnetIp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.constraints.PortEditConstraints;
import voss.multilayernms.inventory.nmscore.model.creator.SubnetIpModelCreator;
import voss.multilayernms.inventory.nmscore.rendering.SubnetRenderingUtil;
import voss.multilayernms.inventory.renderer.SubnetRenderer;
import voss.multilayernms.inventory.util.SubnetListUtil;
import voss.nms.inventory.util.NodeUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SubnetIpHandler {

    private static final Logger log = LoggerFactory.getLogger(SubnetIpHandler.class);

    public static List<SubnetIp> getList(ObjectFilterQuery query, String mvoId) throws NotBoundException, AuthenticationException, IOException, InventoryException, ExternalServiceException {
        List<SubnetIp> subnetIpList = new ArrayList<SubnetIp>();
        IpSubnetDto ipSubnet = MplsNmsInventoryConnector.getInstance().getMvoDto(mvoId, IpSubnetDto.class);
        subnetIpList.addAll(createIpList(ipSubnet, null));

        for (NodeDto node : MplsNmsInventoryConnector.getInstance().getActiveNodes()) {
            int listSize = subnetIpList.size();
            for (int i = 0; i < listSize; i++) {
                PortDto member = NodeUtil.getIpIfByIp(node, SubnetRenderer.getVpnPrefix(ipSubnet), subnetIpList.get(i).getIpAddress());
                if (isTargetPort(member)) {
                    subnetIpList.set(i, SubnetIpModelCreator.createModel(member, InventoryIdCalculator.getId(member)));
                }
            }
        }
        return subnetIpList;
    }


    public static List<SubnetIp> getList(ObjectFilterQuery query) throws NotBoundException, AuthenticationException, IOException, InventoryException, ExternalServiceException {
        List<SubnetIp> subnetIpList = new ArrayList<SubnetIp>();

        if (!(query.containsKey("TargetNodeName") || query.containsKey("TargetIfName"))) {
            IMatcher subnetMatcher = query.get("SubnetIP");
            for (IpSubnetNamespaceDto ipSubnetNameSpace : SubnetRenderer.getAllIpSubnetNamespace()) {
                Set<IpSubnetDto> ipSubnets = ipSubnetNameSpace.getUsers();
                if (ipSubnets == null) {
                    continue;
                }
                for (IpSubnetDto ipSubnet : ipSubnets) {
                    subnetIpList.addAll(createIpList(ipSubnet, subnetMatcher));
                }
            }
        }

        List<PortDto> ipIfList = new ArrayList<PortDto>();
        for (NodeDto node : MplsNmsInventoryConnector.getInstance().getActiveNodes()) {
            for (PortDto port : node.getPorts()) {
                if (isTargetPort(port)) {
                    ipIfList.add((IpIfDto) port);
                }
            }
        }

        for (PortDto ipif : filter(query, ipIfList)) {
            SubnetIp model = SubnetIpModelCreator.createModel(ipif, InventoryIdCalculator.getId(ipif));
            int i = ipContains(subnetIpList, model);
            if (i < 0) {
                subnetIpList.add(model);
            } else {
                subnetIpList.set(i, model);
            }
        }

        return subnetIpList;
    }

    private static int ipContains(List<SubnetIp> subnetIpList, SubnetIp model) {
        int size = subnetIpList.size();
        for (int i = 0; i < size; i++) {
            SubnetIp subnetIp = subnetIpList.get(i);
            if (subnetIp.getIpAddress().equals(model.getIpAddress())
                    && subnetIp.getText().equals(model.getText())) {
                return i;
            }
        }
        return -1;
    }

    private static List<SubnetIp> createIpList(IpSubnetDto ipSubnet, IMatcher matcher) {
        List<SubnetIp> result = new ArrayList<SubnetIp>();

        IpSubnetAddressDto address = ipSubnet.getSubnetAddress();
        if (address != null) {
            long start = SubnetListUtil.getIpRangeMin(address.getAddress().toString(), address.getSubnetMask().toString()) & 0xffffffffl;
            int size = 1 << (32 - address.getSubnetMask());
            for (int i = 0; i < size; i++) {
                String value = SubnetListUtil.int2IpAddressString(start + i);
                if (matcher == null || matcher.matches(value)) {
                    result.add(SubnetIpModelCreator.createIp(value, SubnetRenderer.getVpnPrefix(ipSubnet)));
                }
            }
        }
        return result;
    }

    private static boolean isTargetPort(PortDto port) {
        if (port == null) return false;
        if (!(port instanceof IpIfDto)) return false;
        if (!PortEditConstraints.isPortDirectLine(port)) return false;

        return true;
    }

    private static List<PortDto> filter(ObjectFilterQuery query, Collection<PortDto> ipifs) {
        List<PortDto> result = new ArrayList<PortDto>();

        for (PortDto ipif : ipifs) {
            boolean unmatched = false;
            for (String field : query.keySet()) {
                String value = SubnetRenderingUtil.rendering(ipif, field);
                if (!query.get(field).matches(value)) {
                    unmatched = true;
                    break;
                }
            }
            if (!unmatched) {
                result.add(ipif);
            }
        }
        return result;
    }
}