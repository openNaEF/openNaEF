package naef.mvo;

import tef.skelton.Model;

import java.util.Collection;
import java.util.Set;

public interface Network extends Model {

    public interface Interconnectable extends Network {
    }

    /**
     * 積層の上側になりうることを表します.
     **/
    public interface UpperStackable extends Interconnectable {

        public void stackOver(LowerStackable lowerLayer);
        public void unstackOver(LowerStackable lowerLayer);
        public Set<? extends LowerStackable> getHereafterLowerLayers(boolean recursive);
        public Set<? extends LowerStackable> getCurrentLowerLayers(boolean recursive);
    }

    /**
     * 積層の下側になりうることを表します.
     */
    public interface LowerStackable extends Interconnectable {

        public void stackUnder(UpperStackable upperLayer);
        public void unstackUnder(UpperStackable upperLayer);
        public Set<? extends UpperStackable> getHereafterUpperLayers(boolean recursive);
        public Set<? extends UpperStackable> getCurrentUpperLayers(boolean recursive);
    }

    /**
     * 包含の外側になりうることを表します.
     */
    public interface Container extends Interconnectable {

        public void addPart(Containee part);
        public void removePart(Containee part);
        public Set<? extends Containee> getHereafterParts(boolean recursive);
        public Set<? extends Containee> getCurrentParts(boolean recursive);
    }

    /**
     * 包含の内側になりうることを表します.
     */
    public interface Containee extends Interconnectable {

        public void setContainer(Container container);
        public Container getCurrentContainer();
    }

    public interface MemberPortConfigurable<T extends Port> extends Network {

        public void addMemberPort(T port);
        public void removeMemberPort(T port);
    }

    public interface Exclusive {
    }

    public interface PathHopSeries<S extends PathHop<S, T, U>, T extends Port, U extends PathHopSeries<S, T, U>>
        extends Container
    {
        public void setLastHop(S hop);
        public S getLastHop();
    }

    public Collection<? extends Port> getCurrentMemberPorts();

    public Collection<? extends Port> getCurrentAttachedPorts();
}
