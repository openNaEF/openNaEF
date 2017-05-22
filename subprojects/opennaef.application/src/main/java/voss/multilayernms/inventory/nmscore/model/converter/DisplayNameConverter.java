package voss.multilayernms.inventory.nmscore.model.converter;

import jp.iiga.nmt.core.model.IModel;
import jp.iiga.nmt.core.model.MetaData;
import jp.iiga.nmt.core.model.Model;
import net.phalanx.core.models.TableInput;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.config.INmsCoreInventoryObjectConfiguration;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

public class DisplayNameConverter {

    private final Class<? extends IModel> targetClass;
    private final INmsCoreInventoryObjectConfiguration config;

    public DisplayNameConverter(Class<? extends IModel> targetClass,
                                INmsCoreInventoryObjectConfiguration config) throws IOException {
        this.targetClass = targetClass;
        this.config = config;
    }

    protected Class<? extends IModel> getTargetClass() {
        return targetClass;
    }

    public List<String> getFields() throws IOException {
        return config.getFields();
    }

    public List<String> getPropertyFields() throws IOException {
        return config.getPropertyFields();
    }

    public Properties getDisplayNames() throws IOException {
        return config.getDisplayNames();
    }

    public TableInput convertList(List<? extends IModel> objects) throws InstantiationException,
            IllegalAccessException, RemoteException, IOException, ExternalServiceException, InventoryException {
        TableInput table = new TableInput();

        List<String> displayNames = new ArrayList<String>();
        for (String field : getFields()) {
            displayNames.add(getDisplayNames().getProperty(field));
        }
        table.setShowColumns(displayNames);
        table.setContent(convertModels(objects));

        return table;
    }

    public List<Model> convertModels(List<? extends IModel> objects)
            throws InstantiationException, IllegalAccessException, IOException {
        List<Model> result = new ArrayList<Model>();

        for (IModel obj : objects) {
            result.add(convertModel(obj));
        }
        return result;
    }

    public Model convertModel(IModel obj) throws IOException, InstantiationException, IllegalAccessException {
        Model model = (Model) obj;

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        List<String> displayOrderKeySet = new ArrayList<String>();
        for (String field : getPropertyFields()) {
            properties.put(getDisplayNames().getProperty(field), model.getMetaData().getPropertyValue(field));
            displayOrderKeySet.add(getDisplayNames().getProperty(field));
        }
        MetaData data = new MetaData();
        data.setKeySet(displayOrderKeySet);
        data.setProperties(properties);

        model.setMetaData(data);

        return model;
    }

}