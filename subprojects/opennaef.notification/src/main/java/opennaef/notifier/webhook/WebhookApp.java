package opennaef.notifier.webhook;

import opennaef.notifier.util.Logs;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.HttpMethodOverrideFilter;

import javax.ws.rs.ApplicationPath;

/**
 * Webhook Application
 */
@ApplicationPath(WebhookApp.APPLICATION_PATH)
public class WebhookApp extends ResourceConfig {
    public static final String APPLICATION_PATH = "hooks";
    public static final String RESOURCE_PACKAGE = WebhookApp.class.getPackage().getName();

    public WebhookApp() {
        packages(RESOURCE_PACKAGE);
        Logs.common.info("install package: {}", RESOURCE_PACKAGE);

        register(HttpMethodOverrideFilter.class);
        Logs.common.info("install filter: {}", HttpMethodOverrideFilter.class.getSimpleName());

        Logs.common.info("{} installed.", WebhookApp.class.getSimpleName());
    }
}
