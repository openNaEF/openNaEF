package voss.multilayernms.inventory.web.vrf;

import naef.dto.PortDto;
import naef.dto.vrf.VrfDto;
import naef.dto.vrf.VrfIfDto;
import naef.dto.vrf.VrfStringIdPoolDto;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.util.PortFilter;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VrfModel extends AbstractReadOnlyModel<List<VrfIfDto>> {
    private static final long serialVersionUID = 1L;
    private List<VrfIfDto> vrfIfs = new ArrayList<VrfIfDto>();
    private String nodeName;
    private String vrfId;

    public VrfModel(String nodeName, String vrfId) throws AuthenticationException, IOException, ExternalServiceException {
        this.nodeName = nodeName;
        this.vrfId = vrfId;

        renew();
    }

    @Override
    public synchronized List<VrfIfDto> getObject() {
        return vrfIfs;
    }

    public synchronized void renew() throws AuthenticationException, IOException, ExternalServiceException {

        List<VrfIfDto> newPorts = getVrfIfs(nodeName, vrfId);
        vrfIfs.clear();
        this.vrfIfs.addAll(newPorts);
    }

    private static List<VrfIfDto> getVrfIfs(String nodeName, String vrfId) throws IOException, AuthenticationException, ExternalServiceException {
        List<VrfIfDto> result = new ArrayList<VrfIfDto>();
        for (VrfIfDto vrfIf : getVrfIfs()) {
            boolean flg = true;
            if (nodeName != null && !"".equals(nodeName) && vrfIf.getNode().getName().indexOf(nodeName) < 0) {
                flg = false;
            }
            if (vrfId != null && !"".equals(vrfId) && !vrfId.equals(vrfIf.getVrfId().toString())) {
                flg = false;
            }
            if (flg) {
                result.add(vrfIf);
            }
        }

        return result;
    }

    private static List<VrfIfDto> getVrfIfs() throws IOException, AuthenticationException, ExternalServiceException {
        List<VrfIfDto> result = new ArrayList<VrfIfDto>();
        MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
        VrfStringIdPoolDto pool = conn.getVrfStringPool();
        Set<VrfDto> vrfDtos = pool.getUsers();
        for (VrfDto vrfDto : vrfDtos) {
            result.addAll(new ArrayList<VrfIfDto>(vrfDto.getMemberVrfifs()));
        }
        return result;
    }


    @SuppressWarnings("unused")
    private class VrfIfFilter implements PortFilter {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean match(PortDto port) {
            Logger log = LoggerFactory.getLogger(VrfModel.class);
            if (port == null) {
                return false;
            }
            if (port instanceof VrfIfDto) {
                return true;
            }
            log.debug("filtered:(" + port.getClass().getSimpleName() + ")" + port.getAbsoluteName());
            return false;
        }

    }
}