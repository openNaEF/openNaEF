package voss.multilayernms.inventory.web.parts;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vrf.VrfIfDto;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.PortFilter;
import voss.nms.inventory.util.Comparators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VrfIfsModel extends AbstractReadOnlyModel<List<VrfIfDto>> {
    private static final long serialVersionUID = 1L;
    private final NodeDto node;
    private List<VrfIfDto> vrfIfs = new ArrayList<VrfIfDto>();

    public VrfIfsModel(NodeDto node) {
        this.node = node;
        renew();
    }

    @Override
    public synchronized List<VrfIfDto> getObject() {
        return vrfIfs;
    }

    public synchronized void renew() {
        node.renew();
        List<VrfIfDto> newPorts = getVrfIfs(node, new VrfIfFilter());
        Collections.sort(newPorts, Comparators.getIfNameBasedPortComparator());
        vrfIfs.clear();
        this.vrfIfs.addAll(newPorts);
    }

    public static List<VrfIfDto> getVrfIfs(NodeDto node, PortFilter filter) {
        List<VrfIfDto> result = new ArrayList<VrfIfDto>();
        for (PortDto port : node.getPorts()) {
            if (port instanceof VrfIfDto) {
                VrfIfDto vrfIf = (VrfIfDto) port;
                if (DtoUtil.mvoEquals(vrfIf.getNode(), node)) {
                    result.add(vrfIf);
                }
            }
        }

        return result;
    }


    private class VrfIfFilter implements PortFilter {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean match(PortDto port) {
            Logger log = LoggerFactory.getLogger(VrfIfsModel.class);
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