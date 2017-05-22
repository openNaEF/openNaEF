package voss.multilayernms.inventory.nmscore.view;

import jp.iiga.nmt.core.model.IDiagram;
import jp.iiga.nmt.core.model.PhysicalLink;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.LabelSwitchedPath;
import net.phalanx.core.models.PseudoWire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.view.topology.*;

import java.io.IOException;

public class TopologyViewMakerFactory {
    private static final Logger log = LoggerFactory.getLogger(TopologyViewMakerFactory.class);

    public static TopologyViewMaker getTopologyViewMaker(ObjectFilterQuery query) throws IOException {
        String targetClassName = query.getTarget().getName();
        log.debug("getTarget:" + targetClassName);

        if (targetClassName.equals(PhysicalLink.class.getName())) {
            return new L2LinkTopologyViewMaker(query);
        } else if (targetClassName.equals(LabelSwitchedPath.class.getName())) {
            return new RsvpLspTopologyViewMaker(query);
        } else if (targetClassName.equals(PseudoWire.class.getName())) {
            return new PseudoWireTopologyViewMaker(query);
        } else if (targetClassName.equals(IDiagram.class.getName())) {
            return new WholeTopologyViewMaker();
        } else {
            throw new IllegalArgumentException("Target is unknown.");
        }

    }

}