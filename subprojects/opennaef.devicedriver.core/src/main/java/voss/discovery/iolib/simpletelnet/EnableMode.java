package voss.discovery.iolib.simpletelnet;

import voss.discovery.iolib.console.ConsoleClient;
import voss.discovery.iolib.console.ConsoleException;

import java.io.IOException;

public class EnableMode implements ModeChanger {
    private static final long serialVersionUID = 1L;
    public static final String MODE_NAME = ConsoleClient.MODE_ENABLE;

    @Override
    public void changeMode(ConsoleClient client) throws IOException, ConsoleException {
        client.execute(MODE_NAME);
    }

    @Override
    public String getModeName() {
        return MODE_NAME;
    }
}