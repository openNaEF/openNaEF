package voss.discovery.iolib;

import voss.discovery.iolib.console.ConsoleException;

import java.io.IOException;

public interface Access {
    void setMonitor(ProgressMonitor monitor);

    ProgressMonitor getMonitor();

    void close() throws IOException, ConsoleException;
}