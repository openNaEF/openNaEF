package voss.multilayernms.inventory.nmscore.view.topology;

import jp.iiga.nmt.core.model.IDiagram;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.nmscore.model.diagrambuilder.WholeDiagramBuilder;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;

public class WholeTopologyViewMaker extends TopologyViewMaker {

    public WholeTopologyViewMaker() throws IOException {
        super(null);
    }

    @Override
    public IDiagram makeTopologyView() throws RemoteException, AuthenticationException, ExternalServiceException, IOException, NotBoundException, InstantiationException, IllegalAccessException, ParseException {
        IDiagram diagram = new WholeDiagramBuilder().build();

        return convertDiagram(diagram);
    }

}