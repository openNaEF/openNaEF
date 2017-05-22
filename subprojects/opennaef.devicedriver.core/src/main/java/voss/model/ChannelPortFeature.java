package voss.model;

import java.util.List;

public interface ChannelPortFeature extends Feature {

    List<Channel> getChannels();

    boolean isStructuredMode();

    Long getSlotUnit();
}