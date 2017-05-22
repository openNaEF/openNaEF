package voss.model;


import java.util.ArrayList;
import java.util.List;

public class SonetPathImpl extends AbstractLogicalPort implements SonetPath {
    private static final long serialVersionUID = 1L;
    private ChannelPortFeature feature = new ChannelFeatureImpl();
    private SonetPath parent;
    private final List<SonetPath> children = new ArrayList<SonetPath>();

    @Override
    public List<SonetPath> getSubClassPath() {
        return new ArrayList<SonetPath>(this.children);
    }

    @Override
    public void addSubClassPath(SonetPath path) {
        this.children.add(path);
    }

    @Override
    public void removeSubClassPath(SonetPath path) {
        this.children.remove(path);
    }

    @Override
    public ChannelPortFeature getChannelFeature() {
        return this.feature;
    }

    @Override
    public void setParentPath(SonetPath parent) {
        this.parent = parent;
    }

    @Override
    public SonetPath getParentPath() {
        return this.parent;
    }

}