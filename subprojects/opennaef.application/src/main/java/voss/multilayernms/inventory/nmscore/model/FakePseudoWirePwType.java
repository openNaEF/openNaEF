package voss.multilayernms.inventory.nmscore.model;

import naef.dto.InterconnectionIfDto;
import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import voss.multilayernms.inventory.renderer.PseudoWireRenderer;

import java.io.Serializable;

public class FakePseudoWirePwType implements FakePseudoWire, Serializable {
    private static final long serialVersionUID = 1L;
    private final PseudowireDto pw;

    public FakePseudoWirePwType(PseudowireDto pw) {
        this.pw = pw;
    }

    @Override
    public Long getPseudoWireId() {
        return pw.getLongId();
    }

    @Override
    public String getPseudoWireName() {
        return PseudoWireRenderer.getPseudoWireName(pw);
    }

    @Override
    public PortDto getAc1() {
        return pw.getAc1();
    }

    @Override
    public PortDto getAc2() {
        return pw.getAc2();
    }

    @Override
    public boolean isPseudoWire() {
        return true;
    }

    @Override
    public boolean isPipe() {
        return false;
    }

    @Override
    public PseudowireDto getPseudowireDto() {
        return this.pw;
    }

    @Override
    public InterconnectionIfDto getPipe() {
        return null;
    }

    @Override
    public void renew() {
        this.pw.renew();
    }
}