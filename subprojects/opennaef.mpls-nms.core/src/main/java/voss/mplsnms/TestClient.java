package voss.mplsnms;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import naef.dto.CustomerInfoDto;
import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.NodeElementDescriptor;
import naef.dto.NodeElementDto;
import naef.dto.NodeElementHierarchy;
import naef.dto.PortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.serial.SerialPortDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsIntegerIdPoolDto;
import naef.dto.vpls.VplsStringIdPoolDto;
import naef.dto.vrf.VrfDto;
import naef.dto.vrf.VrfIntegerIdPoolDto;
import naef.dto.vrf.VrfStringIdPoolDto;
import naef.ui.NaefDtoFacade;
import naef.ui.NaefShellFacade;
import tef.DateTime;
import tef.MVO;
import tef.TransactionId;
import tef.skelton.Attribute;
import tef.skelton.ResolveException;
import tef.skelton.dto.DtoChanges;
import tef.skelton.dto.EntityDto;
import tef.skelton.dto.MvoDtoDesc;
import tef.skelton.dto.MvoOid;

public class TestClient {

    private static MplsnmsRmiServiceFacade serviceFacade__;
    private static NaefDtoFacade dtoFacade__;
    private static NaefShellFacade shellFacade__;

    private static class TestDtoChangeListener 
        extends UnicastRemoteObject
        implements tef.skelton.dto.DtoChangeListener
    {
        public static void main(String[] args) throws Exception {
            if (args.length != 1) {
                System.out.println("args: [rmi server url]");
                return;
            }

            String rmiServerUrl = args[0];
            MplsnmsRmiServiceAccessPoint ap = (MplsnmsRmiServiceAccessPoint) Naming.lookup(rmiServerUrl);
            ap.getServiceFacade().getDtoFacade()
                .addDtoChangeListener("test dto change listener", new TestDtoChangeListener());
        }

        TestDtoChangeListener() throws RemoteException {
        }

        @Override public void transactionCommitted(
            tef.TransactionId txid, Set<EntityDto> newObjs, Set<EntityDto> changedObjs, DtoChanges changes)
        {
            try {
                System.out.println(
                    "transaction:"
                        + txid
                        + ", time:" + changes.getTargetTime()
                        + "("
                        + new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss.SSS").format(new Date(changes.getTargetTime()))
                        + ")");

                System.out.println(" new: " + newObjs.size());
                for (EntityDto dto : newObjs) {
                    System.out.println("  - " + dto.getClass().getName() + " " + getRefStr(dto.getDescriptor()));
                }

                System.out.println(" changed: " + changedObjs.size());
                for (EntityDto dto : changedObjs) {
                    System.out.println("  - " + dto.getClass().getName() + " " + getRefStr(dto.getDescriptor()));
                    for (String attrname : changes.getChangedAttributeNames(dto)) {
                        System.out.println(
                            "    - " + attrname + ": "
                                + getObjectStr(changes.getPreChangeValue(dto, attrname))
                                + " -> "
                                + getObjectStr(dto.getValue(attrname)));
                    }
                }

                System.out.println(" map change:");
                for (EntityDto dto : changedObjs) {
                    for (String attrname : changes.getChangedAttributeNames(dto)) {
                        DtoChanges.MapChange mapchange = changes.getMapChange(dto, attrname);
                        if (mapchange.getAddedKeys().size() == 0
                            && mapchange.getRemovedKeys().size() == 0
                            && mapchange.getAlteredKeys().size() == 0)
                        {
                            continue;
                        }

                        System.out.println("  - added: " + mapchange.getAddedKeys().size());
                        for (Object key : mapchange.getAddedKeys()) {
                            Object value = mapchange.getAddedValue(key);
                            System.out.println("    " + getObjectStr(key) + ":" + getObjectStr(value));
                        }

                        System.out.println("  - removed: " + mapchange.getRemovedKeys().size());
                        for (Object key : mapchange.getRemovedKeys()) {
                            Object value = mapchange.getRemovedValue(key);
                            System.out.println("    " + getObjectStr(key) + ":" + getObjectStr(value));
                        }

                        System.out.println("  - altered: " + mapchange.getAlteredKeys().size());
                        for (Object key : mapchange.getAlteredKeys()) {
                            Object prevalue = mapchange.getAlteredPreValue(key);
                            Object postvalue = mapchange.getAlteredPostValue(key);
                            System.out.println(
                                "    " + getObjectStr(key)
                                    + ": pre-" + getObjectStr(prevalue)
                                    + ": post-" + getObjectStr(postvalue));
                        }
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
                throw new RuntimeException(t);
            }
        }

        private String getObjectStr(Object o) {
            if (o == null) {
                return "null";
            } else if (o instanceof EntityDto.Desc<?>) {
                return getRefStr((EntityDto.Desc<?>) o);
            } else if (o instanceof Collection<?>) {
                Collection<?> elems = (Collection<?>) o;
                SortedSet<String> elemStrs = new TreeSet<String>();
                for (Object elem : elems) {
                    elemStrs.add(getObjectStr(elem));
                }

                StringBuilder result = new StringBuilder();
                for (String elemStr : elemStrs) {
                    result.append(result.length() == 0 ? "" : ",");
                    result.append(elemStr);
                }
                return "{" + result.toString() + "}";
            } else {
                return o.toString();
            }
        }

        private String getRefStr(EntityDto.Desc<?> desc) {
            MvoDtoDesc<?> ref = (MvoDtoDesc) desc;
            return ref.getMvoId()
                + "/" + ref.getVersion() + "-" + ref.getTimestamp()
                + "@" + Long.toString(ref.getTime(), 16);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("args: [rmi server url] [rmc server address] [rmc server port]");
            return;
        }

        String rmiServerUrl = args[0];
        String rmcServerAddress = args[1];
        int rmcServerPort = Integer.parseInt(args[2]);

        lib38k.rmc.RmcClientService rmc
            = new lib38k.rmc.RmcClientService.Remote(null, rmcServerAddress, rmcServerPort);

        MplsnmsRmiServiceAccessPoint ap = (MplsnmsRmiServiceAccessPoint) Naming.lookup(rmiServerUrl);
        serviceFacade__ = ap.getServiceFacade();
        dtoFacade__ = serviceFacade__.getDtoFacade();
        shellFacade__ = serviceFacade__.getShellFacade();

        VlanIdPoolDto vlanidpool = dtoFacade__.getUniqueNameObject(VlanIdPoolDto.class, "vlans");
        if (vlanidpool != null) {
            System.out.println("select id pool users: " + vlanidpool.getOid());
            Set<VlanDto> vlans = dtoFacade__.selectIdPoolUsers(
                vlanidpool,
                Arrays.<NaefDtoFacade.SearchCondition>asList(
                    new NaefDtoFacade.SearchCondition(
                        NaefDtoFacade.SearchMethod.REGEXP,
                        "naef.vlan.id",
                        ".*5"),
                    new NaefDtoFacade.SearchCondition(
                        NaefDtoFacade.SearchMethod.PARTIAL_MATCH,
                        "_LastEditor",
                        "Sys")));
            for (VlanDto vlan : vlans) {
                System.out.println(" - " + vlan.getOid());
            }
        }
        System.out.println("-----");

        for (Attribute<?, ?> attr : dtoFacade__.getDeclaredAttributes(NodeDto.class)) {
            System.out.println(attr.getName() + " " + attr.getType());
        }

        System.out.println("* vrf");
        for (VrfIntegerIdPoolDto pool : dtoFacade__.getRootIdPools(VrfIntegerIdPoolDto.class)) {
            for (VrfDto vrf : pool.getUsers()) {
                System.out.println("  - " + vrf.getOid() + " " + vrf.getIntegerId());
            }
        }
        for (VrfStringIdPoolDto pool : dtoFacade__.getRootIdPools(VrfStringIdPoolDto.class)) {
            for (VrfDto vrf : pool.getUsers()) {
                System.out.println("  - " + vrf.getOid() + " " + vrf.getStringId());
            }
        }

        System.out.println("* vpls");
        for (VplsIntegerIdPoolDto pool : dtoFacade__.getRootIdPools(VplsIntegerIdPoolDto.class)) {
            for (VplsDto vpls : pool.getUsers()) {
                System.out.println("  - " + vpls.getOid() + " " + vpls.getIntegerId());
            }
        }
        for (VplsStringIdPoolDto pool : dtoFacade__.getRootIdPools(VplsStringIdPoolDto.class)) {
            for (VplsDto vpls : pool.getUsers()) {
                System.out.println("  - " + vpls.getOid() + " " + vpls.getStringId());
            }
        }

        System.out.println("* serial port declared attrs:");
        for (Attribute<?, ?> attr : dtoFacade__.getDeclaredAttributes(SerialPortDto.class)) {
            System.out.println(" - " + attr.getName() + " " + attr.getType());
        }
        System.out.println("* port serializable attrs:");
        for (Attribute<?, ?> attr : dtoFacade__.getSerializableAttributes(SerialPortDto.class)) {
            System.out.println(" - " + attr.getName() + " " + attr.getType());
        }

        try {
            shellFacade__.executeBatch(Arrays.<String>asList(
                "context",
                "context",
                "context",
                "hoge",
                "context",
                "context"));
        } catch (NaefShellFacade.ShellException se) {
            dumpShellException(se);
        }

        System.out.println();

        System.out.println("* lag bundle ports:");
        for (NodeDto node : dtoFacade__.getNodes()) {
            System.out.println("  - " + node.getName());
            for (PortDto port : node.getPorts()) {
                if (port instanceof EthLagIfDto) {
                    System.out.println("    - " + port.getNodeLocalName());
                    for (EthPortDto bundlePort : ((EthLagIfDto) port).getBundlePorts()) {
                        System.out.println("      - " + bundlePort.getNodeLocalName());
                    }
                }
            }
        }

        NodeDto nodeA = null;
        for (NodeDto node : dtoFacade__.getNodes()) {
            if (node.getName().equals("Node-A")) {
                nodeA = node;
                break;
            }
        }
        if (nodeA != null) {
            testAttributeHistory(nodeA);
        }

        testMvoVersionSnapshot();

        dumpPortNetworks();

        printRandomNodeelemHierarchy(0.0f);
        printRandomNodeelemHierarchy(0.5f);

        if (nodeA != null) {
            showCustomerInfos(nodeA, "lag-1|vlan-100");
            showCustomerInfos(nodeA, "lag-1");
        }

        System.out.println(
            "* dtofacade.getUniqueNameObject(): " + dtoFacade__.getUniqueNameObject(NodeDto.class, "Node-A"));

        dtoFacade__.addDtoChangeListener("change listener", new TestDtoChangeListener());
    }

    private static void shell(String... commands) {
        try {
            shellFacade__.executeBatch(Arrays.<String>asList(commands));
        } catch (RemoteException re) {
            throw new RuntimeException(re);
        } catch (NaefShellFacade.ShellException se) {
            dumpShellException(se);
        }
    }

    private static void dumpShellException(NaefShellFacade.ShellException se) {
        System.out.println("* batch error:");
        System.out.println("  - line count: " + se.getBatchLineCount());
        System.out.println("  - command: " + se.getCommand());
        System.out.println("  - message: " + se.getMessage());
    }

    private static void testAttributeHistory(NodeDto node) throws Exception {
        System.out.println();

        String ipAddrAttrName = "ip address";
        dumpAttributeHistory(node, ipAddrAttrName);
        setAttribute(node, ipAddrAttrName, "1.2.3.4");
        dumpAttributeHistory(node, ipAddrAttrName);
        setAttribute(node, ipAddrAttrName, "1.2.3.4"); 
        setAttribute(node, ipAddrAttrName, "2.3.4.5");
        setAttribute(node, "vendor name", "aiueo"); 
        setAttribute(node, ipAddrAttrName, "3.4.5.6");
        setAttribute(node, ipAddrAttrName, "1.2.3.4");
        dumpAttributeHistory(node, ipAddrAttrName);
    }

    private static void setAttribute(EntityDto dto, String attrName, String valueStr)
        throws Exception 
    {
        shell(
            "context mvo-id;" + ((MvoOid) dto.getOid()).oid.getLocalStringExpression(),
            "attribute set \"" + attrName + "\" " + valueStr);
    }

    private static void dumpAttributeHistory(EntityDto dto, String attrName) throws Exception {
        SortedMap<TransactionId.W, Object> history
            = dtoFacade__.<Object>getAttributeHistory(((MvoOid) dto.getOid()).oid, attrName);
        System.out.println("* attribute " + attrName + ", history size: " + history.size());
        for (Map.Entry<TransactionId.W, Object> entry : history.entrySet()) {
            System.out.println(" - " + entry.getKey() + " " + entry.getValue());
        }
    }

    private static void testMvoVersionSnapshot() throws Exception {
        RsvpLspDto lsp = (RsvpLspDto) resolveDto("rsvp-lsp.id-pool;,lsp:E-G");
        if (lsp == null) {
            System.out.println("no lsp.");
            return;
        }

        shell(
            "context rsvp-lsp.id-pool;,lsp:E-G",
            "attribute reset naef.enabled-time",
            "context rsvp-lsp-hop-series.id-pool;,id;path:E-C-A-D-G",
            "attribute reset naef.enabled-time");
        lsp.renew();
        TransactionId.W txid1 = ((MvoDtoDesc<?>) lsp.getDescriptor()).getTimestamp();

        shell(
            "context rsvp-lsp-hop-series.id-pool;,id;path:E-C-A-D-G",
            "attribute set naef.enabled-time 2010/01/01");
        lsp.renew();
        TransactionId.W txid2 = ((MvoDtoDesc<?>) lsp.getDescriptor()).getTimestamp();

        shell(
            "context rsvp-lsp-hop-series.id-pool;,id;path:E-C-A-D-G",
            "attribute set naef.enabled-time 2010/01/02");
        lsp.renew();
        TransactionId.W txid3 = ((MvoDtoDesc<?>) lsp.getDescriptor()).getTimestamp();

        lsp = (RsvpLspDto) getMvoDto(lsp.getOid(), txid1);
        assertValue(null, datetimeStr(getEnabledTime(lsp.getHopSeries1())));
        lsp.renew();
        assertValue("2010.01.02-00:00:00", datetimeStr(getEnabledTime(lsp.getHopSeries1())));

        lsp = (RsvpLspDto) getMvoDto(lsp.getOid(), txid2);
        assertValue("2010.01.01-00:00:00", datetimeStr(getEnabledTime(lsp.getHopSeries1())));

        lsp = (RsvpLspDto) getMvoDto(lsp.getOid(), txid3);
        assertValue("2010.01.02-00:00:00", datetimeStr(getEnabledTime(lsp.getHopSeries1())));

        List<String> configuredHops = lsp.getHopSeries1().getConfiguredHops();
        System.out.println("* configured-hops: " + configuredHops.size());
        for (String s : configuredHops) {
            System.out.println("  - " + s);
        }
    }

    private static void assertValue(Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            System.out.println("[ok] " + expected);
        } else {
            throw new RuntimeException("assertion failed: expected=" + expected + ", actual=" + actual);
        }
    }

    private static EntityDto getMvoDto(EntityDto.Oid oid, TransactionId.W txid)
        throws Exception
    {
        return getMvoDto(((MvoOid) oid).oid, txid);
    }

    private static EntityDto getMvoDto(MVO.MvoId mvoid, TransactionId.W txid) throws Exception {
        return dtoFacade__.getMvoDto(mvoid, txid);
    }

    private static DateTime getEnabledTime(EntityDto dto) {
        return getDatetime(dto, "naef.enabled-time");
    }

    private static DateTime getDatetime(EntityDto dto, String attrName) {
        return (DateTime) dto.getValue(attrName);
    }

    private static DateFormat datetimeFormat__ = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss");

    private static String datetimeStr(DateTime datetime) {
        return datetime == null ? null : datetimeFormat__.format(datetime.toJavaDate());
    }

    private static EntityDto resolveDto(String fqn) {
        try {
            return dtoFacade__.getMvoDto(dtoFacade__.resolveMvoId(fqn), null);
        } catch (ResolveException re) {
            return null;
        } catch (RemoteException re) {
            throw new RuntimeException(re);
        }
    }

    private static void dumpPortNetworks() throws Exception {
        System.out.println("port networks:");
        for (NodeDto node : dtoFacade__.getNodes()) {
            for (PortDto port : node.getPorts()) {
                Collection<NetworkDto> networks = port.getNetworks();
                if (networks.size() > 0) {
                    System.out.print(" - " + port.getAbsoluteName() + ": ");
                    for (NetworkDto network : networks) {
                        System.out.print(network.getClass().getSimpleName() + ", ");
                    }
                    System.out.println();
                }
            }
        }
    }

    private static void printRandomNodeelemHierarchy(float threshold) throws Exception {
        System.out.println();
        System.out.println("sampling node element hierarchy: threshold=" + threshold);

        Set<NodeElementDescriptor<?>> descriptors = new HashSet<NodeElementDescriptor<?>>();
        for (NodeDto node : dtoFacade__.getNodes()) {
            for (NodeElementDto elem : getSamplingSubelements(threshold, node)) {
                descriptors.add(new NodeElementDescriptor.MvoId(((MvoOid) elem.getOid()).oid));
            }
        }

        NodeElementHierarchy hierarchy = dtoFacade__.buildNodeElementHierarchy(descriptors);
        for (NodeElementHierarchy.Entry entry : hierarchy.getRootEntries()) {
            printNodeElementHierarchyEntry("", entry);
        }
    }

    private static void printNodeElementHierarchyEntry(String indent, NodeElementHierarchy.Entry entry)
        throws Exception
    {
        System.out.println(indent + "- " + getNodeElementHierarchyEntryStr(entry));
        for (NodeElementHierarchy.Entry subentry : entry.getSubEntries()) {
            printNodeElementHierarchyEntry(indent + "  ", subentry);
        }
    }

    private static String getNodeElementHierarchyEntryStr(NodeElementHierarchy.Entry entry)
        throws Exception
    {
        NodeElementDto dto = (NodeElementDto) getMvoDto(entry.getMvoId(), null);
        return dto.getSqn();
    }

    private static final Random random__ = new Random();

    private static Set<NodeElementDto> getSamplingSubelements(float threshold, NodeElementDto elem) {
        Set<NodeElementDto> result = new HashSet<NodeElementDto>();

        if (random__.nextFloat() >= threshold) {
            result.add(elem);
        }

        for (NodeElementDto subelem : elem.getSubElements()) {
            result.addAll(getSamplingSubelements(threshold, subelem));
        }
        return result;
    }

    private static void showCustomerInfos(NodeDto node, final String portNameRegexp)
        throws Exception 
    {
        Set<PortDto> ports = new HashSet<PortDto>(
            dtoFacade__.getNodePorts(
                node,
                new tef.skelton.Filter<naef.mvo.Port>() {

                    @Override public boolean accept(naef.mvo.Port port) {
                        return port.getName().matches(portNameRegexp);
                    }
                }));
        Set<CustomerInfoDto> customerInfos = dtoFacade__.getCustomerInfos(ports);
        System.out.println("customer infos: " + customerInfos.size());
        for (CustomerInfoDto info : customerInfos) {
            System.out.println("  - " + info.getName());
        }
    }
}
