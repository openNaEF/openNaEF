package voss.multilayernms.inventory.nmscore.view.portedit;

import jp.iiga.nmt.core.model.portedit.PortEditModel;
import naef.dto.PortDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.renderer.SubnetRenderer;

import java.io.IOException;
import java.util.*;

public abstract class PortEditDialogMaker {
    Logger log = LoggerFactory.getLogger(PortEditDialogMaker.class);
    String inventoryId;

    abstract public PortEditModel makeDialog() throws InventoryException, IOException, ExternalServiceException;

    protected static String convertNull2ZeroString(String value) {
        if (value == null) {
            return "";
        } else {
            return value;
        }
    }

    protected Map<String, List<String>> getSubnetList() throws IOException, ExternalServiceException, InventoryException {
        Map<String, List<String>> subnetList = new HashMap<String, List<String>>();
        for (IpSubnetNamespaceDto children : SubnetRenderer.getAllIpSubnetNamespace()) {
            for (IpSubnetDto ipSubnet : children.getUsers()) {
                String ipsubnet = SubnetRenderer.getIpAddress(ipSubnet) + "/" + SubnetRenderer.getSubnetMask(ipSubnet);
                String vpnPrefix = SubnetRenderer.getVpnPrefix(ipSubnet);
                if (vpnPrefix != null) {
                    subnetList.put(ipsubnet + "," + vpnPrefix, getIpList(ipSubnet));
                } else if (vpnPrefix == null) {
                    subnetList.put(ipsubnet, getIpList(ipSubnet));
                }
            }
        }
        return sortMap(subnetList);
    }

    private Map<String, List<String>> sortMap(Map<String, List<String>> map) {
        List<String> keyList = new ArrayList<String>();
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            keyList.add(it.next());
        }
        Collections.sort(keyList);

        LinkedHashMap<String, List<String>> sortedMap = new LinkedHashMap<String, List<String>>();
        for (String key : keyList) {
            Collections.sort(map.get(key));
            sortedMap.put(key, map.get(key));
        }

        return sortedMap;
    }

    private List<String> getIpList(IpSubnetDto ipSubnet) {
        List<String> ipList = new ArrayList<String>();

        for (PortDto port : ipSubnet.getMemberIpifs()) {
            ipList.add(PortRenderer.getIpAddress(port));
        }
        return ipList;
    }
}