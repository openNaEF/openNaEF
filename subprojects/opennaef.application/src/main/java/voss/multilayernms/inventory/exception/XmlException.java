package voss.multilayernms.inventory.exception;

@SuppressWarnings("serial")
public class XmlException extends Exception {
    public XmlException(final Exception parent) {
        super.initCause(parent);
    }

    public XmlException(final String message) {
        super(message);
    }
}