package voss.multilayernms.inventory.config;

import voss.core.server.config.CoreConfiguration;
import voss.core.server.database.NaefBridge;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class VossInventoryApplicationListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        CoreConfiguration config = CoreConfiguration.getInstance();
        if (!config.isBridgeInstanciated()) {
            System.out.println("naef-bridge is not instanciated. nothing to do.");
            return;
        }
        NaefBridge bridge = config.getBridge();
        try {
            bridge.close();
            System.out.println("naef-bridge closed.");
        } catch (Exception e) {
            System.out.println("naef-bridge close failed. See stacktrace on system err.");
            e.printStackTrace(System.err);
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        System.out.println("listener enabled.");
    }
}