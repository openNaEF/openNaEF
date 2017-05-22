package voss.model;

@SuppressWarnings("serial")
public class AtmPvc extends AbstractLogicalPort {

    public class Pcr extends LongConfigProperty {

        Pcr() {
        }

        public Pcr(Long value) {
            super(value);
        }
    }

    public class Mcr extends LongConfigProperty {

        Mcr() {
        }

        public Mcr(Long value) {
            super(value);
        }
    }

    public static class AtmQos implements ConfigProperty {
        private AtmQosType type;

        private AtmQos(AtmQosType type) {
            this.type = type;
        }

        AtmQos() {
        }

        AtmQosType getType() {
            return this.type;
        }
    }

    private AtmVp vp_;
    private int vci_;
    private String aalType;

    public AtmPvc(AtmVp vp, String ifName, int vci) {
        initDevice(vp.getDevice());
        initIfName(ifName);
        this.vp_ = vp;
        this.vci_ = vci;
        this.vp_.addPvc(this);
    }

    public AtmPvc(AtmVp vp, int vci) {
        initDevice(vp.getDevice());
        this.vp_ = vp;
        this.vci_ = vci;
        this.vp_.addPvc(this);
    }

    public synchronized AtmVp getVp() {
        return vp_;
    }

    public synchronized int getVci() {
        return vci_;
    }

    public void setPcr(Long value) {
        if (value == null) {
            throw new NullArgumentIsNotAllowedException();
        }

        addProperty(new Pcr(value));
    }

    public Long getPcr() {
        Pcr pcr = (Pcr) selectConfigProperty(Pcr.class);
        return pcr == null ? null : pcr.getValue();
    }

    public void setMcr(Long value) {
        if (value == null) {
            throw new NullArgumentIsNotAllowedException();
        }

        addProperty(new Mcr(value));
    }

    public Long getMcr() {
        Mcr mcr = (Mcr) selectConfigProperty(Mcr.class);
        return mcr == null ? null : mcr.getValue();
    }

    public synchronized void setAtmQos(AtmQosType qos) {
        if (qos == null) {
            throw new NullArgumentIsNotAllowedException();
        }
        addProperty(new AtmQos(qos));
    }

    public synchronized AtmQosType getAtmQos() {
        AtmQos qos = (AtmQos) selectConfigProperty(AtmQos.class);
        return qos == null ? null : qos.getType();
    }

    public synchronized String getAalType() {
        return this.aalType;
    }

    public synchronized void setAalType(String type) {
        this.aalType = type;
    }
}