package voss.nms.inventory.model;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import org.apache.wicket.model.AbstractReadOnlyModel;
import voss.core.server.util.NodeUtil;
import voss.core.server.util.PortFilter;
import voss.nms.inventory.config.InventoryConfiguration;
import voss.nms.inventory.util.Comparators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PortsModel extends AbstractReadOnlyModel<List<PortDto>> {
    private static final long serialVersionUID = 1L;
    private final NodeDto node;
    private List<PortDto> ports = new ArrayList<PortDto>();
    private PortFilter filter = null;
    private Comparator<PortDto> comparator = null;
    private final int repeat;

    public PortsModel(NodeDto node) {
        this.node = node;
        try {
            InventoryConfiguration config = InventoryConfiguration.getInstance();
            this.repeat = config.getPortsPanelLines();
        } catch (Exception e) {
            throw new IllegalArgumentException("read config failed.", e);
        }
        renew();
    }

    public PortsModel(NodeDto node, int repeat) {
        this.node = node;
        this.repeat = repeat;
        renew();
    }

    @Override
    public synchronized List<PortDto> getObject() {
        return ports;
    }

    public void setFilter(PortFilter filter) {
        this.filter = filter;
    }

    public PortFilter getFilter() {
        if (this.filter == null) {
            return new NodeDetailPortFilter();
        }
        return this.filter;
    }

    public Comparator<PortDto> getComparator() {
        if (this.comparator == null) {
            return Comparators.getIfNameBasedPortComparator();
        }
        return this.comparator;
    }

    public void setComparator(Comparator<PortDto> comparator) {
        this.comparator = comparator;
    }

    public synchronized void renew() {
        node.renew();
        List<PortDto> newPorts = NodeUtil.getWholePorts(node, getFilter());
        Collections.sort(newPorts, getComparator());
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
}