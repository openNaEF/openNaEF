package voss.discovery.agent.cisco;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;
import com.trilead.ssh2.StreamGobbler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class NexusTest {
    private final static Logger log = LoggerFactory.getLogger(NexusTest.class);
    private OutputStream stdin;
    private Connection connection;
    private Session session;
    private final String ip;
    private WrapperReader reader;
    private BufferedWriter writer;

    public static final String HELLO_MSG = "<?xml version=\"1.0\"?>\r\n"
            + "<nc:hello xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\r\n"
            + "  <nc:capabilities>\r\n"
            + "    <nc:capability>urn:ietf:params:xml:ns:netconf:base:1.0</nc:capability>\r\n"
            + "  </nc:capabilities>\r\n"
            + "</nc:hello>]]>]]>";

    public static final String CLOSE_MSG = "<?xml version=\"1.0\"?>\r\n"
            + "<nc:rpc message-id=\"101\" xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\" "
            + "xmlns=\"http://www.cisco.com/nxos:1.0\">\r\n"
            + "  <nc:close-session/>\r\n"
            + "</nc:rpc>]]>]]>";

    public static final String EXEC_COMMAND_PREFIX = "<?xml version=\"1.0\"?>\r\n"
            + "<nf:rpc message-id=\"110\" xmlns=\"http://www.cisco.com/nxos:1.0:if_manager\" "
            + "xmlns:nf=\"urn:ietf:params:xml:ns:netconf:base:1.0\" "
            + "xmlns:nxos=\"http://www.cisco.com/nxos:1.0\">\r\n"
            + "<nxos:exec-command>\r\n"
            + "  <nxos:cmd>\r\n";

    public static final String EXEC_COMMAND_SUFFIX = "\r\n  </nxos:cmd>\r\n"
            + "</nxos:exec-command>\r\n"
            + "</nf:rpc>]]>]]>";

    public NexusTest(String ip) {
        this.ip = ip;
    }

    public void connect(String userName, String password) throws Exception {
        this.connection = new Connection(ip);
        this.connection.connect();
        log.info("connected: " + ip + ":22");

        boolean isAuthenticated = connection.authenticateWithPassword(userName, password);
        if (isAuthenticated == false) {
            throw new IOException("Authentication failed.");
        }
        this.session = this.connection.openSession();
        this.session.startSubSystem("xmlagent");
        BufferedReader _reader = new BufferedReader(new InputStreamReader(this.session.getStdout()));
        this.reader = new WrapperReader(_reader);
        this.stdin = this.session.getStdin();
        this.writer = new BufferedWriter(new OutputStreamWriter(this.stdin));
        new StreamGobbler(this.session.getStderr());
        log.info("login: " + ip + ":22");
        log.info("server-hello: " + reader.readAll());
    }

    public String execute(String msg, boolean waitForResponse) throws IOException {
        log.info("sending: " + msg);
        this.writer.write(msg);
        this.writer.flush();
        log.info("sent");
        if (waitForResponse) {
            String response = this.reader.readAll().toString();
            log.info("response: " + response);
            return response;
        } else {
            log.info("no response expected.");
            return null;
        }
    }

    public void execCommand(String command) throws IOException {
        String msg = EXEC_COMMAND_PREFIX + command + EXEC_COMMAND_SUFFIX;
        execute(msg, true);
    }

    public void close() throws Exception {
        if (this.writer != null) {
            this.writer.close();
            this.writer = null;
        }
        if (this.reader != null) {
            this.reader.close();
            this.reader = null;
        }
        if (this.session != null) {
            this.session.close();
            this.session = null;
        }
        if (this.connection != null) {
            this.connection.close();
            this.connection = null;
        }
        log.info("disconnected: " + ip + ":22");
    }

    public class WrapperReader {
        private BufferedReader reader = null;

        public WrapperReader(BufferedReader reader) {
            this.reader = reader;
        }

        public BufferedReader getReader() {
            return this.reader;
        }

        public StringBuilder readAll() {
            int i;
            StringBuilder result = new StringBuilder();
            try {
                while ((i = reader.read()) != -1) {
                    result.append((char) i);
                    if (!reader.ready()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                        }
                    }
                    if (!reader.ready()) {
                        break;
                    }
                }
            } catch (IOException e) {
                log.warn("", e);
            }
            return result;
        }

        public void close() throws IOException {
            if (this.reader != null) {
                reader.close();
            }
        }
    }

    public static void main(String[] args) {
        try {
            NexusTest nt = new NexusTest("example.com");
            nt.connect("hoge", "fuga");
            nt.execute(HELLO_MSG, false);
            nt.execCommand("show running-config");
            nt.execute(CLOSE_MSG, true);
            nt.close();
        } catch (Exception e) {
            log.error("", e);
        }
    }
}