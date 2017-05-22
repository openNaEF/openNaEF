package voss.discovery.iolib.simpletelnet;

import voss.discovery.iolib.console.ConsoleClient;
import voss.discovery.iolib.console.ConsoleException;

import java.io.IOException;
import java.io.Serializable;

public interface ModeChanger extends Serializable {
    void changeMode(ConsoleClient client) throws IOException, ConsoleException;

    String getModeName();
}