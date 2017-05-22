package voss.discovery.iolib.simpletelnet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.console.ConsoleException;
import voss.util.VossMiscUtility;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.SocketException;
import java.util.Arrays;

public abstract class RealTerminalSocket extends TerminalSocket {
    private static final Logger log = LoggerFactory.getLogger(RealTerminalSocket.class);
    private static final Logger telnetLogger = LoggerFactory.getLogger("telnet-session");
    protected int TIMEOUT_TIME;
    protected long COMMAND_INTERVAL = 100L;
    protected long MORE_INTERVAL = 100L;
    private boolean vt100 = false;
    private long beginAccess = 0L;
    private long lastAccess = 0L;

    public void setCommandInterval(long interval) {
        this.COMMAND_INTERVAL = interval;
    }

    public long getCommandInterval() {
        return this.COMMAND_INTERVAL;
    }

    public void setMoreInterval(long interval) {
        this.MORE_INTERVAL = interval;
    }

    public long getMoreInterval() {
        return this.MORE_INTERVAL;
    }

    public void waitForNextCommand() {
        long now = System.currentTimeMillis();
        long waited = now - lastAccess;
        if (waited > COMMAND_INTERVAL) {
            telnetLogger.debug("no wait.");
            return;
        }
        try {
            long interval = this.COMMAND_INTERVAL - waited;
            telnetLogger.debug("waiting " + interval + " ms");
            Thread.sleep(interval);
        } catch (Exception e) {
        }
    }

    public void setTerminalAsVt100() {
        this.vt100 = true;
    }

    public boolean isVt100() {
        return this.vt100;
    }

    public String receiveTo(String toString, MoreExecutor more)
            throws SocketException, ConsoleException, IOException,
            ReceivingTimeoutException {
        ReceiveResult result = receiveTo(new String[]{toString}, more);
        return result.getResult();
    }

    public ReceiveResult receiveTo(String[] toString, MoreExecutor more)
            throws SocketException, ConsoleException, IOException,
            ReceivingTimeoutException {
        ReceiveResult result = new ReceiveResult(toString);
        try {
            while (!result.isReceiveEnd()) {
                if (more.isMoreInput(result.getReceiveByteArray())) {
                    telnetLogger.debug("send next: [" + decode(more.getString()) + "]");
                    send(more.getSendMoreBytes(result.getResult()));
                }
                read(result.getReceiveStream(), TIMEOUT_TIME);
                if (telnetLogger.isTraceEnabled()) {
                    telnetLogger.trace(VossMiscUtility.showDetail(result.getResult()));
                }
            }
        } catch (ReceivingTimeoutException e) {
            telnetLogger.warn("no expected response:");
            telnetLogger.warn("- expected: " + Arrays.toString(toString));
            telnetLogger.warn("- got: " + VossMiscUtility.showDetail(result.getResult()));
            throw e;
        } finally {
            result.close();
        }
        this.lastAccess = System.currentTimeMillis();
        log.debug("command lapse: " + (this.lastAccess - this.beginAccess) + " ms.");
        if (telnetLogger.isDebugEnabled()) {
            telnetLogger.debug("<< RECV\r\n" + decode(result.getResult()));
        }
        return result;
    }

    protected String receiveToPossible(MoreExecutor more) throws SocketException,
            ReceivingTimeoutException, ConsoleException, IOException {
        TelnetByteArray temp = new TelnetByteArray();
        while (true) {
            read(temp, TIMEOUT_TIME);
            byte[] bytes = temp.toByteArray();
            if (more.isMoreInput(bytes)) {
                try {
                    log.debug("more: wait " + this.MORE_INTERVAL + " ms to next.");
                    Thread.sleep(this.MORE_INTERVAL);
                } catch (InterruptedException e) {
                }
                send(more.getSendMoreBytes(new String(bytes)));
            } else {
                break;
            }
        }
        this.lastAccess = System.currentTimeMillis();
        log.debug("command lapse: " + (this.lastAccess - this.beginAccess) + " ms.");
        return new String(temp.toByteArray());
    }

    @Override
    public void sendln(String message) throws IOException {
        waitForNextCommand();
        if (log.isDebugEnabled()) {
            log.debug("command send: " + message);
        }
        if (telnetLogger.isDebugEnabled()) {
            telnetLogger.debug(">> SEND\r\n" + message);
        }
        super.sendln(message);
        this.beginAccess = System.currentTimeMillis();
    }

    private void read(OutputStream out, int timeout) throws IOException,
            ConsoleException {
        do {
            int read = read(timeout);
            out.write(read);
        } while (0 < getInputStream().available());
        out.flush();
    }

    public abstract PushbackInputStream getInputStream() throws IOException,
            ConsoleException;

    private String decode(String val) {
        StringBuffer sb = new StringBuffer();
        byte[] bytes = val.getBytes();
        for (int i = 0; i < bytes.length; ++i) {
            byte b = bytes[i];
            if (b > 0xff) {
                sb.append((char) b);
            } else {
                if (' ' <= b && b <= '~') {
                    sb.append((char) b);
                } else {
                    sb.append("[").append(Integer.toHexString(b).toLowerCase()).append("]");
                    if (b == 0x0a) {
                        sb.append("\r\n");
                    }
                }
            }
        }
        return sb.toString();
    }
}