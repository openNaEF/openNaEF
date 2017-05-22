package voss.model;

public interface VlanStpElement extends VlanModel {

    public String getVlanStpElementId();

    public VlanDevice getDevice();

    public void initDevice(VlanDevice device);

    public VlanIf[] getVlanIfs();

    public void setVlanIfs(VlanIf[] vlanIfs);
}