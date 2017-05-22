package voss.nms.inventory.diff.network.diffunitbuilder;

import naef.dto.NaefDto;
import voss.core.server.exception.InventoryException;
import voss.model.VlanModel;

import java.io.IOException;
import java.util.Map;

public interface DiffUnitBuilder<T1 extends VlanModel, T2 extends NaefDto> {
    void buildDiffUnits(Map<String, T1> network, Map<String, T2> db) throws IOException, InventoryException;
}