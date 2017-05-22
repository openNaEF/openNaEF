package tef.ui.shell;

import java.io.IOException;
import java.io.PrintStream;

public interface ShellConnection {

    public String readLine() throws IOException;

    public PrintStream getOut();

    public void setEchoEnabled(boolean value);

    public void resetCommandHistory();

    public void close() throws IOException;
}
