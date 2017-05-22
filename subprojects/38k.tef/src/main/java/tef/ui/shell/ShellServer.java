package tef.ui.shell;

import lib38k.logger.Logger;
import lib38k.net.telnetd.TelnetConnection;
import lib38k.net.telnetd.TelnetServer;
import tef.TefService;
import tef.TefServiceConfig;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ShellServer {

    private static ShellServer instance__;

    private int shellPort_;
    private Logger logger_;

    public ShellServer() {
        synchronized (ShellServer.class) {
            if (instance__ != null) {
                throw new IllegalStateException
                        ("multiple shell-service is not supported");
            }

            instance__ = this;
        }

        TefServiceConfig.ShellConfig config
                = TefService.instance().getTefServiceConfig().shellConfig;

        shellPort_ = config.port;
        logger_ = TefService.instance().createLogger("tty");
    }

    public static ShellServer getInstance() {
        return instance__;
    }

    private static class CommandComplementableTelnetConnection extends TelnetConnection {

        private ShellSession shellSession_;

        CommandComplementableTelnetConnection(Socket socket) throws IOException {
            super(socket);
        }

        void setShellSession(ShellSession shellSession) {
            shellSession_ = shellSession;
        }

        @Override
        protected List<String> getCommandCandidates(String currentLine) {
            return shellSession_.getCommandCandidates(currentLine);
        }
    }

    public void start() throws Exception {
        final TelnetServer telnetd = new TelnetServer(shellPort_) {

            @Override
            protected TelnetConnection newTelnetConnection(Socket socket)
                    throws IOException {
                return new CommandComplementableTelnetConnection(socket);
            }
        };

        new Thread() {

            public void run() {
                while (true) {
                    try {
                        CommandComplementableTelnetConnection telnetConnection
                                = (CommandComplementableTelnetConnection)
                                telnetd.getNextConnection();
                        ShellSession session
                                = createSession
                                (new TelnetShellConnection(telnetConnection),
                                        getLogger());
                        telnetConnection.setShellSession(session);
                        session.log
                                ("connection: "
                                        + telnetConnection.getClientAddress().getHostAddress()
                                        + ":" + telnetConnection.getClientPort());

                        session.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        }.start();
    }

    protected ShellSession createSession
            (ShellConnection shellConnection, Logger logger) {
        return new ShellSession(shellConnection, logger);
    }

    Logger getLogger() {
        return logger_;
    }
}
