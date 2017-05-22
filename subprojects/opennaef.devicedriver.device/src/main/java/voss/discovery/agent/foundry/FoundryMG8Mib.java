package voss.discovery.agent.foundry;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import voss.discovery.agent.mib.EtherLikeMib;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.model.EthernetPort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FoundryMG8Mib extends FoundryCollectMethods {

    public FoundryMG8Mib(SnmpAccess snmp) {
        super(snmp);
    }

    public EthernetPort.Duplex getDuplex(int portIfIndex) throws IOException, AbortedException {
        try {
            Integer entry = SnmpUtil.getInteger(snmp, EtherLikeMib.Dot3StatsDuplexStatusEntry.OID + "." + portIfIndex);
            switch (entry.intValue()) {
                case 1:
                    return null;
                case 2:
                    return EthernetPort.Duplex.HALF;
                case 3:
                    return EthernetPort.Duplex.FULL;
                default: {
                    System.out.println("Unknown foundryPortDuplex value (" + entry.intValue() + ")");
                    return null;
                }
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    protected final String snVLanByPortMemberTagMode = foundryEnterpriseOID
            + ".1.1.3.2.6.1.4";

    public VlanPortBindings[] getVlanPortBindings() throws IOException, AbortedException {
        try {
            ArrayList<VlanPortBindings> result = new ArrayList<VlanPortBindings>();
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, snVLanByPortMemberTagMode);
            for (IntSnmpEntry entry : entries) {
                result.add(new VlanPortBindings(entry));
            }
            return result.toArray(new VlanPortBindings[0]);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }


}