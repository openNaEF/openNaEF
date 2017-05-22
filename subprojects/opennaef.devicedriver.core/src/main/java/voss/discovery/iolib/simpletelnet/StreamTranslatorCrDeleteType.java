package voss.discovery.iolib.simpletelnet;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class StreamTranslatorCrDeleteType implements StreamTranslator {
    private static final byte[] CRLF_WITH_NULL_ZERO_BYTES = {'\r', 0x00, '\n'};
    private static final String CRLF_WITH_NULL_ZERO = new String(CRLF_WITH_NULL_ZERO_BYTES);

    @Override
    public String translate(String stream) throws IOException {
        if (stream == null) {
            return null;
        }
        stream = stream.replace(CRLF_WITH_NULL_ZERO, "\r\n");
        Reader reader = null;
        StringBuffer result = new StringBuffer();
        try {
            reader = new StringReader(stream);
            int c = -1;
            StringBuffer temp = new StringBuffer();
            boolean carriageReturned = false;
            while ((c = reader.read()) != -1) {
                if (((char) c) == '\r') {
                    carriageReturned = true;
                } else if (((char) c) == '\n') {
                    result.append(temp).append('\r').append('\n');
                    carriageReturned = false;
                    temp = new StringBuffer();
                } else {
                    if (carriageReturned) {
                        carriageReturned = false;
                        temp = new StringBuffer();
                    }
                    temp.append((char) c);
                }
            }
            result.append(temp);
            return result.toString();

        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

}