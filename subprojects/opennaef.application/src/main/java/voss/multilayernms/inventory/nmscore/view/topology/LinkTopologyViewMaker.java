package voss.multilayernms.inventory.nmscore.view.topology;

import jp.iiga.nmt.core.model.IDiagram;
import net.phalanx.core.expressions.ObjectFilterQuery;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.nmscore.model.diagrambuilder.LinkDiagramBuilder;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class LinkTopologyViewMaker extends TopologyViewMaker {

    public LinkTopologyViewMaker(ObjectFilterQuery query) throws IOException {
        super(query);
    }

    @Override
    public IDiagram makeTopologyView() throws RemoteException, AuthenticationException, ExternalServiceException, IOException, NotBoundException, InstantiationException, IllegalAccessException {
        IDiagram diagram = null;

        if (isQueryContainsInventoryIdAsKey()) {
            String inventoryId = getInventoryIdFromQuery();
            diagram = new LinkDiagramBuilder().build(inventoryId);
        }

        return convertDiagram(diagram);
    }


}