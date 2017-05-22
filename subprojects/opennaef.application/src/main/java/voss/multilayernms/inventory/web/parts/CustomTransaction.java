package voss.multilayernms.inventory.web.parts;

import org.apache.wicket.markup.html.WebPage;
import voss.core.server.util.Util;

import java.io.Serializable;
import java.util.List;

public interface CustomTransaction extends Serializable {
    WebPage getBackPage();

    WebPage getForwardPage();

    String getTitle();

    String getHead();

    String getConfirmationMessage();

    String getSuccessResultMessage();

    String getFailResultMessage();

    void execute();

    List<ExtraInput> getExtraInputs();

    boolean isSuccess();

    Exception getException();

    public static class ExtraInput implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String key;
        private final Class<?> type;
        private final String title;
        private String value;

        public ExtraInput(String key, Class<?> type, String title) {
            if (Util.isNull(key, type, title)) {
                throw new IllegalArgumentException("missing args.");
            }
            this.key = key;
            this.type = type;
            this.title = title;
        }

        public String getKey() {
            return this.key;
        }

        public Class<?> getType() {
            return this.type;
        }

        public String getTitle() {
            return this.title;
        }

        public String getValue() {
            return this.value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}