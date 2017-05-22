package voss.multilayernms.inventory.web.deploy;


import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.Model;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.UrlUtil;

import java.util.Arrays;
import java.util.List;

public class DeployPage extends WebPage {
    public static final String OPERATION_NAME = "Deploy";
    private static final String KEY_LOG = "key_log";
    private final String EDITOR_NAME;

    public DeployPage() {
        this(new PageParameters());
    }

    public DeployPage(PageParameters param) {
        try {
            this.EDITOR_NAME = AAAWebUtil.checkAAA(this, OPERATION_NAME);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }

        add(UrlUtil.getTopLink("top"));
        add(UrlUtil.getLink("refresh", "deploy"));

        add(new FileUploadForm());
        add(new LogForm(param.getString(KEY_LOG)));
    }

    private class FileUploadForm extends Form<Void> {
        private static final long serialVersionUID = 1L;
        public static final String ID = "fileForm";
        private FileUploadField _fileUploadField;

        public FileUploadForm() {
            super(ID);
            _fileUploadField = new FileUploadField("fileInput");
            _fileUploadField.setRequired(true);
            add(_fileUploadField);
        }

        @Override
        public void onSubmit() {
            PageParameters param = new PageParameters();
            List<String> commands = getCommands(_fileUploadField.getFileUpload());

            ShellCommands cmd = new ShellCommands(EDITOR_NAME);
            cmd.addCommands(commands);
            try {
                ShellConnector.getInstance().execute2(cmd);
                param.add(KEY_LOG, "success");
            } catch (InventoryException e) {
                param.put(KEY_LOG, e.getMessage());
            }

            setResponsePage(new DeployPage(param));
        }

        private List<String> getCommands(FileUpload fileUpload) {
            if (fileUpload == null) return null;
            return Arrays.asList(new String(fileUpload.getBytes()).split("\n"));
        }
    }

    private class LogForm extends Form<Void> {
        private static final long serialVersionUID = 1L;
        public static final String ID = "logForm";

        public LogForm(String log) {
            super(ID);
            add(new TextArea<String>("logArea", Model.of(log)));
        }
    }
}