package voss.multilayernms.inventory.web.vpls;

import naef.dto.PortDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vpls.VplsStringIdPoolDto;
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

public class VplsModel extends AbstractReadOnlyModel<List<VplsIfDto>> {
    private static final long serialVersionUID = 1L;
    private List<VplsIfDto> vplsIfs = new ArrayList<VplsIfDto>();
    private String nodeName;
    private String vplsId;

    public VplsModel(String nodeName, String vplsId) throws AuthenticationException, IOException, ExternalServiceException {
        this.nodeName = nodeName;
        this.vplsId = vplsId;

        renew();
    }

    @Override
    public synchronized List<VplsIfDto> getObject() {
        return vplsIfs;
    }

    public synchronized void renew() throws AuthenticationException, IOException, ExternalServiceException {

        List<VplsIfDto> newPorts = getVplsIfs(nodeName, vplsId);
        vplsIfs.clear();
        this.vplsIfs.addAll(newPorts);
    }

    private static List<VplsIfDto> getVplsIfs(String nodeName, String vplsId) throws IOException, AuthenticationException, ExternalServiceException {
        List<VplsIfDto> result = new ArrayList<VplsIfDto>();
        for (VplsIfDto vplsIf : getVplsIfs()) {
            boolean flg = true;
            if (nodeName != null && !"".equals(nodeName) && vplsIf.getNode().getName().indexOf(nodeName) < 0) {
                flg = false;
            }
            if (vplsId != null && !"".equals(vplsId) && !vplsId.equals(vplsIf.getVplsId().toString())) {
                flg = false;
            }
            if (flg) {
                result.add(vplsIf);
            }
        }

        return result;
    }

    private static List<VplsIfDto> getVplsIfs() throws IOException, AuthenticationException, ExternalServiceException {
        List<VplsIfDto> result = new ArrayList<VplsIfDto>();
        MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
        VplsStringIdPoolDto pool = conn.getVplsStringPool();
        Set<VplsDto> vplsDtos = pool.getUsers();
        for (VplsDto vplsDto : vplsDtos) {
            result.addAll(new ArrayList<VplsIfDto>(vplsDto.getMemberVplsifs()));
        }
        return result;
    }


    @SuppressWarnings("unused")
    private class VplsIfFilter implements PortFilter {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean match(PortDto port) {
            Logger log = LoggerFactory.getLogger(VplsModel.class);
            if (port == null) {
                return false;
            }
            if (port instanceof VplsIfDto) {
                return true;
            }
            log.debug("filtered:(" + port.getClass().getSimpleName() + ")" + port.getAbsoluteName());
            return false;
        }

    }
}