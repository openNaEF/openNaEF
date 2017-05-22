package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.CsvImportQuery;
import voss.multilayernms.inventory.nmscore.csv.CsvImporterFactory;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;
import java.io.IOException;

public class InventoryCsvImportState extends UnificUIViewState {

    public InventoryCsvImportState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            CsvImporterFactory.getCsvImporter(getQuery(context)).commit(context.getUser());

            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }

    private CsvImportQuery getQuery(FlowContext context) throws IOException {
        return (CsvImportQuery) Operation.getTargets(context);
    }

}