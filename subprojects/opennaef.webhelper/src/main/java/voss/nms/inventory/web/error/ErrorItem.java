package voss.nms.inventory.web.error;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ErrorItem implements Serializable {
    private static final long serialVersionUID = 1L;

    public String title;
    public List<String> lines = new ArrayList<String>();

    public ErrorItem() {
    }

    public ErrorItem(String title, String content) {
        this.title = title;
        this.lines.add(content);
    }

    public void addLine(String line) {
        if (line == null) {
            return;
        }
        this.lines.add(line);
    }

    public static List<ErrorItem> createSimpleErrorItem(String title, String content) {
        ErrorItem item = new ErrorItem(title, content);
        List<ErrorItem> items = new ArrayList<ErrorItem>();
        items.add(item);
        return items;
    }
}