package voss.multilayernms.inventory.web.node;

import naef.dto.HardPortDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.PortFilter;
import voss.nms.inventory.config.InventoryConfiguration;
import voss.nms.inventory.model.PortsModel;
import voss.nms.inventory.util.Comparators;
import voss.nms.inventory.util.NodeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HardPortModel extends AbstractReadOnlyModel<List<PortDto>> {

    private static final long serialVersionUID = 1L;
    private final NodeDto node;
    private List<PortDto> ports = new ArrayList<PortDto>();
    private final int repeat;

    public HardPortModel(NodeDto node) {
        this.node = node;
        try {
            InventoryConfiguration config = InventoryConfiguration.getInstance();
            this.repeat = config.getPortsPanelLines();
        } catch (Exception e) {
            throw new IllegalArgumentException("read config failed.", e);
        }
        renew();
    }

    public HardPortModel(NodeDto node, int repeat) {
        this.node = node;
        this.repeat = repeat;
        renew();
    }


    public synchronized void renew() {
        node.renew();
        List<PortDto> newPorts = NodeUtil.getWholePorts(node, new NodeDetailPortFilter());
        Collections.sort(newPorts, Comparators.getIfNameBasedPortComparator());
        ports.clear();
        int lines = 0;
        for (PortDto newPort : newPorts) {
            ports.add(newPort);
            lines++;
            if (this.repeat > 0 && this.repeat == lines) {
                ports.add(null);
                lines = 0;
            }
        }
    }

    private class NodeDetailPortFilter implements PortFilter {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean match(PortDto port) {
            Logger log = LoggerFactory.getLogger(PortsModel.class);
            if (port == null) {
                return false;
            }
            if (port instanceof HardPortDto) {
                return true;
            }
            log.debug("filtered:(" + port.getClass().getSimpleName() + ")" + port.getAbsoluteName());
            return false;
        }
    }

    @Override
    public synchronized List<PortDto> getObject() {
        return ports;
    }

}