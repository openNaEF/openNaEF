package voss.nms.inventory.element;

import java.io.Serializable;

public class HyperLink implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String delimiter = "@";
    private String caption;
    private String url;
    private boolean openAnotherWindow = false;

    public HyperLink(String linkWithCaption) {
        int lastIndex = linkWithCaption.lastIndexOf(delimiter);
        if (lastIndex == -1) {
            throw new IllegalArgumentException();
        }
        this.caption = linkWithCaption.substring(0, lastIndex);
        this.url = linkWithCaption.substring(lastIndex + 1);
    }

    public HyperLink(String caption, String url) {
        this.caption = caption;
        this.url = url;
    }

    public HyperLink() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCaption() {
        return this.caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public boolean isOpenAnotherWindow() {
        return this.openAnotherWindow;
    }

    public boolean getOpenAnotherWindow() {
        return this.openAnotherWindow;
    }

    public void setOpenAnotherWindow(boolean open) {
        this.openAnotherWindow = open;
    }

    public String getLinkWithCaption() {
        return getLinkWithCaption(this.caption, this.url);
    }

    public static String getLinkWithCaption(String caption, String url) {
        if (url.indexOf(delimiter) != -1) {
            throw new IllegalArgumentException("A URL including [" + delimiter + "] can not be registered.");
        } else if (caption.indexOf(delimiter) != -1) {
            throw new IllegalArgumentException("A URL including [" + delimiter + "] can not be registered.");
        } else if (url.indexOf(' ') != -1) {
            throw new IllegalArgumentException("You can not register a URL containing half-width spaces.");
        } else if (caption.indexOf(' ') != -1) {
            throw new IllegalArgumentException("You can not register a URL containing half-width spaces.");
        }
        return caption + delimiter + url;
    }
}