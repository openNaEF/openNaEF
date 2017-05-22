package voss.discovery.iolib.snmp;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpClient;
import net.snmp.SnmpResponseException;
import net.snmp.VarBind;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.Access;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

public interface SnmpAccess extends Access {

    SnmpClient getSnmpClient();

    void clearCache();

    void setCommunityString(String community);

    String getCommunityString();

    void setSnmpTimeoutSeconds(int sec);

    int getSnmpTimeoutSeconds();

    void setSnmpRetry(int retry);

    int getSnmpRetry();

    VarBind get(String oid) throws SocketTimeoutException, SocketException,
            IOException, SnmpResponseException, AbortedException;

    Map<String, VarBind> multiGet(String[] oids) throws SocketTimeoutException,
            SocketException, IOException, SnmpResponseException,
            AbortedException;

    VarBind getNextChild(String oid) throws AbortedException,
            SocketTimeoutException, SocketException, IOException,
            SnmpResponseException;

    void walk(String oid, final SerializableWalkProcessor walkProcessor)
            throws AbortedException, SocketTimeoutException, SocketException,
            IOException, RepeatedOidException, SnmpResponseException;

    InetSocketAddress getSnmpAgentAddress();

    List<String> getCachedVarbinds();

}