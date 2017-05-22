package voss.multilayernms.inventory.nmscore.view.topology;

import jp.iiga.nmt.core.model.*;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.LabelSwitchedPath;
import net.phalanx.core.models.PseudoWire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.model.converter.*;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.List;

public abstract class TopologyViewMaker {

    private final Logger log = LoggerFactory.getLogger(TopologyViewMaker.class);

    private final ObjectFilterQuery query;

    private final NodeModelDisplayNameConverter nodeConverter;
    private final PortModelDisplayNameConverter portConverter;
    private final LinkModelDisplayNameConverter linkConverter;
    private final RsvpLspModelDisplayNameConverter lspConverter;
    private final PseudoWireModelDisplayNameConverter pwConverter;

    public TopologyViewMaker(ObjectFilterQuery query) throws IOException {
        this.query = query;

        this.nodeConverter = new NodeModelDisplayNameConverter();
        this.portConverter = new PortModelDisplayNameConverter();
        this.linkConverter = new LinkModelDisplayNameConverter();
        this.lspConverter = new RsvpLspModelDisplayNameConverter();
        this.pwConverter = new PseudoWireModelDisplayNameConverter();
    }

    public ObjectFilterQuery getQuery() {
        return query;
    }

    public abstract IDiagram makeTopologyView() throws RemoteException, InventoryException, ExternalServiceException, IOException, AuthenticationException, NotBoundException, InstantiationException, IllegalAccessException, ParseException;

    protected boolean isQueryContainsInventoryIdAsKey() {
        return getQuery().containsKey("ID");
    }

    @SuppressWarnings("unchecked")
    protected String getInventoryIdFromQuery() {
        String inventoryId = null;

        Object target = getQuery().get("ID").getPattern();
        if (target instanceof String) {
            inventoryId = (String) target;
        } else if (target instanceof List) {
            inventoryId = (String) ((List<String>) target).get(0);
        }

        log.debug("inventoryId[" + inventoryId + "]");
        return inventoryId;
    }

    protected IDiagram convertDiagram(IDiagram diagram) throws IOException, InstantiationException, IllegalAccessException {

        for (IModel model : diagram.getAllElements()) {
            String modelClassName = model.getClass().getName();

            if (modelClassName.equals(Device.class.getName())) {
                nodeConverter.convertModel(model);
            } else if (modelClassName.equals(LogicalDeviceLayer.class.getName())) {
                nodeConverter.convertModel(model);
            } else if (modelClassName.equals(PhysicalEthernetPort.class.getName())) {
                portConverter.convertModel(model);
            } else if (modelClassName.equals(LogicalEthernetPort.class.getName())) {
                portConverter.convertModel(model);
            } else if (modelClassName.equals(PhysicalLink.class.getName())) {
                linkConverter.convertModel(model);
            } else if (modelClassName.equals(LabelSwitchedPath.class.getName())) {
                lspConverter.convertModel(model);
            } else if (modelClassName.equals(PseudoWire.class.getName())) {
                pwConverter.convertModel(model);
            } else {
                throw new IllegalArgumentException("Target is unknown: " + modelClassName);
            }
        }

        return diagram;
    }
}