package voss.model.value;

import voss.model.ConfigProperty;
import voss.model.VlanModelUtils;

public abstract class PortSpeedValue implements ConfigProperty {
    private static final long serialVersionUID = 1L;

    public static class Admin extends PortSpeedValue {
        private static final long serialVersionUID = 1L;

        public static final Admin AUTO = new Admin(true, null, "auto");

        private boolean isAuto_;

        @SuppressWarnings("unused")
        private Admin() {
        }

        public Admin(long value) {
            this(false, new Long(value), null);
        }

        public Admin(long value, String remarks) {
            this(false, new Long(value), remarks);
        }

        public Admin(String remarks) {
            this(false, null, remarks);
        }

        private Admin(boolean isAuto, Long value, String remarks) {
            super(value, remarks);
            if ((isAuto && value != null) || ((!isAuto) && value == null)) {
                throw new IllegalArgumentException();
            }

            isAuto_ = isAuto;
        }

        public synchronized boolean isAuto() {
            return isAuto_;
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof Admin)) {
                return false;
            }

            Admin another = (Admin) o;

            return (this.isAuto() == another.isAuto()) && super.equals(another);
        }
    }

    public static class Oper extends PortSpeedValue {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unused")
        private Oper() {
        }

        public Oper(long value) {
            this(new Long(value), null);
        }

        public Oper(long value, String remarks) {
            this(new Long(value), remarks);
        }

        public Oper(String remarks) {
            this(null, remarks);
        }

        private Oper(Long value, String remarks) {
            super(value, remarks);
        }
    }

    private Long value_;
    private String remarks_;

    protected PortSpeedValue() {
    }

    protected PortSpeedValue(long value) {
        this(new Long(value), null);
    }

    protected PortSpeedValue(long value, String remarks) {
        this(new Long(value), remarks);
    }

    protected PortSpeedValue(String remarks) {
        this(null, remarks);
    }

    protected PortSpeedValue(Long value, String remarks) {
        value_ = value;
        remarks_ = remarks;
    }

    public synchronized Long getValue() {
        return value_;
    }

    public Long getValueAsMega() {
        Long value = getValue();
        if (value == null) {
            return null;
        }

        return new Long(value.longValue() / 1000000);
    }

    public synchronized String getRemarks() {
        return remarks_;
    }

    @Override
    public int hashCode() {
        return getValue() == null ? 0 : getValue().intValue();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof PortSpeedValue)) {
            return false;
        } else if (this == o) {
            return true;
        }
        PortSpeedValue another = (PortSpeedValue) o;
        return (VlanModelUtils.equals(this.getValue(), another.getValue()))
                && (VlanModelUtils.equals(this.getRemarks(), another.getRemarks()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append(":");
        sb.append(this.value_.longValue());
        if (this.remarks_ != null) {
            sb.append("(").append(this.remarks_).append(")");
        }
        return sb.toString();
    }
}