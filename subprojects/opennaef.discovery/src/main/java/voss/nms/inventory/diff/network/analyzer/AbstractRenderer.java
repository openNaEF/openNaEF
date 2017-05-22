package voss.nms.inventory.diff.network.analyzer;

import org.slf4j.LoggerFactory;
import voss.core.server.util.Util;
import voss.model.VlanModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractRenderer<T extends VlanModel> implements Renderer {
    public static final String DEVICE_DELIMITER = ":";
    public static final String ELEMENT_DELIMITER = "/";
    private final T model;
    private final String parentID;
    private final String inventoryID;
    private final String parentAbsoluteName;
    private final String absoluteName;
    private final int depth;
    protected final Map<String, String> inventoryIDtoAbsoluteNameMap;

    public AbstractRenderer(T model, String parentID, int depth, Map<String, String> map) {
        if (Util.isNull(model)) {
            throw new IllegalArgumentException();
        } else if (parentID != null && map.get(parentID) == null) {
            throw new IllegalArgumentException();
        }
        this.model = model;
        this.depth = depth;
        this.parentID = parentID;
        this.parentAbsoluteName = map.get(parentID);
        this.inventoryID = setInventoryID(parentID);
        this.absoluteName = buildAbsoluteName(this.parentAbsoluteName);
        map.put(inventoryID, absoluteName);
        this.inventoryIDtoAbsoluteNameMap = map;
        LoggerFactory.getLogger(getClass()).debug("[Network] inventory-id=" + inventoryID + ", absolute-name=" + absoluteName);
    }

    abstract protected String setInventoryID(String parentID);

    abstract protected String buildAbsoluteName(String parentAbsoluteName);

    @Override
    abstract public List<String> getAttributeNames();

    @Override
    public String getID() {
        return this.inventoryID;
    }

    @Override
    public T getModel() {
        return this.model;
    }

    @Override
    public String getParentID() {
        return this.parentID;
    }

    @Override
    public String getAbsoluteName() {
        return this.absoluteName;
    }

    @Override
    public String getParentAbsoluteName() {
        return this.parentAbsoluteName;
    }

    protected String getAbsoluteNameByInventoryID(String inventoryID) {
        return this.inventoryIDtoAbsoluteNameMap.get(inventoryID);
    }

    @Override
    abstract public String getValue(String attrName);

    @Override
    abstract public String getValue(Enum<?> attr);

    @Override
    public List<String> getValues(Enum<?> attr) {
        return new ArrayList<String>();
    }

    @Override
    public Map<String, String> getMap(Enum<?> attr) {
        return new HashMap<String, String>();
    }

    @Override
    abstract public boolean isRoot();

    public int getDepth() {
        return this.depth;
    }
}