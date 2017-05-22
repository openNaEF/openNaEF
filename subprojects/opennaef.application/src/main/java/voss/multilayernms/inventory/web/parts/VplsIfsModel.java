package voss.multilayernms.inventory.web.parts;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vpls.VplsIfDto;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.PortFilter;
import voss.nms.inventory.util.Comparators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VplsIfsModel extends AbstractReadOnlyModel<List<VplsIfDto>> {
    private static final long serialVersionUID = 1L;
    private final NodeDto node;
    private List<VplsIfDto> vplsIfs = new ArrayList<VplsIfDto>();

    public VplsIfsModel(NodeDto node) {
        this.node = node;
        renew();
    }

    @Override
    public synchronized List<VplsIfDto> getObject() {
        return vplsIfs;
    }

    public synchronized void renew() {
        node.renew();
        List<VplsIfDto> newPorts = getVplsIfs(node, new VplsIfFilter());
        Collections.sort(newPorts, Comparators.getIfNameBasedPortComparator());
        vplsIfs.clear();
        this.vplsIfs.addAll(newPorts);
    }

    public static List<VplsIfDto> getVplsIfs(NodeDto node, PortFilter filter) {
        List<VplsIfDto> result = new ArrayList<VplsIfDto>();
        for (PortDto port : node.getPorts()) {
            if (port instanceof VplsIfDto) {
                VplsIfDto vplsIf = (VplsIfDto) port;
                if (DtoUtil.mvoEquals(vplsIf.getNode(), node)) {
                    result.add(vplsIf);
                }
            }
        }

        return result;
    }


    private class VplsIfFilter implements PortFilter {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean match(PortDto port) {
            Logger log = LoggerFactory.getLogger(VplsIfsModel.class);
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