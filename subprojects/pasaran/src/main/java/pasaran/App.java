package pasaran;


import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import pasaran.api.KeseranApi;
import pasaran.naef.PasaranNaefService;

import java.util.Objects;


/**
 * Naefを起動後、Jettyが立ち上がる
 * web フォルダがドキュメントルートとして扱われる
 */
public class App {
    public static void main(String[] args) throws Exception {
        // Naef 設定
        //System.setProperty("naef-rmi-port", "38110");
        System.setProperty("voss.mplsnms.rmi-service-name", "mplsnms");
        System.setProperty("running_mode", "console");
        System.setProperty("tef-working-directory",
                Objects.toString(System.getProperty(
                        "tef-working-directory"),
                        "./naef"));
        startNaef();
        startJetty();
    }

    public static void startNaef() throws Exception {
        // Naef 起動
        new PasaranNaefService().start();
    }

    public static void startJetty() throws Exception {
        // Jetty 起動
        HandlerList handlers = new HandlerList();

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        ServletHolder servletHolder = context.addServlet(ServletContainer.class, "/*");
        servletHolder.setInitOrder(0);
        servletHolder.setInitParameter(
                "com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.sun.jersey.api.container.filter.GZIPContentEncodingFilter");

        // Pasaran API
        RestAPIs.installRestApi(servletHolder,
                CORSResponseFilter.class,
                KeseranApi.class);

        // 静的コンテンツ
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase("web");
        handlers.addHandler(resourceHandler);
        handlers.addHandler(context);

        String port = System.getProperty("jetty-port", "2510");
        Server jetty = new Server(Integer.parseInt(port));
        jetty.setHandler(handlers);
        try {
//            RolloverFileOutputStream os = new RolloverFileOutputStream("logs/yyyy_mm_dd_jetty.log", true);
//            PrintStream logStream = new PrintStream(os);
//            System.setOut(logStream);
//            System.setErr(logStream);
//            Log.getRootLogger().info("JCG Embedded Jetty logging started.", new Object[]{});

            jetty.start();
            jetty.join();
        } finally {
            jetty.destroy();
        }
    }
}
