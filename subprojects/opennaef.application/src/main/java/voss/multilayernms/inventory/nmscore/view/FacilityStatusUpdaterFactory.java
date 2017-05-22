package voss.multilayernms.inventory.nmscore.view;

import jp.iiga.nmt.core.model.IModel;
import net.phalanx.core.models.LabelSwitchedPath;
import net.phalanx.core.models.PseudoWire;
import net.phalanx.core.models.Vpls;
import net.phalanx.core.models.Vrf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.view.datamodifier.*;

import java.io.IOException;
import java.util.Collection;

public class FacilityStatusUpdaterFactory {

    @SuppressWarnings("unused")
    private final static Logger log = LoggerFactory.getLogger(FacilityStatusUpdaterFactory.class);

    public static FacilityStatusUpdater getFacilityStatusUpdater(Collection<? extends IModel> targets, String userName) throws IOException {
        String targetClassName = targets.iterator().next().getClass().getName();

        if (targetClassName.equals(LabelSwitchedPath.class.getName())) {
            return new RsvpLspFacilityStatusUpdater(targets, userName);
        } else if (targetClassName.equals(PseudoWire.class.getName())) {
            return new PseudoWireFacilityStatusUpdater(targets, userName);
        } else if (targetClassName.equals(Vpls.class.getName())) {
            return new VplsFacilityStatusUpdater(targets, userName);
        } else if (targetClassName.equals(Vrf.class.getName())) {
            return new VrfFacilityStatusUpdater(targets, userName);
        }

        throw new IllegalArgumentException("objectType is unknown.");
    }

}