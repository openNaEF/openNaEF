package voss.discovery.agent.foundry;

import voss.discovery.iolib.snmp.UnexpectedVarBindException;


public interface PhysicalPortType {

    public final static int NOT_PRESENT = -1;

    public abstract void setup() throws UnexpectedVarBindException;

    public abstract int getIfIndex();

    public abstract int getPortIfIndex();

    public abstract int getPortNumber();

    public abstract int getSlotIndex();

}