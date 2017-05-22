package voss.discovery.iolib.simpletelnet;

import voss.discovery.iolib.console.ConsoleClient;
import voss.discovery.iolib.console.ConsoleException;

import java.io.IOException;

public class ConfigMode implements ModeChanger {
    private static final long serialVersionUID = 1L;
    public static final String MODE_NAME = "configure";

    @Override
    public void changeMode(ConsoleClient client) throws IOException, ConsoleException {
        client.execute("configure");
    }

    @Override
    public String getModeName() {
        return MODE_NAME;
    }

}