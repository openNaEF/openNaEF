package voss.discovery.iolib.simpletelnet;


public interface MoreExecutor {
    String getString();

    byte[] getSendMoreBytes(String value);

    boolean isMoreInput(byte[] value);
}