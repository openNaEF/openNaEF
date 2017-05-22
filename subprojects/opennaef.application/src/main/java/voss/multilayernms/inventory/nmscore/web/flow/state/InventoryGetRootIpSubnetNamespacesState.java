package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.resistvlansubnet.VlanPoolsAndMasterSubnetList;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.renderer.SubnetRenderer;

import javax.servlet.ServletException;
import java.io.IOException;

public class InventoryGetRootIpSubnetNamespacesState extends UnificUIViewState {

    private final static Logger log = LoggerFactory.getLogger(InventoryGetRootIpSubnetNamespacesState.class);

    public InventoryGetRootIpSubnetNamespacesState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {
        try {
            VlanPoolsAndMasterSubnetList list = new VlanPoolsAndMasterSubnetList();
            for (IpSubnetNamespaceDto namespace : SubnetRenderer.getAllIpSubnetNamespace()) {
                list.addSubnetNameSpacelist(namespace.getName(), DtoUtil.getStringOrNull(namespace, ATTR.VPN_PREFIX), DtoUtil.getMvoId(namespace).toString(), isRoot(namespace), deletable(namespace));
            }
            setXmlObject(list);
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

    private boolean deletable(IpSubnetNamespaceDto namespace) {
        if (namespace == null) {
            return false;
        }

        if (namespace.getChildren().size() > 0) {
            return false;
        }

        if (namespace.getUsers().size() > 0) {
            return false;
        }

        IpSubnetAddressDto address = namespace.getIpSubnetAddress();
        if (address != null && address.getChildren().size() > 0) {
            return false;
        }

        return true;
    }

    private boolean isRoot(IpSubnetNamespaceDto ipSubnetNamespace) {
        return ipSubnetNamespace.getParent() == null ? true : false;
    }
}