package voss.model;

public class CiscoCatalystEthernetPort extends EthernetPortImpl {
    private static final long serialVersionUID = 1L;

    public static class TrunkPortDynamicStatus extends VlanModelConstants implements ConfigProperty {
        private static final long serialVersionUID = 1L;
        public static final TrunkPortDynamicStatus TRUNKING = new TrunkPortDynamicStatus("trunking");
        public static final TrunkPortDynamicStatus NOT_TRUNKING = new TrunkPortDynamicStatus("not-trunking");

        private TrunkPortDynamicStatus(String id) {
            super(id);
        }
    }

    public CiscoCatalystEthernetPort(Device device, String ifName, String portTypeName) {
        initDevice(device);
        initIfName(ifName);
        setPortTypeName(portTypeName);
    }
}