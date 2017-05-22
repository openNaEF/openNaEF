package voss.multilayernms.inventory.nmscore.view.filteringfields;

import naef.dto.LocationDto;
import net.phalanx.core.expressions.FilterQueryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.config.INmsCoreInventoryObjectConfiguration;
import voss.multilayernms.inventory.config.NmsCoreNodeConfiguration;
import voss.multilayernms.inventory.database.LocationType;
import voss.multilayernms.inventory.renderer.LocationRenderer;
import voss.multilayernms.inventory.web.location.LocationUtil;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.LinkedHashSet;


public class NodeFilteringFieldsMaker extends FilteringFieldsMaker {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(NodeFilteringFieldsMaker.class);


    @Override
    protected INmsCoreInventoryObjectConfiguration getConfig() throws IOException {
        return NmsCoreNodeConfiguration.getInstance();
    }


    @Override
    protected void setProposals(String field, FilterQueryContext context) throws RemoteException, IOException, ExternalServiceException {

        if (field.equals("BUILDING")) {
            LinkedHashSet<String> proposals = new LinkedHashSet<String>();
            for (LocationDto location : LocationUtil.getLocationsByType(LocationType.BUILDING)) {
                proposals.add(LocationRenderer.getBuildingCode(location));
            }
            context.setProposals(proposals.toArray(new String[0]));
        } else if (field.equals("CITY")) {
            LinkedHashSet<String> proposals = new LinkedHashSet<String>();
            for (LocationDto location : LocationUtil.getLocationsByType(LocationType.CITY)) {
                proposals.add(LocationUtil.getCaption(location));
            }
            context.setProposals(proposals.toArray(new String[0]));
        } else if (field.equals("COUNTRY")) {
            LinkedHashSet<String> proposals = new LinkedHashSet<String>();
            for (LocationDto location : LocationUtil.getLocationsByType(LocationType.COUNTRY)) {
                proposals.add(LocationUtil.getCaption(location));
            }
            context.setProposals(proposals.toArray(new String[0]));
        } else if (field.equals("AREA")) {
            LinkedHashSet<String> proposals = new LinkedHashSet<String>();
            for (LocationDto location : LocationUtil.getLocationsByType(LocationType.AREA)) {
                proposals.add(LocationUtil.getCaption(location));
            }
            context.setProposals(proposals.toArray(new String[0]));
        } else if (field.equals("FLOOR")) {
            LinkedHashSet<String> proposals = new LinkedHashSet<String>();
            for (LocationDto location : LocationUtil.getLocationsByType(LocationType.FLOOR)) {
                proposals.add(LocationUtil.getCaption(location));
            }
            context.setProposals(proposals.toArray(new String[0]));
        } else if (field.equals("RACK")) {
            LinkedHashSet<String> proposals = new LinkedHashSet<String>();
            for (LocationDto location : LocationUtil.getLocationsByType(LocationType.RACK)) {
                proposals.add(LocationUtil.getCaption(location));
            }
            context.setProposals(proposals.toArray(new String[0]));
        } else if (field.equals("Accommodation Location")) {
            LinkedHashSet<String> proposals = new LinkedHashSet<String>();
            for (LocationDto location : LocationUtil.getLocationsByType(LocationType.AREA)) {
                proposals.add(LocationUtil.getCaption(location));
            }
            context.setProposals(proposals.toArray(new String[0]));
        } else {
            super.setProposals(field, context);
        }

    }
}