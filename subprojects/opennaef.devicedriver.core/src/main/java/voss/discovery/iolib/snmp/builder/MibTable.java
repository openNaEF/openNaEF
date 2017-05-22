package voss.discovery.iolib.snmp.builder;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import net.snmp.VarBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.AbstractWalkProcessor;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpEntry;
import voss.discovery.iolib.snmp.SnmpHelper;
import voss.discovery.iolib.snmp.SnmpUtil.EntryBuilder;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.utils.ByteArrayUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class MibTable {
    private static final Logger log = LoggerFactory.getLogger(MibTable.class);
    private final SnmpAccess snmp;
    private final String name;
    private final String baseOid;
    private final List<TableColumn> tableColumns = new ArrayList<TableColumn>();
    private Map<KeyHolder, TableRow> rows = new HashMap<KeyHolder, TableRow>();
    private boolean abortOnException = false;

    public MibTable(SnmpAccess snmp, String name, String baseOid) {
        this.snmp = snmp;
        this.name = name;
        this.baseOid = baseOid;
    }

    public void setAbortOnException(boolean value) {
        this.abortOnException = value;
    }

    public boolean isAbortOnException() {
        return this.abortOnException;
    }

    public String getTableName() {
        return this.name;
    }

    public <T extends SnmpEntry> void addColumn(String suffix, String name) {
        assert suffix != null;

        String oid = this.baseOid + "." + suffix;
        for (TableColumn column : this.tableColumns) {
            if (column.oid.equals(oid)) {
                log.error("duplicated key.");
                throw new IllegalArgumentException("duplicated key: " + suffix + ":" + name);
            }
        }
        TableColumn newColumn = new TableColumn(this.baseOid + "." + suffix, name);
        this.tableColumns.add(newColumn);
    }

    public TableColumn getColumn(String oid) {
        for (TableColumn col : this.tableColumns) {
            if (col.oid.equals(oid)) {
                return col;
            }
        }
        return null;
    }

    public String getColumnName(String oid) {
        for (TableColumn col : this.tableColumns) {
            if (col.oid.equals(oid)) {
                return col.displayName;
            }
        }
        return null;
    }

    public void walk() throws IOException, AbortedException {
        this.rows.clear();
        for (TableColumn column : this.tableColumns) {
            final String oid = column.oid;
            try {
                AbstractWalkProcessor<Map<KeyHolder, TableRow>> walker
                        = new AbstractWalkProcessor<Map<KeyHolder, TableRow>>(this.rows) {
                    private static final long serialVersionUID = 1L;

                    public void process(VarBind varbind) {
                        SnmpEntry entry = new SnmpEntry(oid, varbind);
                        KeyHolder key = new KeyHolder(entry.oidSuffix);
                        TableRow row = result.get(key);
                        if (row == null) {
                            row = new TableRow(baseOid, key);
                            result.put(key, row);
                        }
                        row.addColumnValue(oid, varbind);
                        walkerlog.trace("walk():" + oid + ":" + key + "->"
                                + new String(entry.value)
                                + "(" + ByteArrayUtil.byteArrayToHexString(entry.value) + ")");
                    }
                };
                this.snmp.walk(column.oid, walker);
                this.rows = walker.getResult();
            } catch (SnmpResponseException e) {
                if (this.abortOnException) {
                    throw new IOException(e);
                } else {
                    log.warn("Exception in walk. Trying next...", e);
                }
            } catch (RepeatedOidException e) {
                if (this.abortOnException) {
                    throw new IOException(e);
                } else {
                    log.warn("Exception in walk. Trying next...", e);
                }
            }
        }
    }

    public List<TableRow> getRows() {
        List<TableRow> rows = new ArrayList<TableRow>();
        for (Map.Entry<KeyHolder, TableRow> entry : this.rows.entrySet()) {
            rows.add(entry.getValue());
        }
        Collections.sort(rows);
        return rows;
    }

    public Map<KeyHolder, TableRow> getKeyAndRows() {
        Map<KeyHolder, TableRow> rows = new HashMap<KeyHolder, TableRow>();
        for (Map.Entry<KeyHolder, TableRow> entry : this.rows.entrySet()) {
            rows.put(entry.getKey(), entry.getValue());
        }
        return rows;
    }

    public static class TableColumn {
        public final String displayName;
        public final String oid;

        public TableColumn(String oid, String name) {
            this.displayName = name;
            this.oid = oid;
        }
    }

    public static class TableRow implements Comparable<TableRow> {
        public final String baseOid;
        public final KeyHolder key;
        public final Map<String, VarBind> columns = new HashMap<String, VarBind>();

        public TableRow(String baseOid, KeyHolder key) {
            this.baseOid = baseOid;
            this.key = key;
        }

        public void addColumnValue(String oid, VarBind varbind) {
            columns.put(oid, varbind);
        }

        public <T extends SnmpEntry> T getColumnValue(String suffix, EntryBuilder<T> builder) {
            String oid = baseOid + "." + suffix;
            VarBind vb = columns.get(oid);
            if (vb == null) {
                return null;
            }
            return builder.buildEntry(oid, vb);
        }

        public <T extends SnmpEntry> T getColumnValueByOid(String oid, EntryBuilder<T> builder) {
            VarBind vb = columns.get(oid);
            if (vb == null) {
                return null;
            }
            return builder.buildEntry(oid, vb);
        }

        public String getValue(String oid) {
            VarBind vb = columns.get(oid);
            if (vb == null) {
                return null;
            }
            int type = (int) vb.getType();
            switch (type) {
                default:
                    StringSnmpEntry stringEntry = getColumnValueByOid(oid, SnmpHelper.stringEntryBuilder);
                    return stringEntry.getValue();
            }
        }

        public List<String> getColumnOids() {
            List<String> result = new ArrayList<String>();
            result.addAll(this.columns.keySet());
            return result;
        }

        public KeyHolder getKey() {
            return this.key;
        }

        @Override
        public int compareTo(TableRow other) {
            if (other == null) {
                return -1;
            } else if (other == this) {
                return 0;
            }
            return this.key.compareTo(other.key);
        }
    }

    public static class KeyHolder implements Comparable<KeyHolder> {
        public final BigInteger[] key;

        public KeyHolder(BigInteger[] key) {
            this.key = key;
        }

        public int intValue(int index) {
            return this.key[index].intValue();
        }

        @Override
        public int compareTo(KeyHolder other) {
            if (other == null) {
                return -1;
            }
            int length = Math.min(this.key.length, other.key.length);
            for (int i = 0; i < length; i++) {
                BigInteger _b1 = this.key[i];
                BigInteger _b2 = other.key[i];
                if (_b1.longValue() < _b2.longValue()) {
                    return -1;
                } else if (_b1.longValue() > _b2.longValue()) {
                    return 1;
                }
            }
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof KeyHolder)) {
                return false;
            }
            if (((KeyHolder) o).key.length != this.key.length) {
                return false;
            }
            for (int i = 0; i < this.key.length; i++) {
                BigInteger bi = ((KeyHolder) o).key[i];
                if (bi.longValue() != this.key[i].longValue()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return getKey().hashCode();
        }

        public String getKey() {
            String key = "KEY=";
            for (BigInteger bi : this.key) {
                key = key + bi.longValue() + ":";
            }
            return key;
        }

        @Override
        public String toString() {
            return getKey();
        }
    }

}