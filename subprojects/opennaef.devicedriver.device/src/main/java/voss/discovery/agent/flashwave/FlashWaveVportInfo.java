package voss.discovery.agent.flashwave;

public class FlashWaveVportInfo {
    private String vportDescriptionAlias;
    private int innerVlanId = -1;
    private int outerVlanId = -1;

    public String getVportDescriptionAlias() {
        return this.vportDescriptionAlias;
    }

    public int getInnerVlanId() {
        return this.innerVlanId;
    }

    public int getOuterVlanId() {
        return this.outerVlanId;
    }

    public void setInnerVlanId(int inner) {
        this.innerVlanId = inner;
    }

    public void setOuterVlanId(int outer) {
        this.outerVlanId = outer;
    }

    public void setVportDescriptionAlias(String descr) {
        this.vportDescriptionAlias = descr;
    }
}