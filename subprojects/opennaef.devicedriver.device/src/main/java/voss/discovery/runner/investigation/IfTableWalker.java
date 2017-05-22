package voss.discovery.runner.investigation;

import voss.discovery.agent.mib.InterfaceMib;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.RealDeviceAccessFactory;
import voss.discovery.iolib.snmp.builder.MibTable;
import voss.discovery.iolib.snmp.builder.MibTable.KeyHolder;
import voss.discovery.iolib.snmp.builder.MibTable.TableRow;
import voss.discovery.runner.simple.DynamicStatusAgent;
import voss.model.NodeInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IfTableWalker {

    public static void main(String[] args) {
        try {
            NodeInfo nodeinfo = DynamicStatusAgent.createNodeInfo(args);
            RealDeviceAccessFactory factory = new RealDeviceAccessFactory();
            DeviceAccess access = factory.getDeviceAccess(nodeinfo);
            MibTable ifTable = new MibTable(access.getSnmpAccess(), "ifTable", InterfaceMib.ifTable);
            ifTable.addColumn(InterfaceMib.ifConnectorPresent_SUFFIX, "ifConnectorPresent");
            ifTable.addColumn(InterfaceMib.ifOperStatus_SUFFIX, "ifOperStatus");
            MibTable ifXTable = new MibTable(access.getSnmpAccess(), "ifXTable", InterfaceMib.ifXTable);
            ifXTable.addColumn(InterfaceMib.ifName_SUFFIX, "ifName");

            ifTable.walk();
            ifXTable.walk();

            Map<KeyHolder, TableRow> map = ifTable.getKeyAndRows();
            Map<KeyHolder, TableRow> map2 = ifXTable.getKeyAndRows();
            List<KeyHolder> keys = new ArrayList<KeyHolder>(map.keySet());
            Collections.sort(keys);
            for (KeyHolder key : keys) {
                System.out.println(key.getKey() + ":");
                TableRow row1 = map.get(key);
                for (String oid : row1.getColumnOids()) {
                    String columnName = ifTable.getColumnName(oid);
                    String value = row1.getValue(oid);
                    System.out.println("\t" + columnName + ": " + value);
                }
                TableRow row2 = map2.get(key);
                for (String oid : row2.getColumnOids()) {
                    String columnName = ifXTable.getColumnName(oid);
                    String value = row2.getValue(oid);
                    System.out.println("\t" + columnName + ": " + value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}