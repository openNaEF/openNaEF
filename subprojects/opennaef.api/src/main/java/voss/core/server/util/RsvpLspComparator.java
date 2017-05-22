package voss.core.server.util;

import naef.dto.NodeDto;
import naef.dto.mpls.RsvpLspDto;

import java.util.Comparator;

public class RsvpLspComparator implements Comparator<RsvpLspDto> {

    @Override
    public int compare(RsvpLspDto o1, RsvpLspDto o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o2 == null) {
            return -1;
        } else if (o1 == null) {
            return 1;
        }
        NodeDto ingress1 = RsvpLspUtil.getIngressNode(o1);
        NodeDto ingress2 = RsvpLspUtil.getIngressNode(o2);
        if (ingress1 == null && ingress2 == null) {
            return o1.getName().compareTo(o2.getName());
        } else if (ingress2 == null) {
            return -1;
        } else if (ingress1 == null) {
            return 1;
        }
        int diff = ingress1.getName().compareTo(ingress2.getName());
        if (diff != 0) {
            return diff;
        }
        return o1.getName().compareTo(o2.getName());
    }

}