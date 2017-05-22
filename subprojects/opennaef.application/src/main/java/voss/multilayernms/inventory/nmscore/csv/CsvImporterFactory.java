package voss.multilayernms.inventory.nmscore.csv;

import jp.iiga.nmt.core.model.CsvImportQuery;
import jp.iiga.nmt.core.model.Device;
import jp.iiga.nmt.core.model.PhysicalEthernetPort;
import net.phalanx.core.models.Vlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.csv.importer.NodeCsvImporter;
import voss.multilayernms.inventory.nmscore.csv.importer.PortCsvImporter;
import voss.multilayernms.inventory.nmscore.csv.importer.VlanCsvImporter;

import java.io.IOException;

public class CsvImporterFactory {
    private final static Logger log = LoggerFactory.getLogger(CsvImporterFactory.class);

    public static CsvImporter getCsvImporter(CsvImportQuery query) throws IOException {
        String targetClassName = query.getTarget().getName();
        log.debug("getTarget:" + targetClassName);

        if (targetClassName.equals(Device.class.getName())) {
            return new NodeCsvImporter(query);
        } else if (targetClassName.equals(PhysicalEthernetPort.class.getName())) {
            return new PortCsvImporter(query);
        } else if (targetClassName.equals(Vlan.class.getName())) {
            return new VlanCsvImporter(query);
        } else {
            throw new IllegalArgumentException("Target is unknown.");
        }
    }

}
