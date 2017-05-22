package voss.core.server.aaa;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.config.ServiceConfiguration;
import voss.core.server.config.VossConfigException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.util.*;

public class AAAConfiguration extends ServiceConfiguration {
    private static final Logger log = LoggerFactory.getLogger(AAAConfiguration.class);

    public static final String AAA_LOG_NAME = "AAALog";
    private static final Logger authLog = LoggerFactory.getLogger(AAA_LOG_NAME);

    public static final String NAME = "AAA";
    public static final String DESCRIPTION = "Authentication, Authorization, Accounting.";
    public static final String FILE_NAME = "AAAConfiguration.xml";

    private static final String KEY_AAA_ENABLE = "aaaEnable";
    private static final String KEY_AAA_SERVER_URL = "serverURL";
    private static final String KEY_HOST_USER_LIST = "hostUserList.user";
    private static final String KEY_IP_ADDRESS_PART = "[@address]";
    private static final String KEY_DISPLAY_NAME_PART = "[@displayName]";
    private static final String KEY_OU_NAME_PART = "[@ou]";

    private static final String KEY_NAME_ATTRIBUTE = "[@name]";
    private static final String KEY_TYPE_ATTRIBUTE = "[@type]";

    private static final String KEY_GROUP_NAME = "groups.group";
    private static final String KEY_GROUP_MEMBER_USER = "user";
    private static final String KEY_GROUP_MEMBER_OU = "ou";
    private static final String KEY_GROUP_OPERATION = "operation";

    private boolean initialized = false;

    private static AAAConfiguration instance = null;

    public static AAAConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new AAAConfiguration();
        }
        return instance;
    }

    private Boolean aaaEnable = false;
    private String aAAServerURL = null;
    private List<AAAGroup> aAAGroups = new ArrayList<AAAGroup>();
    private Map<String, AAAUser> hostUserListEntries = new HashMap<String, AAAUser>();

    private AAAConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    public synchronized void reloadConfigurationInner() throws IOException {
        try {
            XMLConfiguration config = new XMLConfiguration();
            config.setDelimiterParsingDisabled(true);
            config.load(getConfigFile());

            Boolean aaaEnable = config.getBoolean(KEY_AAA_ENABLE);
            String aaaServerURL = config.getString(KEY_AAA_SERVER_URL);

            Map<String, AAAUser> hostUserListEntries = new HashMap<String, AAAUser>();
            List<?> users = config.configurationsAt(KEY_HOST_USER_LIST);
            for (int i = 0; i < users.size(); i++) {
                String key = KEY_HOST_USER_LIST + "(" + i + ")";
                String address = config.getString(key + KEY_IP_ADDRESS_PART);
                String name = config.getString(key + KEY_NAME_ATTRIBUTE);
                String displayName = config.getString(key + KEY_DISPLAY_NAME_PART);
                String ou = config.getString(key + KEY_OU_NAME_PART);
                HashSet<String> ous = new HashSet<String>();
                ous.add(ou);
                AAAUser user = new AAAUser(name, ous, displayName, address);
                hostUserListEntries.put(address, user);
            }

            List<AAAGroup> aaaGroups = new ArrayList<AAAGroup>();

            List<?> groups = config.configurationsAt(KEY_GROUP_NAME);
            for (int i = 0; i < groups.size(); i++) {
                String groupKey = KEY_GROUP_NAME + "(" + i + ")";
                HierarchicalConfiguration sub = config.configurationAt(groupKey);
                String groupName = config.getString(groupKey + KEY_NAME_ATTRIBUTE);
                AAAGroup group = new AAAGroup(groupName);

                List<?> groupUsers = sub.configurationsAt(KEY_GROUP_MEMBER_USER);
                for (int j = 0; j < groupUsers.size(); j++) {
                    String key = KEY_GROUP_MEMBER_USER + "(" + j + ")";
                    String userName = sub.getString(key + KEY_NAME_ATTRIBUTE);
                    group.addUser(userName);
                }

                List<?> groupOUs = sub.configurationsAt(KEY_GROUP_MEMBER_OU);
                for (int j = 0; j < groupOUs.size(); j++) {
                    String key = KEY_GROUP_MEMBER_OU + "(" + j + ")";
                    String ou = sub.getString(key + KEY_NAME_ATTRIBUTE);
                    group.addOrganizationalUnit(ou);
                }

                List<?> groupOperations = sub.configurationsAt(KEY_GROUP_OPERATION);
                for (int j = 0; j < groupOperations.size(); j++) {
                    String key = KEY_GROUP_OPERATION + "(" + j + ")";
                    String name = sub.getString(key + KEY_NAME_ATTRIBUTE);
                    String type = sub.getString(key + KEY_TYPE_ATTRIBUTE);
                    boolean allowed = type.equals("allow");
                    AAAOperation op = new AAAOperation(name, allowed);
                    group.addAAAOperation(op);
                }
                aaaGroups.add(group);
            }

            this.aaaEnable = aaaEnable;
            this.aAAServerURL = aaaServerURL;
            this.hostUserListEntries = hostUserListEntries;
            this.aAAGroups = aaaGroups;
            this.initialized = true;
            log.info("reload finished. Now AAA check is " + aaaEnable);
        } catch (ConfigurationException e) {
            log.error(e.getMessage(), e);
            throw new IOException("failed to reload config: " + FILE_NAME);
        }

    }

    public boolean checkAAA(String ipAddress, String operationName) throws ExternalServiceException,
            NotLoggedInException, AuthorizationException {
        if(aaaEnable){
            log("ipAddress: " + ipAddress);
            AAAUser user = authenticate(ipAddress);
            if (user == null) {
                authLog.info("checkAAA(): not logged in: " + ipAddress);
                throw new NotLoggedInException();
            }
            log("login as " + user.getName());
            boolean result = false;
            for (AAAGroup group : this.aAAGroups) {
                boolean member = group.isMember(user);
                if (member) {
                    result = group.isAllowed(operationName);
                    break;
                }
            }
            log("operation '" + operationName + (result ? "' allowed " : " denied")
                    + " to user '" + user.getName() + "'");
            return result;
        }
        return Boolean.TRUE;
    }

    private void log(String msg) {
        long id = Thread.currentThread().getId();
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(id).append("]");
        sb.append(msg);
        authLog.info(sb.toString());
    }

    public AAAUser authenticate(String ipAddress) throws ExternalServiceException {
        AAAUser user = hostUserListEntries.get(ipAddress);
        if (user != null) {
            return user;
        }
        if (ipAddress == null) {
            return null;
        } else if (ipAddress.equals("127.0.0.1")) {
            try {
                Enumeration<NetworkInterface> localNICs = NetworkInterface.getNetworkInterfaces();
                while (localNICs.hasMoreElements()) {
                    NetworkInterface nic = localNICs.nextElement();
                    Enumeration<InetAddress> addresses = nic.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        String ip = addr.getHostAddress();
                        if (ip.equals("127.0.0.1")) {
                            continue;
                        }
                        user = authenticateIpAddress(ip);
                        if (user != null) {
                            return user;
                        }
                    }
                }
                return null;
            } catch (SocketException e) {
                log.error(e.getMessage(), e);
                throw new ExternalServiceException("Cannot determine local IP adress.");
            }
        } else {
            return authenticateIpAddress(ipAddress);
        }
    }

    private AAAUser authenticateIpAddress(String ipAddress) throws ExternalServiceException {
        String url = aAAServerURL + ipAddress;
        log.info("testing AAA URL=" + url);
        AAAUser user = hostUserListEntries.get(ipAddress);
        if (user != null) {
            return user;
        }

        HttpClient httpClient = new HttpClient();
        GetMethod get = new GetMethod(url);
        try {
            int status = httpClient.executeMethod(get);
            if (status != 200) {
                throw new ExternalServiceException("Authentication process has failed on AuthServer. " +
                        "status code = [" + status + "]");
            }

            String loginUser = null;
            String displayName = null;
            Set<String> organizationalUnits = new HashSet<String>();

            Header[] headers = get.getResponseHeaders();
            for (Header header : headers) {
                String name = header.getName();
                if (!name.startsWith("X-")) {
                    continue;
                }
                String value = header.getValue();
                if (name.equals("X-Auth-Response-Status")) {
                    int authStatus = Integer.parseInt(value);
                    if (authStatus != 2) {
                        return null;
                    }
                    continue;
                } else if (name.equals("X-Auth-Login-User")) {
                    loginUser = value;
                } else if (name.equals("X-Auth-Display-Name")) {
                    displayName = value;
                } else if (name.equals("X-Auth-OrganizationalUnitName")) {
                    organizationalUnits.add(value);
                }
            }

            if (loginUser == null) {
                log.error("The response from the authentication server is invalid.");
                StringBuilder sb = new StringBuilder();
                sb.append("reponse=>\r\n");
                for (Header header : headers) {
                    sb.append("Header: ")
                            .append(header.getName())
                            .append("->")
                            .append(header.getValue())
                            .append("\r\n");
                }
                sb.append("Body: " + get.getResponseBodyAsString()).append("\r\n");
                log.error(sb.toString());
                throw new ExternalServiceException("Unexpected response from AuthServer.");
            }
            if (displayName == null) {
                displayName = loginUser;
            }
            user = new AAAUser(loginUser, organizationalUnits, displayName, ipAddress);
            return user;
        } catch (HttpException e) {
            log.error(e.getMessage(), e);
            throw ExceptionUtils.getExternalServiceException(e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public boolean isEnable() {
        return this.aaaEnable;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public static class AAAGroup {
        private final String groupName;
        private final List<String> users = new ArrayList<String>();
        private final List<String> organizationalUnits = new ArrayList<String>();
        private final List<AAAOperation> operations = new ArrayList<AAAOperation>();

        public AAAGroup(String name) {
            this.groupName = name;
        }

        public String getGroupName() {
            return this.groupName;
        }

        public void addUser(String user) {
            if (user == null) {
                log.error("null user not allowd.");
                return;
            }
            if (!users.contains(user)) {
                users.add(user);
            }
        }

        public void addOrganizationalUnit(String ou) {
            if (ou == null) {
                log.error("null organizationalUnit is not allowed.");
                return;
            }
            if (!organizationalUnits.contains(ou)) {
                organizationalUnits.add(ou);
            }
        }

        public void addAAAOperation(AAAOperation operation) {
            operations.add(operation);
        }

        public boolean isMember(AAAUser user) {
            boolean member = isMember(user.getName());
            if (!member) {
                member = isMember(user.getOrganizationalUnits());
            }
            return member;
        }

        private boolean isMember(String userName) {
            if (userName == null) {
                return false;
            }
            for (String user : users) {
                if (user.equals(userName) || userName.matches(user)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isMember(Set<String> usersOu) {
            for (String organizationalUnit : this.organizationalUnits) {
                if (usersOu.contains(organizationalUnit)) {
                    return true;
                }
                for (String ou : usersOu) {
                    if (ou.matches(organizationalUnit)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isAllowed(String operationName) {
            for (AAAOperation op : operations) {
                if (op.matches(operationName)) {
                    return op.isAllowed();
                }
            }
            return false;
        }
    }

    public static class AAAOperation {
        private final boolean allowd;
        private final String operationName;

        public AAAOperation(String operationName, boolean allowed) {
            this.operationName = operationName;
            this.allowd = allowed;
        }

        public boolean isAllowed() {
            return this.allowd;
        }

        public String getOperationName() {
            return this.operationName;
        }

        public boolean matches(String operationName) {
            if (operationName == null) {
                return false;
            }
            return operationName.equals(this.operationName)
                    || operationName.matches(this.operationName);
        }
    }

    @Override
    protected void publishServices() throws RemoteException, IOException, VossConfigException, InventoryException,
            ExternalServiceException {
    }

}