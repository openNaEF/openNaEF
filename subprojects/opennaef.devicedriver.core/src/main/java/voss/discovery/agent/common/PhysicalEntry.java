package voss.discovery.agent.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PhysicalEntry {
    public int index;
    public String path = "";
    public int position = -1;
    public EntityType type;
    public int depth = 0;
    public String name;
    public String descr;
    public String hardwareRev;
    public String firmwareRev;
    public String softwareRev;
    public String serialNumber;
    public int ifindex = -1;
    public String physicalName;
    public PhysicalEntry parent = null;
    private final List<PhysicalEntry> children = new ArrayList<PhysicalEntry>();

    public PhysicalEntry(int oidsuffix, EntityType type) {
        this.index = oidsuffix;
        this.type = type;
    }

    public void addChild(Integer index, PhysicalEntry entry) {
        entry.parent = this;
        children.add(entry);
    }

    public List<PhysicalEntry> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void alignDepth() {
        for (PhysicalEntry child : this.children) {
            child.depth = this.depth + 1;
            child.path = this.path + "." + this.index;
            child.alignDepth();
        }
    }

    public String toString() {
        return repeat("\t", this.depth) + "[" + this.path + "." + this.index + "] "
                + "type=" + this.type + " name=" + this.name
                + (physicalName == null ? "" : " physicalName='" + physicalName + "'")
                + (descr == null ? "" : " descr='" + descr + "'")
                + (position == -1 ? "" : " position=" + position + "")
                + (ifindex == -1 ? "" : " ifindex=" + ifindex + "")
                ;
    }

    public String repeat(String str, int times) {
        String result = "";
        for (int i = 0; i < times; i++) {
            result = result + str;
        }
        return result;
    }
}