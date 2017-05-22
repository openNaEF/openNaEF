package voss.multilayernms.inventory.nmscore.model.converter;

import jp.iiga.nmt.core.model.IModel;
import jp.iiga.nmt.core.model.MetaData;
import jp.iiga.nmt.core.model.Model;
import net.phalanx.core.models.LabelSwitchedPath;
import net.phalanx.core.models.TableInput;
import net.phalanx.core.models.TableModifyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.config.NmsCoreRsvpLspConfiguration;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.inventory.constants.INVENTORY_FIELD_NAME;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

public class RsvpLspModelDisplayNameConverter extends DisplayNameConverter {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(RsvpLspModelDisplayNameConverter.class);

    private Map<String, Integer> maxHopIpPathFieldSize = new HashMap<String, Integer>();
    private HashSet<String> existNotAvailableIpPathField = new HashSet<String>();

    public RsvpLspModelDisplayNameConverter() throws IOException {
        super(LabelSwitchedPath.class,
                NmsCoreRsvpLspConfiguration.getInstance());
    }

    @Override
    public TableInput convertList(List<? extends IModel> objects) throws RemoteException, InstantiationException, IllegalAccessException, IOException, InventoryException, ExternalServiceException {
        calcuratMaxHopIpPathFieldSize(objects);

        TableInput table = super.convertList(objects);

        TableModifyContext context = new TableModifyContext();
        context.setCellEditorName("org.eclipse.jface.viewers.ComboBoxViewerCellEditor");
        context.setSelections(MplsNmsInventoryConnector.getInstance().getConstants(INVENTORY_FIELD_NAME.FACILITY_STATUS));
        table.putModifyContext(getDisplayNames().getProperty(INVENTORY_FIELD_NAME.FACILITY_STATUS), context);

        return table;
    }

    private void calcuratMaxHopIpPathFieldSize(List<? extends IModel> objects) throws IOException {
        for (IModel obj : objects) {
            Model model = (Model) obj;
            for (String field : NmsCoreRsvpLspConfiguration.getInstance().getHopIpPathFieldsName()) {
                String hopIpAddresses = model.getMetaData().getPropertyValue(field).toString();
                for (String record : hopIpAddresses.split("\n")) {
                    if (record.startsWith("HopSize=")) {
                        String value = record.substring("HopSize=".length());
                        Integer size = Integer.valueOf(value);
                        if (!maxHopIpPathFieldSize.containsKey(field) || maxHopIpPathFieldSize.get(field) < size) {
                            maxHopIpPathFieldSize.put(field, size);
                        }
                        if (size == 0) {
                            existNotAvailableIpPathField.add(field);
                        }
                    }
                }
            }
        }
    }

    @Override
    public Model convertModel(IModel obj) throws IOException, InstantiationException, IllegalAccessException {
        Model model = (Model) obj;

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        HashSet<String> displayOrderKeySet = new LinkedHashSet<String>();
        for (String field : getPropertyFields()) {
            if (NmsCoreRsvpLspConfiguration.getInstance().getHopIpPathFieldsName().contains(field)) {
                String records = model.getMetaData().getPropertyValue(field).toString();
                String headerWithoutCount = null;
                String headerOfNotAvailable = null;
                int count = 0;
                for (String record : records.split("\n")) {
                    if (record.startsWith("HeaderWithoutCount=")) {
                        headerWithoutCount = record.substring("HeaderWithoutCount=".length());
                        headerOfNotAvailable = headerWithoutCount.substring(0, headerWithoutCount.indexOf(":"));
                    } else {
                        int index = record.indexOf("|");
                        if (index != -1) {
                            if (record.startsWith(headerWithoutCount)) {
                                String header = record.substring(0, index);
                                String value = record.substring(index + 1);
                                properties.put(header, value);
                                displayOrderKeySet.add(header);
                                count++;
                            } else {
                                properties.put(headerOfNotAvailable, "N/A");
                                displayOrderKeySet.add(headerOfNotAvailable);
                            }
                        }
                    }
                }

                if (count > 0 && existNotAvailableIpPathField.contains(field)) {
                    properties.put(headerOfNotAvailable, null);
                    displayOrderKeySet.add(headerOfNotAvailable);
                }

                Integer max = (maxHopIpPathFieldSize.containsKey(field) ? maxHopIpPathFieldSize.get(field) : 0);
                while (count < max) {
                    count++;
                    String header = headerWithoutCount + count;
                    properties.put(header, null);
                    displayOrderKeySet.add(header);
                }
            } else {
                properties.put(getDisplayNames().getProperty(field), model.getMetaData().getPropertyValue(field));
                displayOrderKeySet.add(getDisplayNames().getProperty(field));
            }
        }
        MetaData data = new MetaData();
        data.setKeySet(new ArrayList<String>(displayOrderKeySet));
        data.setProperties(properties);

        model.setMetaData(data);

        return model;
    }

}