package voss.model;

import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("serial")
public class ChannelFeatureImpl extends AbstractLogicalPort implements ChannelPortFeature {
    private final List<Channel> channels = new ArrayList<Channel>();
    private boolean isStructuredMode = false;
    private Long slotUnit = 64 * 1000L;
    private Port parent = null;

    @Override
    public List<Channel> getChannels() {
        return this.channels;
    }

    public void addChannel(Channel ch) {
        if (ch == null) {
            return;
        }
        this.channels.add(ch);
    }

    @Override
    public Long getSlotUnit() {
        return this.slotUnit;
    }

    public void setSlotUnit(Long value) {
        this.slotUnit = value;
    }

    @Override
    public boolean isStructuredMode() {
        return this.isStructuredMode;
    }

    public void setStructuredMode(boolean value) {
        this.isStructuredMode = value;
    }

    @Override
    public Port getParentPort() {
        return this.parent;
    }

    @Override
    public void setParentPort(Port port) {
        this.parent = port;
    }

}