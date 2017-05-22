package naef.mvo.mpls;

import naef.mvo.PathHop;
import naef.mvo.Port;

public class RsvpLspHop extends PathHop<RsvpLspHop, Port, RsvpLspHopSeries> {

    public RsvpLspHop(MvoId id) {
        super(id);
    }

    public RsvpLspHop(RsvpLspHopSeries owner, Port srcPort, Port dstPort) {
        super(owner, srcPort, dstPort);
    }
}
