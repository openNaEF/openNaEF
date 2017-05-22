package voss.multilayernms.inventory.web.util;

import voss.core.server.config.ServiceConfigRegistry;

import java.io.IOException;


public class ReloadConfigurationCommand extends ServiceCommand {
    public static final String NAME = "Reload Configuration";
    public static final String DESC = "Reload configuration files.";
    public static final String CMD = "reloadConfig";

    public ReloadConfigurationCommand() {
        super(NAME, DESC);
    }

    @Override
    public void execute() throws IOException {
        ServiceConfigRegistry registry = ServiceConfigRegistry.getInstance();
        registry.reloadAll();
    }

}