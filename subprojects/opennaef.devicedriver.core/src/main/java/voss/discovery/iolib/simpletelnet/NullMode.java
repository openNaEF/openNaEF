package voss.discovery.iolib.simpletelnet;

import voss.discovery.iolib.console.ConsoleClient;
import voss.discovery.iolib.console.ConsoleException;

import java.io.IOException;

public class NullMode implements ModeChanger {
    private static final long serialVersionUID = 1L;

    public void changeMode(ConsoleClient client) throws IOException,
            ConsoleException {
    }

    public String getModeName() {
        return "null";
    }

}