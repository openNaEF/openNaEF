package voss.model;

public interface Port extends VlanModel {
    public static final String STATUS_UP = "up";
    public static final String STATUS_DOWN = "down";
    public static final String STATUS_TESTING = "testing";
    public static final String STATUS_UNKNOWN = "unknown";
    public static final String STATUS_DORMANT = "dormant";
    public static final String STATUS_NOT_PRESENT = "not present";
    public static final String STATUS_LOWER_LAYER_DOWN = "lower layer down";

    public Device getDevice();

    public void initDevice(Device device);

    public int getIfIndex() throws NotInitializedException;

    public Integer getRawIfIndex();

    public void initIfIndex(int ifIndex);

    public void initAlternativeIfIndex(int ifIndex);

    public Integer getAlternativeIfIndex();

    public String getIfName();

    public void initIfName(String ifName);

    public Port getAliasSource();

    public void setAliasSource(Port source);

    public boolean isAliasPort();

    public Port getAssociatedPort();

    public void setAssociatedPort(Port associate);

    public Port getAssociatePort();

    public void setAssociatePort(Port associate);

    public boolean isAssociatedPort();

    public String getConfigName();

    public void setConfigName(String name);

    public String getIfDescr();

    public void setIfDescr(String ifDescr);

    public String getStatus();

    public void setStatus(String status);

    public String getAdminStatus();

    public void setAdminStatus(String adminStatus);

    public String getOperationalStatus();

    public void setOperationalStatus(String operationalStatus);

    public String getFullyQualifiedName();

    public ConfigProperty selectConfigProperty(Class<?> targetClass);

    public ConfigProperty[] getConfigProperties();

    public void setBandwidth(Long bandwidth);

    public Long getBandwidth();

    public void setOspfAreaID(String id);

    public String getOspfAreaID();

    public void setIgpCost(int cost);

    public int getIgpCost();
}