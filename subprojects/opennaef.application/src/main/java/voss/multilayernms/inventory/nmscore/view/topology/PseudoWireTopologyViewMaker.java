package voss.multilayernms.inventory.nmscore.view.topology;

import jp.iiga.nmt.core.model.IDiagram;
import net.phalanx.core.expressions.ObjectFilterQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.nmscore.model.diagrambuilder.PseudoWireDiagramBuilder;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class PseudoWireTopologyViewMaker extends TopologyViewMaker {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(PseudoWireTopologyViewMaker.class);

    public PseudoWireTopologyViewMaker(ObjectFilterQuery query) throws IOException {
        super(query);
    }

    @Override
    public IDiagram makeTopologyView() throws RemoteException, ExternalServiceException, IOException, AuthenticationException, NotBoundException, InstantiationException, IllegalAccessException {
        IDiagram diagram = null;

        if (isQueryContainsInventoryIdAsKey()) {
            String inventoryId = getInventoryIdFromQuery();
            diagram = new PseudoWireDiagramBuilder().build(inventoryId);
        }

        return convertDiagram(diagram);
    }


}