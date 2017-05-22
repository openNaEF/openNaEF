package voss.model;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class VlanModelUtils {

    private VlanModelUtils() {
    }

    public static Object[] arraycopy(Object[] src) {
        Class<?> componentType = src.getClass().getComponentType();
        if (componentType.isArray()) {
            throw new IllegalArgumentException("nested array is not supported.");
        }

        int length = src.length;

        Object copy = Array.newInstance(componentType, length);
        System.arraycopy(src, 0, copy, 0, length);
        return (Object[]) copy;
    }

    public static Object[] arrayaddNoDuplicate(Object[] src, Object o) {
        if (containsComparedByEquals(src, o)) {
            return src;
        }

        Object[] dst = (Object[]) Array
                .newInstance(src.getClass().getComponentType(), src.length + 1);
        System.arraycopy(src, 0, dst, 0, src.length);
        dst[dst.length - 1] = o;
        return dst;
    }

    public static Object[] arrayadd(Object[] src, Object[] values) {
        Object[] result = (Object[]) Array
                .newInstance(src.getClass().getComponentType(), src.length + values.length);
        System.arraycopy(src, 0, result, 0, src.length);
        System.arraycopy(values, 0, result, src.length, values.length);
        return result;
    }

    public static boolean containsComparedByEquals(Object[] array, Object o) {
        for (int i = 0; i < array.length; i++) {
            if (equals(array[i], o)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsCompareAsSame(Object[] array, Object o) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == o) {
                return true;
            }
        }
        return false;
    }

    public static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    public static void sortByIfname(Port[] ports) {
        Arrays.sort(ports, new PortIfnameComparator());
    }

    public static <T extends Port> void sortByIfname(List<T> ports) {
        Collections.sort(ports, new PortIfnameComparator());
    }

    public static void sortVlanIf(Port[] vlanIfs) {
        Arrays.sort(vlanIfs, getVlanIfComparator());
    }

    public static void sortEthernetPort(Port[] ethernetPorts) {
        Arrays.sort(ethernetPorts, getPhysicalPortComparator());
    }

    public static void sortLogicalEthernetPort(Port[] ethernetPorts) {
        Arrays.sort(ethernetPorts, getLogicalEthernetPortComparator());
    }

    public static <T extends Port> void sortVlanIf(List<T> vlanIfs) {
        Collections.sort(vlanIfs, getVlanIfComparator());
    }

    public static <T extends Port> void sortPhysicalPort(List<T> physicalPorts) {
        Collections.sort(physicalPorts, getPhysicalPortComparator());
    }

    public static <T extends Port> void sortLogicalEthernetPort(List<T> ethernetPorts) {
        Collections.sort(ethernetPorts, getLogicalEthernetPortComparator());
    }


    public static void sortPseudoWirePort(Port[] pseudoWirePorts) {
        Arrays.sort(pseudoWirePorts, getPseudoWirePortComparator());
    }

    public static <T extends Port> void sortPseudoWirePort(List<T> pseudoWirePorts) {
        Collections.sort(pseudoWirePorts, getPseudoWirePortComparator());
    }

    private static Comparator<Port> getPhysicalPortComparator() {
        return new Comparator<Port>() {
            public int compare(Port o1, Port o2) {
                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o2 == null) {
                    return 1;
                } else if (o1 == null) {
                    return -1;
                }
                if (o1 == o2) {
                    return 0;
                }
                if (o1 instanceof PhysicalPort && o2 instanceof PhysicalPort) {
                    PhysicalPort e1 = (PhysicalPort) o1;
                    PhysicalPort e2 = (PhysicalPort) o2;
                    return comparePhysicalPort(e1, e2);
                } else {
                    throw new IllegalArgumentException();
                }
            }
        };
    }

    private static int comparePhysicalPort(PhysicalPort e1, PhysicalPort e2) {
        if (e1 == null && e2 == null) {
            return 0;
        } else if (e2 == null) {
            return 1;
        } else if (e1 == null) {
            return -1;
        }
        if (e1 == e2) {
            return 0;
        }
        int r = 0;
        if (e1.getModule() != null && e2.getModule() != null) {
            int slot1 = 0;
            int slot2 = 0;
            try {
                slot1 = e1.getModule().getSlot().getSlotIndex();
                slot2 = e2.getModule().getSlot().getSlotIndex();
                r = slot1 - slot2;
            } catch (NotInitializedException e) {
                try {
                    slot1 = Integer.parseInt(e1.getModule().getSlot().getSlotId());
                    slot2 = Integer.parseInt(e2.getModule().getSlot().getSlotId());
                    r = slot1 - slot2;
                } catch (NumberFormatException ex) {
                    r = e1.getModule().getSlot().getSlotId().compareTo(e2.getModule().getSlot().getSlotId());
                }
            }
            r = slot1 - slot2;
            if (r != 0) {
                return r;
            }
        } else {
            r = nullTo0(e1.getModule(), 1) - nullTo0(e2.getModule(), 1);
            if (r != 0) {
                return r;
            }
        }
        try {
            r = e1.getPortIndex() - e2.getPortIndex();
        } catch (NotInitializedException e) {
            try {
                Matcher m1 = portPattern.matcher(e1.getIfName());
                Matcher m2 = portPattern.matcher(e2.getIfName());
                if (m1.matches() && m2.matches()) {
                    r = Integer.parseInt(m1.group(1)) - Integer.parseInt(m2.group(1));
                }
            } catch (NumberFormatException nfe) {
                return e1.getIfName().compareTo(e2.getIfName());
            }
        }
        return r;
    }

    static Pattern portPattern = Pattern.compile(".*([0-9]+)");

    private static Comparator<Port> getLogicalEthernetPortComparator() {
        return new Comparator<Port>() {
            public int compare(Port o1, Port o2) {
                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o2 == null) {
                    return 1;
                } else if (o1 == null) {
                    return -1;
                }
                if (o1 == o2) {
                    return 0;
                }
                if (o1 instanceof LogicalEthernetPort && o2 instanceof LogicalEthernetPort) {
                    LogicalEthernetPort e1 = (LogicalEthernetPort) o1;
                    LogicalEthernetPort e2 = (LogicalEthernetPort) o2;
                    return compareLogicalEthernetPort(e1, e2);
                } else {
                    throw new IllegalArgumentException();
                }
            }
        };
    }

    private static Comparator<Port> getPseudoWirePortComparator() {
        return new Comparator<Port>() {
            public int compare(Port o1, Port o2) {
                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o2 == null) {
                    return 1;
                } else if (o1 == null) {
                    return -1;
                }
                if (o1 == o2) {
                    return 0;
                }
                if (o1 instanceof PseudoWirePort && o2 instanceof PseudoWirePort) {
                    PseudoWirePort pw1 = (PseudoWirePort) o1;
                    PseudoWirePort pw2 = (PseudoWirePort) o2;
                    return comparePseudoWirePort(pw1, pw2);
                } else {
                    throw new IllegalArgumentException();
                }
            }
        };
    }

    private static int compareLogicalEthernetPort(LogicalEthernetPort e1, LogicalEthernetPort e2) {
        EthernetPort[] pe1 = e1.getPhysicalPorts();
        EthernetPort[] pe2 = e2.getPhysicalPorts();
        if (pe1 == null && pe2 == null) {
            return 0;
        } else if (pe2 == null) {
            return 1;
        } else if (pe1 == null) {
            return -1;
        }
        if (pe1.length == 0 && pe2.length == 0) {
            return 0;
        } else if (pe1.length == 0) {
            return -1;
        } else if (pe2.length == 0) {
            return 1;
        }
        Arrays.sort(pe1, getPhysicalPortComparator());
        Arrays.sort(pe2, getPhysicalPortComparator());
        return comparePhysicalPort(pe1[0], pe2[0]);
    }

    private static Comparator<Port> getVlanIfComparator() {
        return new Comparator<Port>() {
            public int compare(Port o1, Port o2) {
                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o2 == null) {
                    return 1;
                } else if (o1 == null) {
                    return -1;
                }
                if (o1 == o2) {
                    return 0;
                }
                if (o1 instanceof VlanIf && o2 instanceof VlanIf) {
                    VlanIf v1 = (VlanIf) o1;
                    VlanIf v2 = (VlanIf) o2;
                    return compareVlanIf(v1, v2);
                } else {
                    System.err.println("o1=" + o1.getClass().getCanonicalName());
                    System.err.println("o2=" + o2.getClass().getCanonicalName());
                    throw new IllegalArgumentException();
                }
            }
        };
    }

    private static int compareVlanIf(VlanIf v1, VlanIf v2) {
        int r = notInitializedTo0(v1) - notInitializedTo0(v2);
        return r;
    }

    private static int comparePseudoWirePort(PseudoWirePort pw1, PseudoWirePort pw2) {
        int r = (int) (pw1.getPeerPwId() - pw2.getPeerPwId()) % Integer.MAX_VALUE;
        return r;
    }

    private static int nullTo0(Object o, int ifNotNullValue) {
        if (o == null) {
            return 0;
        } else {
            return ifNotNullValue;
        }
    }

    private static int notInitializedTo0(VlanIf vlanIf) {
        try {
            return vlanIf.getVlanId();
        } catch (NotInitializedException e) {
            return 0;
        }
    }
}