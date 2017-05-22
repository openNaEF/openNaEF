package naef.mvo;

import java.util.Collection;
import java.util.Collections;

import tef.skelton.Attribute;
import tef.skelton.Attribute.ValueTypeConstraintAttr;
import tef.skelton.UniquelyNamedModelHome;

public class NetworkType extends NaefObjectType {

    public static class Attr {

        public static final ValueTypeConstraintAttr.Multi<NetworkType, Port, Network> ACCEPTABLE_PORT_TYPES
            = new ValueTypeConstraintAttr.Multi<NetworkType, Port, Network>("naef.acceptable-port-types")
        {
            @Override public Collection<? extends Port> getExistingValues(Network model) {
                return model.getCurrentMemberPorts();
            }

            @Override public Attribute.SingleAttr<NetworkType, Network> getConstraintAttr() {
                return AbstractNetwork.Attr.OBJECT_TYPE;
            }
        };

        public static final ValueTypeConstraintAttr.Multi<NetworkType, Network, Network> ACCEPTABLE_LOWER_TYPES
            = new ValueTypeConstraintAttr.Multi<NetworkType, Network, Network>("naef.acceptable-lower-types")
        {
            @Override public Collection<? extends Network> getExistingValues(Network model) {
                return model instanceof Network.UpperStackable
                    ? ((Network.UpperStackable) model).getHereafterLowerLayers(false)
                    : Collections.<Network>emptySet();
            }

            @Override public Attribute.SingleAttr<NetworkType, Network> getConstraintAttr() {
                return AbstractNetwork.Attr.OBJECT_TYPE;
            }
        };

        public static final ValueTypeConstraintAttr.Multi<NetworkType, Network, Network> ACCEPTABLE_UPPER_TYPES
            = new ValueTypeConstraintAttr.Multi<NetworkType, Network, Network>("naef.acceptable-upper-types")
        {
            @Override public Collection<? extends Network> getExistingValues(Network model) {
                return model instanceof Network.LowerStackable
                    ? ((Network.LowerStackable) model).getHereafterUpperLayers(false)
                    : Collections.<Network>emptySet();
            }

            @Override public Attribute.SingleAttr<NetworkType, Network> getConstraintAttr() {
                return AbstractNetwork.Attr.OBJECT_TYPE;
            }
        };

        public static final ValueTypeConstraintAttr.Single<NetworkType, Network, Network> ACCEPTABLE_CONTAINER_TYPES
            = new ValueTypeConstraintAttr.Single<NetworkType, Network, Network>("naef.acceptable-container-types")
        {
            @Override public Network getExistingValue(Network model) {
                return model instanceof Network.Containee
                    ? ((Network.Containee) model).getCurrentContainer()
                    : null;
            }

            @Override public Attribute.SingleAttr<NetworkType, Network> getConstraintAttr() {
                return AbstractNetwork.Attr.OBJECT_TYPE;
            }
        };

        public static final ValueTypeConstraintAttr.Multi<NetworkType, Network, Network> ACCEPTABLE_PART_TYPES
            = new ValueTypeConstraintAttr.Multi<NetworkType, Network, Network>("naef.acceptable-part-types")
        {
            @Override public Collection<? extends Network> getExistingValues(Network model) {
                return model instanceof Network.Container
                    ? ((Network.Container) model).getHereafterParts(false)
                    : Collections.<Network>emptySet();
            }

            @Override public Attribute.SingleAttr<NetworkType, Network> getConstraintAttr() {
                return AbstractNetwork.Attr.OBJECT_TYPE;
            }
        };
    }

    public static final UniquelyNamedModelHome.SharedNamespace<NetworkType> home
        = new UniquelyNamedModelHome.SharedNamespace<NetworkType>(NaefObjectType.home, NetworkType.class);

    public NetworkType(MvoId id) {
        super(id);
    }

    public NetworkType(String name) {
        super(name);
    }
}
