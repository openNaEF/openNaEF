package voss.discovery.iolib.netconf;

import java.io.IOException;

public interface NetConfClient {
    void connect() throws IOException, NetConfException;

    void disconnect() throws IOException;

    boolean isConnected();

    String get(String msg) throws IOException, NetConfException;

    void put(String msg) throws IOException, NetConfException;
}