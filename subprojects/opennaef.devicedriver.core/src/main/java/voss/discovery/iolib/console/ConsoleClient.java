package voss.discovery.iolib.console;

import java.io.IOException;


public interface ConsoleClient {
    public static final String MODE_NOT_LOGIN = "not login";
    public static final String MODE_LOGIN = "login";
    public static final String MODE_ENABLE = "enable";
    public static final String MODE_CONFIG = "configure";

    void login() throws IOException, ConsoleException;

    String changeMode(String mode) throws IOException, ConsoleException;

    String execute(String command) throws IOException, ConsoleException;

    String getPrompt();

    boolean isConnected();

    void breakConnection() throws IOException, ConsoleException;

    void logout() throws IOException, ConsoleException;
}