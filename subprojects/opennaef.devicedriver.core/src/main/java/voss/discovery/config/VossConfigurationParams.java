package voss.discovery.config;

import static voss.discovery.config.ParamDataType.INT;
import static voss.discovery.config.ParamDataType.STRING;

public enum VossConfigurationParams {
    RMI_REGISTRY_SERVER_HOST(STRING, "localhost"),
    RMI_REGISTRY_SERVER_PORT(INT, Integer.toString(4444)),
    EXCEPTION_SERVER_RMI_PORT(INT, Integer.toString(4441)),
    SESSION_SERVICE_RMI_PORT(INT, Integer.toString(4442)),

    SCHEDULER_SERVICE_RMI_PORT(INT, Integer.toString(4446)),
    SCHEDULER_SESSION_RMI_PORT(INT, Integer.toString(4456)),

    NODEINFO_SERVICE_RMI_PORT(INT, Integer.toString(4447)),
    NODEINFO_SESSION_RMI_PORT(INT, Integer.toString(4457)),

    AGENTMANAGER_SERVICE_RMI_PORT(INT, Integer.toString(4449)),
    AGENTMANAGER_SESSION_RMI_PORT(INT, Integer.toString(4459)),

    INVENTORY_SERVICE_RMI_PORT(INT, Integer.toString(4448)),
    INVENTORY_SESSION_RMI_PORT(INT, Integer.toString(4458)),

    AGENT_SERVICE_RMI_PORT(INT, Integer.toString(4461)),
    AGENT_SESSION_RMI_PORT(INT, Integer.toString(4471)),

    SUPPORTED_SITES(STRING, "DEFAULT"),
    AGENT_THREADS(INT, Integer.toString(10)),
    AGENT_SITE_NAME(STRING, "DEFAULT"),

    SERVER_BASE_DIR(STRING, "."),
    SERVER_MODE(STRING, "public"),

    HTTP_SERVER_PORT(INT, Integer.toString(1226)),
    HTTP_SERVER_LAYOUT(STRING, "jetty-layout.txt"),
    HTTP_SERVER_LOCAL_WEB_ROOT(STRING, "inventory/web/"),
    HTTP_SERVER_LOCAL_WAR_ROOT(STRING, "inventory/war/"),
    HTTP_SERVER_LOCAL_LIB_ROOT(STRING, "inventory/lib/"),
    HTTP_SERVER_RESOURCE_FILENAME(STRING, "voss-discovery-resouce.txt"),
    HTTP_SERVER_DEFAULT_HEADER_INCLUDE(STRING, "header.html"),
    HTTP_SERVER_DEFAULT_FOOTER_INCLUDE(STRING, "footer.html"),
    HTTP_SERVER_DEFAULT_CSS(STRING, "simple.css"),

    DB_BACKUP_DIR(STRING, "backup/"),

    ENTRY_IDENTITY_CLASS_NAME(STRING, "voss.inventory.EntryIdentityAsDeviceIpAddress"),

    AAA_SERVICE_URL(STRING, "http://localhost:12345/"),
    AAA_SERVICE_USE(STRING, "false"),
    AAA_SERVICE_LOGOUT_NOTIFICATION_PORT(INT, Integer.toString(12346)),
    AAA_REQUEST_TIMEOUT_SEC(INT, Integer.toString(10)),

    LINKS_CONFIG(STRING, "links.config"),
    FILE_GENERATION(INT, Integer.toString(0)),

    INVENTORY_BASE_DIR(STRING, "inventory/"),
    FILE_STORE_DIR(STRING, "files/"),
    SNAPSHOT_STORE_DIR(STRING, "snapshots/"),;

    private final ParamDataType type;
    private final String defaultValue;

    private VossConfigurationParams(ParamDataType type, String defaultValue) {
        if (type == null) {
            throw new IllegalArgumentException();
        }
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public ParamDataType getType() {
        return this.type;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }
}