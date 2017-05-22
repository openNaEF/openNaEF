package voss.multilayernms.inventory.nmscore.csv;

import jp.iiga.nmt.core.model.CsvImportQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.model.converter.DisplayNameConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public abstract class CsvImporter {

    protected final Logger log = LoggerFactory.getLogger(CsvImporter.class);

    private final CsvImportQuery query;
    protected final DisplayNameConverter converter;

    public CsvImporter(CsvImportQuery query, DisplayNameConverter converter) {
        this.query = query;
        this.converter = converter;
    }

    public DisplayNameConverter getConverter() {
        return converter;
    }

    protected List<String> getHeader() {
        return query.getStream().get(0);
    }

    protected List<List<String>> getRecords() {
        List<List<String>> stream = query.getStream();
        return query.getStream().subList(1, stream.size());
    }

    protected List<String> getFieldNames() throws IOException {
        List<String> fields = new ArrayList<String>();

        for (String name : getHeader()) {
            String field = getFieldName(name);
            if (field == null) {
                throw new IllegalArgumentException();
            }
            fields.add(field);
        }

        return fields;
    }

    private String getFieldName(String name) throws IOException {
        for (Entry<Object, Object> entry : converter.getDisplayNames().entrySet()) {
            String viewName = (String) entry.getValue();
            String field = (String) entry.getKey();
            if (name.equals(viewName)) {
                return field;
            }
        }

        return null;
    }

    public abstract void commit(String editorName) throws IOException, InventoryException, ExternalServiceException;
}