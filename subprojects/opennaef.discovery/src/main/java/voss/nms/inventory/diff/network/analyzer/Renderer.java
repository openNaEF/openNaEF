package voss.nms.inventory.diff.network.analyzer;

import voss.model.VlanModel;

import java.util.List;
import java.util.Map;

public interface Renderer {
    boolean isRoot();

    String getParentID();

    String getID();

    VlanModel getModel();

    String getAbsoluteName();

    String getParentAbsoluteName();

    List<String> getAttributeNames();

    String getValue(String attrName);

    String getValue(Enum<?> attr);

    List<String> getValues(Enum<?> attr);

    Map<String, String> getMap(Enum<?> attr);

    int getDepth();
}