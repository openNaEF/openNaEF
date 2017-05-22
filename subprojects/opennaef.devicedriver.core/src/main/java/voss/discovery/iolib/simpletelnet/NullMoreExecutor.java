package voss.discovery.iolib.simpletelnet;


public class NullMoreExecutor implements MoreExecutor {
    public String getString() {
        return " ";
    }

    public byte[] getSendMoreBytes(String value) {
        throw new UnsupportedOperationException();
    }

    public boolean isMoreInput(byte[] value) {
        return false;
    }

}