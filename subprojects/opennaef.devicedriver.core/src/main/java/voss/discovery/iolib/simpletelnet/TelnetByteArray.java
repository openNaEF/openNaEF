package voss.discovery.iolib.simpletelnet;

import java.io.ByteArrayOutputStream;


public class TelnetByteArray extends ByteArrayOutputStream {

    public byte[] toByteArray() {
        byte[] source = super.toByteArray();
        byte[] result = new byte[source.length];
        int index = 0;
        for (int i = 0; i < source.length; i++) {
            if (source[i] == '\b') {
                index--;
            } else {
                result[index] = source[i];
                index++;
            }
        }
        return chopIAC(trim(result, index));
    }

    private byte[] chopIAC(byte[] value) {
        byte[] result = new byte[value.length];
        int index = 0;
        for (int i = 0; i < value.length; i++) {
            if (value[i] == VossTelnetOption.IAC) {
                i += 2;
            } else {
                result[index] = value[i];
                index++;
            }
        }
        return trim(result, index);
    }

    private static byte[] trim(byte[] source, int length) {
        byte[] result = new byte[length];
        System.arraycopy(source, 0, result, 0, result.length);
        return result;
    }

}