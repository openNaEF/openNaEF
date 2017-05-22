package opennaef.rest.api;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pasaran.CORSResponseFilter;
import pasaran.RestAPIs;
import pasaran.api.KeseranApi;
import pasaran.naef.PasaranNaefService;

import java.util.Objects;

/**
 *
 */
public class App {
    public static final Logger log = LoggerFactory.getLogger(App.class);

    private static final String PARAMETER_NAME = "javax.ws.rs.Application";

    public static void main(String[] args) {
        startNaef();
        startJetty();
    }

    public static void startNaef() {
        System.setProperty("naef.rmi-client-authentication-enabled", "false");

        System.setProperty("voss.mplsnms.rmi-service-name", "mplsnms");
        System.setProperty("running_mode", "console");
        System.setProperty("tef-working-directory",
                Objects.toString(System.getProperty(
                        "tef-working-directory"),
                        "./naef"));
        new PasaranNaefService().start();
    }

    /**
     * Naef Rest API を起動する
     */
    public static void startJetty() {
        HandlerList handlers = new HandlerList();

        // naef api
        ServletContextHandler naefApiHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        naefApiHandler.setContextPath(NaefRestApi.APPLICATION_PATH);
        ServletHolder naefServletHolder = naefApiHandler.addServlet(ServletContainer.class, "/*");
        naefServletHolder.setInitOrder(0);
        naefServletHolder.setInitParameter(PARAMETER_NAME, NaefRestApi.class.getName());
        handlers.addHandler(naefApiHandler);

        // swagger
        ResourceHandler swagger = new ResourceHandler();
        swagger.setResourceBase("swagger-ui");
        swagger.setDirectoriesListed(false);
        ContextHandler swaggerHandler = new ContextHandler("/swagger");
        swaggerHandler.setHandler(swagger);
        handlers.addHandler(swaggerHandler);

        // keseran
        ResourceHandler keseran = new ResourceHandler();
        keseran.setResourceBase("keseran");
        keseran.setDirectoriesListed(false);
        ContextHandler keseranHandler = new ContextHandler("/keseran");
        keseranHandler.setHandler(keseran);
        handlers.addHandler(keseranHandler);

        // pasaran
        ServletContextHandler pasaran = new ServletContextHandler(ServletContextHandler.SESSIONS);
        pasaran.setContextPath("/");
        handlers.addHandler(pasaran);
        ServletHolder servletHolder = pasaran.addServlet(ServletContainer.class, "/*");
        servletHolder.setInitOrder(0);
        RestAPIs.installRestApi(servletHolder,
                CORSResponseFilter.class,
                KeseranApi.class);

        String port = System.getProperty("jetty-port", "2510");
        Server jetty = new Server(Integer.parseInt(port));
        jetty.setHandler(handlers);
        try {
            jetty.start();
            jetty.join();
        } catch (Exception e) {
            log.error("jetty error", e);
            e.printStackTrace();
        } finally {
            jetty.destroy();
        }
    }
}
