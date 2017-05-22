package voss.discovery.agent.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleQuickMibScanner {
    private final static Logger log = LoggerFactory.getLogger(SimpleQuickMibScanner.class);
    private File file;
    private final String regexp = "[ ]*([A-Za-z0-9]+)([^:]+)::=[ ]*\\{ *([A-Za-z0-9]+) *([0-9]+) *\\}";
    private Pattern pattern = Pattern.compile(regexp);

    private final static String ciscoEntityVendortypeOIDMIB = ".1.3.6.1.4.1.9.12.3";
    private final static String ciscoProducts = ".1.3.6.1.4.1.9.1";
    private final static String ciscoModules = ".1.3.6.1.4.1.9.12";
    private final static String jnxMibs = ".1.3.6.1.4.1.2636";
    private final static String jnxProducts = ".1.3.6.1.4.1.2636.1";

    private final Map<String, String> mibtree = new HashMap<String, String>();
    private final Map<String, String> oidtree = new HashMap<String, String>();

    public SimpleQuickMibScanner(File file) {
        this.file = file;
        mibtree.put("ciscoEntityVendortypeOIDMIB", ciscoEntityVendortypeOIDMIB);
        mibtree.put("ciscoProducts", ciscoProducts);
        mibtree.put("ciscoModules", ciscoModules);
        mibtree.put("jnxMibs", jnxMibs);
        mibtree.put("jnxProducts", jnxProducts);
    }

    public Map<String, String> getOidTree() {
        Map<String, String> result = new TreeMap<String, String>();
        List<String> keys = new ArrayList<String>(oidtree.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            result.put(key, oidtree.get(key));
        }
        return Collections.unmodifiableMap(result);
    }

    public String getEntityName(String oid) {
        String result = oidtree.get(oid);
        if (result == null) {
            return "UNKNOWN";
        }
        return result;
    }

    public void scan() {
        BufferedReader reader = null;
        int read = 0;
        try {
            reader = new BufferedReader(new FileReader(file));
            String buffer = "";
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("--")) {
                    continue;
                }
                buffer = buffer + line;
                if (buffer.endsWith("}")) {
                    parse(buffer);
                    buffer = "";
                    read++;
                } else if (buffer.endsWith(";")) {
                    buffer = "";
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        log.debug("total read: " + read);
        makeOidTree();
    }

    private void makeOidTree() {
        int i = 1;
        oidtree.clear();
        for (String key : mibtree.keySet()) {
            oidtree.put(mibtree.get(key), key);
        }
        List<String> oidlist = new ArrayList<String>();
        oidlist.addAll(oidtree.keySet());
        Comparator<String> comparator = new Comparator<String>() {
            public int compare(String s1, String s2) {
                return s1.compareTo(s2);
            }
        };
        Collections.sort(oidlist, comparator);
        for (String oid : oidlist) {
            log.trace("[" + i++ + "]" + oid + "->" + oidtree.get(oid));
        }
    }

    private void parse(String entry) {
        Matcher matcher = pattern.matcher(entry);
        if (matcher.matches()) {
            String mibobject = matcher.group(1);
            String mibparent = matcher.group(3);
            String suffix = matcher.group(4);

            String oid = mibtree.get(mibparent);
            if (oid == null) {
                throw new IllegalStateException(entry);
            }
            oid = oid + "." + suffix;
            if (mibtree.get(oid) != null) {
                throw new IllegalStateException("duplicated entry: " + oid);
            }
            mibtree.put(mibobject, oid);
        }
    }

    public void writeFile(String filename, Map<String, String> map) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filename));
            for (String key : map.keySet()) {
                writer.write(key + "\t" + map.get(key) + "\r\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        String base = "E:/Info/MIB/All";

        File mib = new File(base, "CISCO-ENTITY-VENDORTYPE-OID-MIB-V1SMI.txt");
        SimpleQuickMibScanner scanner = new SimpleQuickMibScanner(mib);
        scanner.scan();
        Map<String, String> oidtree = scanner.getOidTree();
        scanner.writeFile("./config/CISCO_ENTITY_MAP.txt", oidtree);

        mib = new File(base, "CISCO-PRODUCTS-MIB-V1SMI.txt");
        scanner = new SimpleQuickMibScanner(mib);
        scanner.scan();
        oidtree = scanner.getOidTree();
        scanner.writeFile("./config/CISCO_PRODUCT_LIST.txt", oidtree);

        mib = new File(base, "mib-jnx-chas-defines.txt");
        scanner = new SimpleQuickMibScanner(mib);
        scanner.scan();
        oidtree = scanner.getOidTree();
        scanner.writeFile("./config/JUNIPER_ENTITY_MAP.txt", oidtree);
    }
}