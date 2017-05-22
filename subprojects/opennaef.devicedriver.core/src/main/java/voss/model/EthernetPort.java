package voss.model;

public interface EthernetPort extends PhysicalPort {
    public static final String PORT_TYPE_NAME_GBIC = "GBIC";
    public static final String PORT_TYPE_NAME_SFP = "SFP";
    public static final String PORT_TYPE_NAME_SFP_PLUS = "SFP+";
    public static final String PORT_TYPE_NAME_XENPAK = "XENPAK";
    public static final String PORT_TYPE_NAME_XFP = "XFP";

    @SuppressWarnings("serial")
    public static class Duplex implements ConfigProperty {

        public static class DuplexValue extends VlanModelConstants {
            public static final DuplexValue FULL = new DuplexValue("full");
            public static final DuplexValue HALF = new DuplexValue("half");

            private DuplexValue() {
            }

            private DuplexValue(String id) {
                super(id);
            }
        }

        public static final Duplex AUTO = new Duplex(true, null, "auto");
        public static final Duplex FULL = new Duplex(false, DuplexValue.FULL, "full");
        public static final Duplex HALF = new Duplex(false, DuplexValue.HALF, "half");

        private boolean isAuto_;
        private DuplexValue value_;
        private String remarks_;

        protected Duplex() {
        }

        public Duplex(DuplexValue value) {
            this(false, value, null);
        }

        public Duplex(DuplexValue value, String remarks) {
            this(false, value, remarks);
        }

        private Duplex(boolean isAuto, DuplexValue value, String remarks) {
            if ((isAuto && value != null) || ((!isAuto) && value == null)) {
                throw new IllegalArgumentException();
            }
            isAuto_ = isAuto;
            value_ = value;
            remarks_ = remarks;
        }

        public Duplex(String remarks) {
            this(false, null, remarks);
        }

        public synchronized boolean isAuto() {
            return isAuto_;
        }

        public synchronized DuplexValue getValue() {
            return value_;
        }

        public synchronized String getRemarks() {
            return remarks_;
        }

        @Override
        public int hashCode() {
            return getValue() == null ? 0 : getValue().getId().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (this == o) {
                return true;
            } else if (!(o instanceof Duplex)) {
                return false;
            }
            Duplex another = (Duplex) o;
            return this.isAuto() == another.isAuto()
                    && (VlanModelUtils.equals(this.getValue(), another.getValue()))
                    && (VlanModelUtils.equals(this.getRemarks(), another.getRemarks()));
        }

        @Override
        public String toString() {
            return "Duplex:" + this.value_.getId() + "(" + this.remarks_ + ")";
        }
    }

    @SuppressWarnings("serial")
    public static class AutoNego implements ConfigProperty {

        public static class AutoNegoValue extends VlanModelConstants {

            public static final AutoNegoValue ON = new AutoNegoValue("on");
            public static final AutoNegoValue OFF = new AutoNegoValue("off");

            private AutoNegoValue() {
            }

            private AutoNegoValue(String id) {
                super(id);
            }
        }

        public static final AutoNego ON = new AutoNego(AutoNegoValue.ON, "on");
        public static final AutoNego OFF = new AutoNego(AutoNegoValue.OFF, "off");

        private AutoNegoValue value_;
        private String remarks_;

        @SuppressWarnings("unused")
        private AutoNego() {
        }

        public AutoNego(AutoNegoValue value) {
            this(value, null);
        }

        public AutoNego(AutoNegoValue value, String remarks) {
            value_ = value;
            remarks_ = remarks;
        }

        public synchronized AutoNegoValue getValue() {
            return value_;
        }

        public synchronized String getRemarks() {
            return remarks_;
        }

        @Override
        public int hashCode() {
            return getRemarks() == null ? 0 : getRemarks().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (this == o) {
                return true;
            } else if (!(o instanceof AutoNego)) {
                return false;
            }
            AutoNego another = (AutoNego) o;
            return (VlanModelUtils.equals(this.getValue(), another.getValue()))
                    && (VlanModelUtils.equals(this.getRemarks(), another.getRemarks()));
        }

        @Override
        public String toString() {
            return "AutoNego:" + this.value_.getId() + "(" + this.remarks_ + ")";
        }
    }

    public void setDuplex(Duplex duplex);

    public Duplex getDuplex();

    public void setAutoNego(AutoNego autoNego);

    public AutoNego getAutoNego();

    public Module getModule();

    public String getMacAddress();

    public void setMacAddress(String macAddress);
}