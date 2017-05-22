package voss.nms.inventory.util;

public enum IdSelectionType {
    FREE("Free input"),
    SELECT("Select from list"),;

    private final String caption;

    private IdSelectionType(String caption) {
        this.caption = caption;
    }

    public String getCaption() {
        return this.caption;
    }
}