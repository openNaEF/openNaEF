package voss.discovery.iolib.simpletelnet;

import voss.discovery.iolib.console.ConsoleClient;
import voss.discovery.iolib.console.ConsoleException;

import java.io.IOException;

public class GlobalMode implements ModeChanger {
    private static final long serialVersionUID = 1L;
    public static final String MODE_NAME = "enable";

    @Override
    public void changeMode(ConsoleClient client) throws IOException, ConsoleException {
        client.execute("enable");
    }

    @Override
    public String getModeName() {
        return MODE_NAME;
    }

}