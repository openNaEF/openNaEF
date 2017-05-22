package voss.multilayernms.inventory.util;

import naef.dto.NaefDto;

public class IllegalInventoryStatusException extends Exception {
    private static final long serialVersionUID = 1L;
    private final NaefDto inventoryObject;

    public IllegalInventoryStatusException(String msg, NaefDto target) {
        super(msg);
        this.inventoryObject = target;
    }

    public IllegalInventoryStatusException(Throwable th, NaefDto target) {
        super(th);
        this.inventoryObject = target;
    }

    public IllegalInventoryStatusException(String msg, Throwable th, NaefDto target) {
        super(msg, th);
        this.inventoryObject = target;
    }

    public NaefDto getInventoryObject() {
        return this.inventoryObject;
    }

}