package voss.multilayernms.inventory.nmscore.model;

import naef.dto.InterconnectionIfDto;
import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;

public interface FakePseudoWire {
    Long getPseudoWireId();

    String getPseudoWireName();

    PortDto getAc1();

    PortDto getAc2();

    boolean isPseudoWire();

    boolean isPipe();

    PseudowireDto getPseudowireDto();

    InterconnectionIfDto getPipe();

    void renew();
}