package voss.model;

import voss.model.value.PortSpeedValue;

import java.util.Arrays;

@SuppressWarnings("serial")
public abstract class AbstractPhysicalPort extends AbstractPort implements PhysicalPort {
    private Integer portIndex_;
    private String portName_;
    private String portTypeName_;
    private String connectorTypeName_;
    private Link link_;

    public AbstractPhysicalPort() {
    }

    public synchronized int getPortIndex() {
        if (portIndex_ == null) {
            throw new NotInitializedException();
        }

        return portIndex_.intValue();
    }

    public synchronized void initPortIndex(int portIndex) {
        if (portIndex_ != null && portIndex_.intValue() == portIndex) {
            return;
        }
        if (portIndex_ != null) {
            throw new AlreadyInitializedException(portIndex_, new Integer(
                    portIndex));
        }

        portIndex_ = new Integer(portIndex);
    }

    public synchronized Integer getRawPortIndex() {
        return this.portIndex_;
    }

    public synchronized String getPortName() {
        return portName_;
    }

    public synchronized void setPortName(String portName) {
        portName_ = portName;
    }

    public synchronized String getPortTypeName() {
        return portTypeName_;
    }

    public synchronized void setPortTypeName(String portTypeName) {
        portTypeName_ = portTypeName;
    }

    public synchronized String getConnectorTypeName() {
        return this.connectorTypeName_;
    }

    public synchronized void setConnectorTypeName(String connectorTypeName) {
        this.connectorTypeName_ = connectorTypeName;
    }

    public void setPortAdministrativeSpeed(PortSpeedValue.Admin portAdminSpeed) {
        addProperty(portAdminSpeed);
    }

    public PortSpeedValue.Admin getPortAdministrativeSpeed() {
        return (PortSpeedValue.Admin) selectConfigProperty(PortSpeedValue.Admin.class);
    }

    public void setPortOperationalSpeed(PortSpeedValue.Oper portOperSpeed) {
        addProperty(portOperSpeed);
    }

    public PortSpeedValue.Oper getPortOperationalSpeed() {
        return (PortSpeedValue.Oper) selectConfigProperty(PortSpeedValue.Oper.class);
    }

    public synchronized void setLink(Link link) {
        link_ = link;
    }

    public synchronized Link getLink() {
        return link_;
    }

    public synchronized PhysicalPort getNeighbor() {
        if (link_ == null) {
            return null;
        }

        if (link_.getPort1() == AbstractPhysicalPort.this) {
            return link_.getPort2();
        }

        if (link_.getPort2() == AbstractPhysicalPort.this) {
            return link_.getPort1();
        }

        throw new RuntimeException("invalid link: this="
                + getFullyQualifiedName() + ", link-port1="
                + link_.getPort1().getFullyQualifiedName() + ", link-port2="
                + link_.getPort2().getFullyQualifiedName());
    }

    public Module getModule() {
        return getModule(getDevice());
    }

    private Module getModule(Container container) {
        if (container == null) {
            return null;
        }
        Slot[] slots = container.getSlots();
        if (slots == null) {
            return null;
        }
        for (int i = 0; i < slots.length; i++) {
            Module module = slots[i].getModule();
            if (module == null) {
                continue;
            }
            if (Arrays.asList(module.getRawPorts()).contains(this)) {
                return module;
            }
            Module subModule = getModule(module);
            if (subModule != null) {
                return subModule;
            }
        }
        return null;
    }

    public void initModule(Module module) {
        module.addPort(AbstractPhysicalPort.this);
    }

    public boolean isFixedChassisPort() {
        return getModule() == null;
    }
}