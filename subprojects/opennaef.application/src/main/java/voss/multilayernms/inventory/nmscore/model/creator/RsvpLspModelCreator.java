package voss.multilayernms.inventory.nmscore.model.creator;

import jp.co.iiga.phalanx.core.export.PhalanxConstants;
import jp.iiga.nmt.core.model.MetaData;
import naef.dto.mpls.RsvpLspDto;
import net.phalanx.core.models.LabelSwitchedPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.config.NmsCoreRsvpLspConfiguration;
import voss.multilayernms.inventory.nmscore.model.diagrambuilder.LinkDiagramBuilder;
import voss.multilayernms.inventory.nmscore.rendering.RsvpLspRenderingUtil;
import voss.multilayernms.inventory.renderer.RsvpLspRenderer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RsvpLspModelCreator {

    private static final Logger log = LoggerFactory.getLogger(LinkDiagramBuilder.class);

    public static LabelSwitchedPath createModel(RsvpLspDto lsp, String inventoryId) throws IOException {
        LabelSwitchedPath model = new LabelSwitchedPath();
        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : NmsCoreRsvpLspConfiguration.getInstance().getPropertyFields()) {
            properties.put(key, RsvpLspRenderingUtil.rendering(lsp, key));
        }
        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(inventoryId);
        model.setText(RsvpLspRenderer.getLspName(lsp));

        try {
            String ipAddress = (String) lsp.getIngressNode().getValue("Management IP Address");
            String nodeName = (String) lsp.getIngressNode().getNode().getName();
            String webtelnetUrl = System.getProperty("vossnms.webtelnet.url");
            if (webtelnetUrl != null && webtelnetUrl.length() != 0) {
                webtelnetUrl = webtelnetUrl.replaceAll("_IPADDRESS_", ipAddress);
                webtelnetUrl = webtelnetUrl.replaceAll("_NODENAME_", nodeName);
                log.debug(PhalanxConstants.WEBTELNET_URL_PROPERTY_NAME + ": " + webtelnetUrl);
            }
            model.setPropertyValue(PhalanxConstants.WEBTELNET_URL_PROPERTY_NAME, webtelnetUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return model;
    }

    public static LabelSwitchedPath createNullModel(String inventoryId) throws IOException {
        LabelSwitchedPath model = new LabelSwitchedPath();
        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : NmsCoreRsvpLspConfiguration.getInstance().getPropertyFields()) {
            properties.put(key, "");
        }
        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(inventoryId);
        model.setText("");

        return model;
    }

}