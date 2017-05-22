package voss.nms.inventory.diff.network;

import voss.model.GenericEthernetSwitch;

public class ErrorDummyDevice extends GenericEthernetSwitch {
    private static final long serialVersionUID = 1L;
    private final String errorMessage;

    public ErrorDummyDevice(String msg) {
        this.errorMessage = msg;
    }

    public String getMessage() {
        return this.errorMessage;
    }
}