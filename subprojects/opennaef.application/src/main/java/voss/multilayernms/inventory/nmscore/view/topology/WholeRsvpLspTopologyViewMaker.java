package voss.multilayernms.inventory.nmscore.view.topology;

import jp.iiga.nmt.core.model.IDiagram;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.model.diagrambuilder.WholeRsvpLspDiagramBuilder;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class WholeRsvpLspTopologyViewMaker extends TopologyViewMaker {

    public WholeRsvpLspTopologyViewMaker() throws IOException {
        super(null);
    }

    @Override
    public IDiagram makeTopologyView() throws RemoteException, AuthenticationException, InventoryException, ExternalServiceException, IOException, NotBoundException, InstantiationException, IllegalAccessException {
        IDiagram diagram = null;
        diagram = new WholeRsvpLspDiagramBuilder().build();
        return convertDiagram(diagram);
    }

}