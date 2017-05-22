package voss.multilayernms.inventory.web.util;

import java.io.IOException;

public abstract class ServiceCommand {
    private final String description;
    private final String name;

    public ServiceCommand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public abstract void execute() throws IOException;
}