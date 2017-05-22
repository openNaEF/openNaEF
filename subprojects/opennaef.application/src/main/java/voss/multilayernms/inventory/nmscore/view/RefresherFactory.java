package voss.multilayernms.inventory.nmscore.view;

import jp.iiga.nmt.core.model.IModel;
import jp.iiga.nmt.core.model.PhysicalEthernetPort;
import jp.iiga.nmt.core.model.PhysicalLink;
import net.phalanx.core.models.LabelSwitchedPath;
import net.phalanx.core.models.PseudoWire;
import net.phalanx.core.models.Vpls;
import net.phalanx.core.models.Vrf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.view.datamodifier.*;

import java.util.Collection;

public class RefresherFactory {
    private static final Logger log = LoggerFactory.getLogger(RefresherFactory.class);

    public static Refresher getRefresher(Collection<? extends IModel> targets, String userName) {
        String targetClassName = targets.iterator().next().getClass().getName();
        log.debug("getTarget:" + targetClassName);

        if (targetClassName.equals(PhysicalEthernetPort.class.getName())) {
            return new PortRefresher(targets, userName);
        } else if (targetClassName.equals(PhysicalLink.class.getName())) {
            return new LinkRefresher(targets, userName);
        } else if (targetClassName.equals(LabelSwitchedPath.class.getName())) {
            return new RsvpLspRefresher(targets, userName);
        } else if (targetClassName.equals(PseudoWire.class.getName())) {
            return new PseudoWireRefresher(targets, userName);
        } else if (targetClassName.equals(Vpls.class.getName())) {
            return new VplsRefresher(targets, userName);
        } else if (targetClassName.equals(Vrf.class.getName())) {
            return new VrfRefresher(targets, userName);
        }

        throw new IllegalArgumentException("Target is unknown.");
    }

}