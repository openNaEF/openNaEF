package voss.model;

import java.util.HashSet;
import java.util.Set;

public class AlaxalaQosFlowListEntry {
    public static final String QOS_CLASS_NORMAL = "";
    public static final String QOS_CLASS_PREMIUM = "premium";
    private static final Set<String> ALL_QOS_CLASSES = new HashSet<String>();

    public static final String DIRECTION_IN = "in";
    public static final String DIRECTION_OUT = "out";
    private static final Set<String> ALL_DIRECTIONS = new HashSet<String>();

    private final String qosFlowListName;
    private final String qosFlowListKey;
    private final int qosFlowListTargetIfIndex;
    private final String qosFlowDirection;
    private final String qosClass;

    {
        ALL_QOS_CLASSES.add(QOS_CLASS_NORMAL);
        ALL_QOS_CLASSES.add(QOS_CLASS_PREMIUM);
        ALL_DIRECTIONS.add(DIRECTION_IN);
        ALL_DIRECTIONS.add(DIRECTION_OUT);
    }

    public AlaxalaQosFlowListEntry(String name, String key, int ifIndex) {
        this.qosFlowListName = name;
        this.qosFlowListKey = key;
        this.qosFlowListTargetIfIndex = ifIndex;
        this.qosFlowDirection = DIRECTION_IN;
        this.qosClass = QOS_CLASS_NORMAL;
    }

    public AlaxalaQosFlowListEntry(String name, String key, int ifIndex, String direction,
                                   String qosClass) {
        assert ALL_DIRECTIONS.contains(direction);
        assert ALL_QOS_CLASSES.contains(qosClass);

        this.qosFlowListName = name;
        this.qosFlowListKey = key;
        this.qosFlowListTargetIfIndex = ifIndex;
        this.qosFlowDirection = direction;
        this.qosClass = qosClass;
    }

    public String getQosFlowListName() {
        return this.qosFlowListName;
    }

    public String getQosFlowListKey() {
        return this.qosFlowListKey;
    }

    public int getQosFlowListTargetIfIndex() {
        return this.qosFlowListTargetIfIndex;
    }

    public String getDirection() {
        return this.qosFlowDirection;
    }

    public String getQosClass() {
        return this.qosClass;
    }

    public String toString() {
        String crlf = "\r\n";
        return "[" + this.qosFlowListName + "]:" + crlf
                + "\tkey=" + this.qosFlowListKey + crlf
                + "\tifIndex=" + this.qosFlowListTargetIfIndex + crlf
                + "\tdirection=" + qosFlowDirection + crlf
                + "\tclass=" + qosClass + crlf;
    }
}