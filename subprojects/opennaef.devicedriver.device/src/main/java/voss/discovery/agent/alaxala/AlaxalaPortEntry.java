package voss.discovery.agent.alaxala;

public interface AlaxalaPortEntry {

    public abstract int getIfIndex();

    public abstract int getRpId();

    public abstract int getSlotId();

    public abstract int getPortId();

    public abstract String getIfName();

}