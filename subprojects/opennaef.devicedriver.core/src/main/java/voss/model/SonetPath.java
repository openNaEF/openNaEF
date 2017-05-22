package voss.model;

import java.util.List;


public interface SonetPath extends LogicalPort {
    void setParentPath(SonetPath parent);

    SonetPath getParentPath();

    List<SonetPath> getSubClassPath();

    void addSubClassPath(SonetPath path);

    void removeSubClassPath(SonetPath path);

    ChannelPortFeature getChannelFeature();
}