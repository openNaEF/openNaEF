package voss.model;

import java.util.List;


public interface Channel extends LogicalPort {
    void setParentPort(Port parent);

    Port getParentPort();

    List<Integer> getTimeslot();

    void setTimeslot(List<Integer> timeslot);

    String getTimeslotRange();

    void setChannelGroupId(String id);

    String getChannelGroupId();
}