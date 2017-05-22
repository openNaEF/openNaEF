package voss.discovery.agent.juniper;

public interface JuniperJnxAnatomyMib {
    public static final String juniperMIB = ".1.3.6.1.4.1.2636";
    public static final String jnxMibs = ".1.3.6.1.4.1.2636.3";
    public static final String jnxBoxAnatomy = ".1.3.6.1.4.1.2636.3.1";

    public static final String jnxContainersTable = ".1.3.6.1.4.1.2636.3.1.6";
    public static final String jnxContainersEntry = ".1.3.6.1.4.1.2636.3.1.6.1";

    public static final String jnxBoxDescr = ".1.3.6.1.4.1.2636.3.1.2.0";

    public static final String jnxBoxSerialNo = ".1.3.6.1.4.1.2636.3.1.3.0";

    public static final String jnxContainersView = jnxContainersEntry + ".2";
    public static final String jnxContainersLevel = jnxContainersEntry + ".3";
    public static final String jnxContainersWithin = jnxContainersEntry + ".4";
    public static final String jnxContainersType = jnxContainersEntry + ".5";

    public static final String jnxContentsTable = jnxBoxAnatomy + ".8";
    public static final String jnxContentsEntry = jnxContentsTable + ".1";

    public static final String jnxContentsContainerIndex = jnxContentsEntry + ".1";
    public static final String jnxContentsType = jnxContentsEntry + ".5";
    public static final String jnxContentsDescr = jnxContentsEntry + ".6";
}