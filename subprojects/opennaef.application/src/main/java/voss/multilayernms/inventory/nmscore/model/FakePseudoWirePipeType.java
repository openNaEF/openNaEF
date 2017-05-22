package voss.multilayernms.inventory.nmscore.model;

import naef.dto.InterconnectionIfDto;
import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import voss.multilayernms.inventory.renderer.PortRenderer;

import java.io.Serializable;
import java.util.*;

public class FakePseudoWirePipeType implements FakePseudoWire, Serializable {
    private static final long serialVersionUID = 1L;
    private final InterconnectionIfDto pipe;
    private final PortDto ac1;
    private final PortDto ac2;

    public FakePseudoWirePipeType(InterconnectionIfDto pipe) {
        if (pipe == null) {
            throw new IllegalArgumentException();
        } else if (pipe.getAttachedPorts().size() != 2) {
            throw new IllegalStateException("member port is not 2 but " + pipe.getAttachedPorts().size());
        }
        this.pipe = pipe;
        Map<String, PortDto> map = new HashMap<String, PortDto>();
        List<String> names = new ArrayList<String>();
        for (PortDto port : pipe.getAttachedPorts()) {
            String name = PortRenderer.getIfName(port);
            names.add(name);
            map.put(name, port);
        }
        Collections.sort(names);
        Iterator<String> it = names.iterator();
        String ac1Name = it.next();
        String ac2Name = it.next();
        this.ac1 = map.get(ac1Name);
        this.ac2 = map.get(ac2Name);
    }

    @Override
    public Long getPseudoWireId() {
        return null;
    }

    @Override
    public String getPseudoWireName() {
        return PortRenderer.getIfName(pipe);
    }

    @Override
    public PortDto getAc1() {
        return this.ac1;
    }

    @Override
    public PortDto getAc2() {
        return this.ac2;
    }

    @Override
    public boolean isPseudoWire() {
        return false;
    }

    @Override
    public boolean isPipe() {
        return true;
    }

    @Override
    public PseudowireDto getPseudowireDto() {
        return null;
    }

    @Override
    public InterconnectionIfDto getPipe() {
        return this.pipe;
    }

    @Override
    public void renew() {
        this.pipe.renew();
    }
}