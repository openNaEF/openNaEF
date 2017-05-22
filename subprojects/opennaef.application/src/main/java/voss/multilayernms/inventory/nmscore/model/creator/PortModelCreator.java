package voss.multilayernms.inventory.nmscore.model.creator;

import jp.co.iiga.phalanx.core.export.PhalanxConstants;
import jp.iiga.nmt.core.model.LogicalEthernetPort;
import jp.iiga.nmt.core.model.MetaData;
import jp.iiga.nmt.core.model.PhysicalEthernetPort;
import naef.dto.PortDto;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.config.NmsCorePortConfiguration;
import voss.multilayernms.inventory.nmscore.rendering.PortRenderingUtil;
import voss.multilayernms.inventory.renderer.PortRenderer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PortModelCreator {

    public static PhysicalEthernetPort createModel(PortDto port, String inventoryId) throws IOException {
        PhysicalEthernetPort model = new PhysicalEthernetPort();
        Map<String, Object> properties = new HashMap<String, Object>();

        for (String key : NmsCorePortConfiguration.getInstance().getPropertyFields()) {
            String value = PortRenderingUtil.rendering(port, key);
            properties.put(key, value);
        }
        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(inventoryId);
        model.setPropertyValue("mvo-id", DtoUtil.getMvoId(port).toString());
        model.setText(PortRenderer.getIfName(port));

        try {
            String ipAddress = (String) port.getNode().getValue("Management IP Address");
            String nodeName = (String) port.getNode().getName();
            String ifName = (String) port.getIfname();
            String webtelnetUrl = System.getProperty("vossnms.webtelnet.url");
            if (webtelnetUrl != null && webtelnetUrl.length() != 0) {
                webtelnetUrl = webtelnetUrl.replaceAll("_IPADDRESS_", ipAddress);
                webtelnetUrl = webtelnetUrl.replaceAll("_NODENAME_", nodeName);
                webtelnetUrl = webtelnetUrl + "&arg_interfaceName=" + ifName;
            }
            model.setPropertyValue(PhalanxConstants.WEBTELNET_URL_PROPERTY_NAME, webtelnetUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return model;
    }

    public static LogicalEthernetPort createLogicalPortModel(PortDto port, String inventoryId) throws IOException {
        LogicalEthernetPort model = new LogicalEthernetPort();
        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : NmsCorePortConfiguration.getInstance().getPropertyFields()) {
            String value = PortRenderingUtil.rendering(port, key);
            properties.put(key, value);
        }
        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(inventoryId);
        model.setText(PortRenderer.getIfName(port));
        model.setPropertyValue("mvo-id", DtoUtil.getMvoId(port).toString());
        return model;
    }

}