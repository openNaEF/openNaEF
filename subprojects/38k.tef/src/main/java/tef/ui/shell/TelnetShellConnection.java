package tef.ui.shell;

import lib38k.net.telnetd.TelnetConnection;

import java.io.IOException;
import java.io.PrintStream;

public class TelnetShellConnection implements ShellConnection {

    private TelnetConnection telnetConnection_;

    public TelnetShellConnection(TelnetConnection telnetConnection) {
        telnetConnection_ = telnetConnection;
    }

    public TelnetConnection getTelnetConnection() {
        return telnetConnection_;
    }

    public String readLine() throws IOException {
        return telnetConnection_.readLine();
    }

    public PrintStream getOut() {
        return telnetConnection_.getOut();
    }

    public void setEchoEnabled(boolean value) {
        telnetConnection_.setEchoEnabled(value);
    }

    public void resetCommandHistory() {
        telnetConnection_.resetCommandHistory();
    }

    public void close() throws IOException {
        telnetConnection_.close();
    }
}
