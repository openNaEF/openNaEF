package naef.mvo;

import tef.skelton.Attribute;
import tef.skelton.Attribute.ValueTypeConstraintAttr;
import tef.skelton.UniquelyNamedModelHome;

import java.util.Collection;

public class PortType extends NodeElementType {

    public static class Attr {

        public static final ValueTypeConstraintAttr.Multi<PortType, Network, Port> ACCEPTABLE_NETWORK_TYPES
            = new ValueTypeConstraintAttr.Multi<PortType, Network, Port>("naef.acceptable-network-types")
        {
            @Override public Collection<Network> getExistingValues(Port model) {
                return model.getHereafterNetworks(Network.class);
            }

            @Override public Attribute.SingleAttr<PortType, Port> getConstraintAttr() {
                return AbstractPort.Attr.OBJECT_TYPE;
            }
        };

        public static final ValueTypeConstraintAttr.Multi<PortType, Port, Port> ACCEPTABLE_LOWER_TYPES
            = new ValueTypeConstraintAttr.Multi<PortType, Port, Port>("naef.acceptable-lower-types")
        {
            @Override public Collection<? extends Port> getExistingValues(Port model) {
                return model.getLowerLayerPorts();
            }

            @Override public Attribute.SingleAttr<PortType, Port> getConstraintAttr() {
                return AbstractPort.Attr.OBJECT_TYPE;
            }
        };

        public static final ValueTypeConstraintAttr.Multi<PortType, Port, Port> ACCEPTABLE_UPPER_TYPES
            = new ValueTypeConstraintAttr.Multi<PortType, Port, Port>("naef.acceptable-upper-types")
        {
            @Override public Collection<? extends Port> getExistingValues(Port model) {
                return model.getUpperLayerPorts();
            }

            @Override public Attribute.SingleAttr<PortType, Port> getConstraintAttr() {
                return AbstractPort.Attr.OBJECT_TYPE;
            }
        };

        public static final ValueTypeConstraintAttr.Single<PortType, Port, Port> ACCEPTABLE_CONTAINER_TYPES
            = new ValueTypeConstraintAttr.Single<PortType, Port, Port>("naef.acceptable-container-types")
        {
            @Override public Port getExistingValue(Port model) {
                return model.getContainer();
            }

            @Override public Attribute.SingleAttr<PortType, Port> getConstraintAttr() {
                return AbstractPort.Attr.OBJECT_TYPE;
            }
        };

        public static final ValueTypeConstraintAttr.Multi<PortType, Port, Port> ACCEPTABLE_PART_TYPES
            = new ValueTypeConstraintAttr.Multi<PortType, Port, Port>("naef.acceptable-part-types")
        {
            @Override public Collection<? extends Port> getExistingValues(Port model) {
                return model.getParts();
            }

            @Override public Attribute.SingleAttr<PortType, Port> getConstraintAttr() {
                return AbstractPort.Attr.OBJECT_TYPE;
            }
        };

        public static final ValueTypeConstraintAttr.Multi<PortType, Port, Port> ACCEPTABLE_XCONNECT_TYPES
            = new ValueTypeConstraintAttr.Multi<PortType, Port, Port>("naef.acceptable-xconnect-types")
        {
            @Override public Collection<? extends Port> getExistingValues(Port model) {
                return model.getCurrentCrossConnectedPorts();
            }

            @Override public Attribute.SingleAttr<PortType, Port> getConstraintAttr() {
                return AbstractPort.Attr.OBJECT_TYPE;
            }
        };
    }

    public static final UniquelyNamedModelHome.SharedNamespace<PortType> home
        = new UniquelyNamedModelHome.SharedNamespace<PortType>(NaefObjectType.home, PortType.class);

    public PortType(MvoId id) {
        super(id);
    }

    public PortType(String name) {
        super(name);
    }
}
