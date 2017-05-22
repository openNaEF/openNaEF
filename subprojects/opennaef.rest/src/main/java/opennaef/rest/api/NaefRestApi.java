package opennaef.rest.api;

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

/**
 * Naef Rest API のResourceとProviderを定義する
 */
@ApplicationPath(NaefRestApi.APPLICATION_PATH)
public class NaefRestApi extends ResourceConfig {
    public static final String APPLICATION_PATH = "/api/v1";
    public static final String RESOURCE_PACKAGE = NaefRestApi.class.getPackage().getName();

    public NaefRestApi() {
        packages(RESOURCE_PACKAGE);
        App.log.info("install package: " + RESOURCE_PACKAGE);

        App.log.info("NaefRestApi installed.");
    }
}

