package voss.model;


import voss.util.VossMiscUtility;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class ChannelImpl extends AbstractLogicalPort implements Channel {
    private List<Integer> timeslots = new ArrayList<Integer>();
    private String channelGroupId = null;
    private Port parentPort = null;

    @Override
    public void setParentPort(Port parent) {
        this.parentPort = parent;
    }

    @Override
    public Port getParentPort() {
        return this.parentPort;
    }

    @Override
    public List<Integer> getTimeslot() {
        return this.timeslots;
    }

    public void setTimeslot(List<Integer> timeslots) {
        if (timeslots != null) {
            this.timeslots.clear();
            this.timeslots.addAll(timeslots);
        }
    }

    @Override
    public String getTimeslotRange() {
        return VossMiscUtility.getConcatinatedRange(timeslots);
    }

    @Override
    public void setChannelGroupId(String id) {
        this.channelGroupId = id;
    }

    @Override
    public String getChannelGroupId() {
        return this.channelGroupId;
    }

}