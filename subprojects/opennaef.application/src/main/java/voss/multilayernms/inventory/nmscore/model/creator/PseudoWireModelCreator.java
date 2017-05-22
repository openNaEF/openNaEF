package voss.multilayernms.inventory.nmscore.model.creator;

import jp.iiga.nmt.core.model.MetaData;
import naef.dto.PortDto;
import naef.dto.mpls.RsvpLspDto;
import net.phalanx.core.models.PseudoWire;
import voss.multilayernms.inventory.config.NmsCorePseudoWireConfiguration;
import voss.multilayernms.inventory.nmscore.model.FakePseudoWire;
import voss.multilayernms.inventory.nmscore.rendering.PseudoWireRenderingUtil;
import voss.multilayernms.inventory.renderer.PseudoWireRenderer;
import voss.multilayernms.inventory.renderer.RsvpLspRenderer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PseudoWireModelCreator {

    public static PseudoWire createModel(FakePseudoWire fpw, RsvpLspDto lsp, String inventoryIdOnLsp,
                                         PortDto acOnIngress, PortDto acOnEgress) throws IOException {
        PseudoWire model = new PseudoWire();
        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : NmsCorePseudoWireConfiguration.getInstance().getPropertyFields()) {
            if (key.equals(NmsCorePseudoWireConfiguration.getInstance().getRelatedRsvplspFieldName())) {
                if (fpw.isPseudoWire() && lsp != null) {
                    properties.put(key, RsvpLspRenderer.getLspName(lsp));
                } else {
                    properties.put(key, null);
                }
            } else {
                properties.put(key, PseudoWireRenderingUtil.rendering(fpw, lsp, key, acOnIngress, acOnEgress));
            }
        }
        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(inventoryIdOnLsp);
        model.setText(PseudoWireRenderer.getPseudoWireID(fpw));
        return model;
    }
}