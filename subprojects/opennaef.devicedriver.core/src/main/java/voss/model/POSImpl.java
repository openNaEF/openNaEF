package voss.model;


public class POSImpl extends AbstractPhysicalPort implements POS {
    private static final long serialVersionUID = 1L;
    private Feature feature;

    public void setLogicalFeature(Feature logical) {
        this.feature = logical;
        this.feature.setParentPort(this);
    }

    public Feature getLogicalFeature() {
        return this.feature;
    }

}