package voss.discovery.iolib.snmp.builder;

import net.snmp.SnmpClient;
import voss.discovery.agent.common.IanaIfType;
import voss.discovery.agent.mib.InterfaceMib;
import voss.discovery.iolib.ProgressMonitor;
import voss.discovery.iolib.snmp.SnmpAccessImpl;
import voss.discovery.iolib.snmp.SnmpHelper;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.iolib.snmp.builder.MibTable.TableRow;

import java.net.InetAddress;

public class IfTableWalker {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("IfTableWalker [host] [community]");
            System.exit(1);
        }
        String hostName = args[0];
        String community = args[1];
        try {
            InetAddress addr = InetAddress.getByName(hostName);
            SnmpClient client = new SnmpClient(addr, community.getBytes());
            SnmpAccessImpl snmp = new SnmpAccessImpl(client);
            snmp.setMonitor(new ProgressMonitor());
            MibTable table = new MibTable(snmp, "ifTable", InterfaceMib.ifTable);
            table.addColumn("1", "ifIndex");
            table.addColumn("2", "ifDescr");
            table.addColumn("3", "ifType");
            table.walk();
            for (TableRow row : table.getRows()) {
                System.out.println(row.getKey().getKey());
                IntSnmpEntry ifIndexEntry = row.getColumnValue("1", SnmpHelper.intEntryBuilder);
                System.out.println("\tifIndex: " + ifIndexEntry.intValue());
                StringSnmpEntry ifDescrEntry = row.getColumnValue("2", SnmpHelper.stringEntryBuilder);
                System.out.println("\tifDescr: " + ifDescrEntry.getValue());
                IntSnmpEntry ifTypeEntry = row.getColumnValue("3", SnmpHelper.intEntryBuilder);
                int ifType = ifTypeEntry.intValue();
                IanaIfType type = IanaIfType.valueOf(ifType);
                System.out.println("\tifType: " + type.name() + "(" + ifType + ")");
            }
            System.out.println("-- end of table.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}