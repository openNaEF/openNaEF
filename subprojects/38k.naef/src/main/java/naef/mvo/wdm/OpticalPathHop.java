package naef.mvo.wdm;

import naef.mvo.PathHop;

public class OpticalPathHop extends PathHop<OpticalPathHop, WdmPort, OpticalPath> {

    public OpticalPathHop(MvoId id) {
        super(id);
    }

    public OpticalPathHop(OpticalPath owner, WdmPort srcPort, WdmPort dstPort) {
        super(owner, srcPort, dstPort);
    }
}
