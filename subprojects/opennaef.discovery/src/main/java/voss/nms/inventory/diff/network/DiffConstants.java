package voss.nms.inventory.diff.network;

public class DiffConstants {
    public static final int vlanIdDepth = 1000;

    public static final int ipSubnetDepth = 2000;

    public static final int nodeDepth = 10000;

    public static final int linkDepth = 20000;

    public static final int lspDepth = 21000;
    public static final int vlanDepth = 22000;
    public static final int pwDepth = 23000;
    public static final int vplsDepth = 24000;
    public static final int vrfDepth = 25000;

    public static final int pipeDepth = 30000;

    public static final int ipIfAndSubnetComplementalDiff = Integer.MAX_VALUE;

    public static final int nodeIpIfCleanUpDiff = Integer.MAX_VALUE - 1;
}