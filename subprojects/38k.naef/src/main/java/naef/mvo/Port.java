package naef.mvo;

import java.util.Collection;
import java.util.Set;

public interface Port extends NodeElement {

    public void joinNetwork(Network network);
    public void disjoinNetwork(Network network);
    public <T extends Network> Set<T> getCurrentNetworks(Class<T> networkClass);
    public <T extends Network> Set<T> getHereafterNetworks(Class<T> networkClass);
    public Collection<? extends Port> getCurrentCrossConnectedPorts();
    public void addLowerLayerPort(Port port);
    public void removeLowerLayerPort(Port port);
    public Collection<? extends Port> getLowerLayerPorts();
    public void addUpperLayerPort(Port port);
    public void removeUpperLayerPort(Port port);
    public Collection<? extends Port> getUpperLayerPorts();
    public void addPart(Port port);
    public void removePart(Port port);
    public Collection<? extends Port> getParts();
    public void setContainer(Port port);
    public void resetContainer();
    public Port getContainer();
}
