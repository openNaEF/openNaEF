package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.portedit.IpSubnet;
import naef.dto.ip.IpSubnetNamespaceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;
import voss.multilayernms.inventory.renderer.SubnetRenderer;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;

public class InventoryGetIpSubnetNamespaceAndAddressPairState extends UnificUIViewState {

    private final static Logger log = LoggerFactory.getLogger(InventoryGetIpSubnetNamespaceAndAddressPairState.class);

    public InventoryGetIpSubnetNamespaceAndAddressPairState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {
        try {
            IpSubnet model = (IpSubnet) Operation.getTargets(context);

            ArrayList<String> pair = new ArrayList<String>();
            for (IpSubnetNamespaceDto subnetname : SubnetRenderer.getAllIpSubnetNamespace()) {
                if (subnetname.getName() != null && subnetname.getIpSubnetAddress() != null) {
                    pair.add(subnetname.getName() + "," + subnetname.getIpSubnetAddress().getName());
                    model.setIpSubnetNameSpaceAndSubnetAddressNamePair(pair);
                }
            }
            setXmlObject(model);
            super.execute(context);
        } catch (InventoryException e) {
            log.error("" + e);
            throw e;
        } catch (ExternalServiceException e) {
            log.error("" + e);
            throw e;
        } catch (IOException e) {
            log.error("" + e);
            throw e;
        } catch (RuntimeException e) {
            log.error("" + e);
            throw e;
        } catch (ServletException e) {
            log.error("", e);
            throw e;
        }
    }
}