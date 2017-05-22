package voss.discovery.iolib.simpletelnet;

import java.io.IOException;

public interface StreamTranslator {
    String translate(String stream) throws IOException;
}