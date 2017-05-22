package voss.model;

import voss.model.value.PortSpeedValue;

public interface PhysicalPort extends Port {

    public int getPortIndex() throws NotInitializedException;

    public void initPortIndex(int portIndex);

    public Integer getRawPortIndex();

    public String getPortName();

    public void setPortName(String portName);

    public String getPortTypeName();

    public void setPortTypeName(String portTypeName);

    public String getConnectorTypeName();

    public void setConnectorTypeName(String connectorType);

    public void setPortAdministrativeSpeed(PortSpeedValue.Admin portAdminSpeed);

    public PortSpeedValue.Admin getPortAdministrativeSpeed();

    public void setPortOperationalSpeed(PortSpeedValue.Oper portOperSpeed);

    public PortSpeedValue.Oper getPortOperationalSpeed();

    public Module getModule();

    public void initModule(Module module);

    public boolean isFixedChassisPort();

    public void setLink(Link link);

    public Link getLink();

    public PhysicalPort getNeighbor();
}