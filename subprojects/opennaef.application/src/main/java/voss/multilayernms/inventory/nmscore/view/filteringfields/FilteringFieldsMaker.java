package voss.multilayernms.inventory.nmscore.view.filteringfields;

import jp.iiga.nmt.core.expressions.EqualsMatcher;
import jp.iiga.nmt.core.expressions.IMatcher;
import jp.iiga.nmt.core.expressions.NumberMatcher;
import jp.iiga.nmt.core.expressions.TextMatcher;
import net.phalanx.core.expressions.FilterQueryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.config.INmsCoreInventoryObjectConfiguration;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.inventory.constants.INVENTORY_FIELD_NAME;
import voss.multilayernms.inventory.nmscore.view.list.ListViewMaker;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class FilteringFieldsMaker {

    static final String EDITOR_NAME_OF_COMBOBOX = "org.eclipse.swt.widgets.Combo";

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(ListViewMaker.class);

    public FilteringFieldsMaker() {
    }

    protected abstract INmsCoreInventoryObjectConfiguration getConfig() throws IOException;

    public List<FilterQueryContext> makeFilteringFieldsView() throws RemoteException, IOException, ExternalServiceException {
        List<FilterQueryContext> result = new ArrayList<FilterQueryContext>();

        List<String> fields = getConfig().getFilteringFields();
        Properties conditions = getConfig().getConditionOfFiltering();
        Properties displayNames = getConfig().getDisplayNames();

        for (String field : fields) {
            FilterQueryContext context = new FilterQueryContext();
            context.setMatcherClass(getMatcherClass(conditions.getProperty(field)));
            context.setDisplayName(displayNames.getProperty(field));
            context.setName(field);

            context.setEditorName(getEditorName(conditions.getProperty(field)));
            if (context.getEditorName() != null) {
                setProposals(field, context);
            }

            result.add(context);
        }

        return result;
    }

    protected List<String> getFacilityStatusList() throws ExternalServiceException, IOException {
        return MplsNmsInventoryConnector.getInstance().getFacilityStatusList();
    }

    protected Class<? extends IMatcher> getMatcherClass(String condition) {
        if (condition.equals("contain")) {
            return TextMatcher.class;
        }
        if (condition.equals("select")) {
            return EqualsMatcher.class;
        }
        if (condition.equals("equal")) {
            return EqualsMatcher.class;
        }
        if (condition.equals("numeric")) {
            return NumberMatcher.class;
        }

        throw new IllegalArgumentException("unknown condition :" + condition);
    }

    protected String getEditorName(String condition) {
        if (condition.equals("select")) {
            return EDITOR_NAME_OF_COMBOBOX;
        } else {
            return null;
        }
    }

    protected void setProposals(String field, FilterQueryContext context) throws RemoteException, IOException, ExternalServiceException {
        List<String> constants = null;

        if (field.equals(INVENTORY_FIELD_NAME.FACILITY_STATUS)) {
            constants = getFacilityStatusList();
        } else {

            if (getConfig().getOperationStatusFieldNames().contains(field)) {
                constants = MplsNmsInventoryConnector.getInstance().getConstants(INVENTORY_FIELD_NAME.OPERATION_STATUS);
            } else {
                constants = MplsNmsInventoryConnector.getInstance().getConstants(field);
            }
        }

        if (constants.isEmpty()) {
            context.setProposals(new String[]{"N/A"});
        } else {
            context.setProposals(constants.toArray(new String[0]));
        }
    }


}