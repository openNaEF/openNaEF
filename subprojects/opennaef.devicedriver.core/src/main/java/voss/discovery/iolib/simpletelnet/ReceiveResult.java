package voss.discovery.iolib.simpletelnet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.utils.ByteArrayUtil;
import voss.discovery.utils.ListUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class ReceiveResult {
    private final static Logger log = LoggerFactory.getLogger(ReceiveResult.class);
    private final List<byte[]> receiveEndBytes;
    private final TelnetByteArray receive = new TelnetByteArray();
    private String key;

    public ReceiveResult(String[] toString) {
        receiveEndBytes = new ArrayList<byte[]>();
        for (String waitFor : toString) {
            receiveEndBytes.add(waitFor.getBytes());
        }
    }

    public String getResult() {
        return new String(receive.toByteArray());
    }

    public String getReceiveKey() {
        return key;
    }

    public void close() throws IOException {
        receive.close();
    }

    public OutputStream getReceiveStream() {
        return receive;
    }

    public byte[] getReceiveByteArray() {
        return receive.toByteArray();
    }

    public boolean isReceiveEnd() {
        byte[] receiveByte = getReceiveByteArray();
        for (byte[] receiveEndByte : receiveEndBytes) {
            String tempKey = new String(receiveEndByte);
            if (log.isDebugEnabled()) {
                log.debug("waiting: " + tempKey);
                if (log.isTraceEnabled()) {
                    log.trace("received: " + new String(receiveByte));
                } else {
                    log.debug("received: " + ListUtil.tail(ListUtil.toLines(new String(receiveByte)), 1));
                }
            }
            if (ByteArrayUtil.contains(receiveByte, receiveEndByte)) {
                this.key = tempKey;
                return true;
            }
            char lastChar = (char) receiveEndByte[receiveEndByte.length - 1];
            if ("$*.+[]()".indexOf(lastChar) >= 0) {
                log.trace("!! end with regex meta-char: " + String.valueOf(lastChar));
                continue;
            }
            if (ByteArrayUtil.matches(receiveByte, receiveEndByte)) {
                this.key = tempKey;
                return true;
            }
        }
        return false;
    }

}