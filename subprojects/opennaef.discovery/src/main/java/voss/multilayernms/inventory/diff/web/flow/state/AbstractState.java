package voss.multilayernms.inventory.diff.web.flow.state;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import net.phalanx.compare.core.DiffItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.diff.util.Util;
import voss.multilayernms.inventory.diff.web.flow.FlowContext;
import voss.multilayernms.inventory.diff.web.validator.HttpValidationContext;
import voss.multilayernms.inventory.diff.web.validator.StringValidator;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.DiffUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.zip.GZIPInputStream;

abstract public class AbstractState implements State {

    private final StateId stateId;
    protected static final Logger log = LoggerFactory.getLogger(AbstractState.class);
    protected static final StringValidator categoryStringValidator = new StringValidator("category");

    public AbstractState(StateId stateId) {
        this.stateId = stateId;
    }

    @Override
    public StateId getStateId() {
        return stateId;
    }

    protected DiffCategory getDiffCategoryFromParameter(FlowContext context) throws ServletException {
        HttpValidationContext validationContext = new HttpValidationContext(context);
        String typeString = categoryStringValidator.get(validationContext);
        if (validationContext.containsError()) {
            throw new ServletException(validationContext.getErrorMessages().get(0));
        }
        return Enum.valueOf(DiffCategory.class, typeString.toUpperCase());
    }

    protected void hasLock(DiffCategory category, String userName) throws ServletException {
        String lockUserName = Util.getLockUserName(category);
        if (lockUserName == null || !lockUserName.equalsIgnoreCase(userName)) {
            throw new ServletException("not locked.");
        }
    }

    protected boolean getLock(DiffCategory category, String userName) {
        if (Util.lock(category, userName)) return true;
        String lockUserName = Util.getLockUserName(category);
        return lockUserName.equals(userName);
    }

    @SuppressWarnings("unchecked")
    private List<DiffItem> getDiffItemListFromGZIPStream(FlowContext context) throws IOException {
        GZIPInputStream in = new GZIPInputStream(context.getHttpServletRequest().getInputStream());
        return (List<DiffItem>) new XStream(new DomDriver("UTF-8")).fromXML(in);
    }

    private Map<String, DiffItem> createInventoryIdItemMap(List<DiffItem> list) {
        Map<String, DiffItem> result = new HashMap<String, DiffItem>();
        for (DiffItem item : list) {
            result.put(item.getInventoryId(), item);
        }
        return result;
    }

    protected Map<String, DiffItem> getInventoryIdItemMap(FlowContext context) throws IOException {
        return createInventoryIdItemMap(getDiffItemListFromGZIPStream(context));
    }

    protected boolean hasAllInventoryId(List<DiffUnit> unitList, Set<String> itemIds) {
        List<String> unitIds = new ArrayList<String>();
        for (DiffUnit unit : unitList) {
            unitIds.add(unit.getInventoryID());
        }
        return unitIds.containsAll(itemIds);
    }

    protected void toXMLResponse(FlowContext context, Object obj) throws IOException {
        HttpServletResponse res = context.getHttpServletResponse();
        res.setCharacterEncoding("UTF-8");
        res.setContentType("text/xml; charset=UTF-8");

        XStream stream = new XStream(new DomDriver("UTF-8"));
        stream.toXML(obj, res.getWriter());
    }

}