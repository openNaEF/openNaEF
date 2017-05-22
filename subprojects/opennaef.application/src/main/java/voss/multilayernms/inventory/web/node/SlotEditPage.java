package voss.multilayernms.inventory.web.node;

import naef.dto.ModuleDto;
import naef.dto.NodeDto;
import naef.dto.SlotDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import voss.core.server.builder.BuildResult;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.renderer.HardwareRenderer;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.nms.inventory.builder.ModuleCommandBuilder;
import voss.nms.inventory.builder.SlotCommandBuilder;
import voss.nms.inventory.database.MetadataManager;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.PageUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class SlotEditPage extends WebPage {
    public static final String OPERATION_NAME = "SlotEdit";
    private final SlotDto slot;
    private final String editorName;
    private final WebPage backPage;

    private String slotType;
    private String slotNote;

    private String moduleType;
    private String moduleNote;

    public SlotEditPage(WebPage backPage, SlotDto slot) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            if (slot == null) {
                throw new IllegalStateException("no slot.");
            } else {
                this.slotType = HardwareRenderer.getSlotType(slot);
                this.slotNote = HardwareRenderer.getNote(slot);
            }
            this.slot = slot;
            if (this.slot.getModule() != null) {
                ModuleDto module = this.slot.getModule();
                this.moduleType = HardwareRenderer.getModuleType(module);
                this.moduleNote = HardwareRenderer.getNote(module);
                ;
            }

            Form<Void> backForm = new Form<Void>("backForm");
            add(backForm);
            Button backButton = new Button("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(getBackPage());
                }
            };
            backForm.add(backButton);

            add(new FeedbackPanel("feedback"));

            String caption = "Node: " + slot.getNode().getName() + " /Slot:" + slot.getName();
            Label captionLabel = new Label("caption", Model.of(caption));
            add(captionLabel);

            Form<Void> editSlot = new Form<Void>("editSlot");
            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        SlotDto slot = getSlot();
                        ModuleDto currentModule = slot.getModule();
                        ModuleCommandBuilder moduleBuilder = null;
                        if (currentModule == null && getModuleType() != null) {
                            moduleBuilder = new ModuleCommandBuilder(slot, editorName);
                            moduleBuilder.setMetadata(getModuleType());
                            moduleBuilder.setNote(getModuleNote());
                            moduleBuilder.setSource(DiffCategory.INVENTORY.name());
                        } else if (currentModule != null) {
                            moduleBuilder = new ModuleCommandBuilder(currentModule, editorName);
                            moduleBuilder.setNote(getModuleNote());
                        }
                        SlotCommandBuilder builder = new SlotCommandBuilder(slot, editorName);
                        builder.setNote(getSlotNote());
                        builder.setSlotTypeName(getSlotType());
                        BuildResult result = builder.buildCommand();
                        BuildResult result2 = moduleBuilder.buildCommand();
                        if (result != BuildResult.NO_CHANGES || result2 != BuildResult.NO_CHANGES) {
                            ShellConnector.getInstance().executes(moduleBuilder, builder);
                        }
                        PageUtil.setModelChanged(getBackPage());
                        setResponsePage(getBackPage());
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            editSlot.add(proceedButton);
            add(editSlot);

            Label slotId = new Label("slotId", new Model<String>(slot.getName()));
            editSlot.add(slotId);

            TextField<String> slotType = new TextField<String>("slotType",
                    new PropertyModel<String>(this, "slotType"));
            editSlot.add(slotType);

            TextField<String> note = new TextField<String>("note",
                    new PropertyModel<String>(this, "slotNote"));
            editSlot.add(note);

            TextField<String> noteModule = new TextField<String>("moduleNote",
                    new PropertyModel<String>(this, "moduleNote"));
            editSlot.add(noteModule);

            try {
                final MetadataManager mm = MetadataManager.getInstance();
                IModel<List<? extends String>> moduleChoices = new AbstractReadOnlyModel<List<? extends String>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public List<String> getObject() {
                        try {
                            List<String> moduleTypes = mm.getModuleTypeList(getVendor());
                            if (moduleTypes == null) {
                                moduleTypes = Collections.emptyList();
                            }
                            SlotDto slot = getSlot();
                            if (slot.getModule() != null) {
                                String moduleTypeName = HardwareRenderer.getModuleType(slot.getModule());
                                if (moduleTypeName != null && !moduleTypes.contains(moduleTypeName)) {
                                    moduleTypes.add(moduleTypeName);
                                }
                            }
                            return moduleTypes;
                        } catch (IOException e) {
                            throw new IllegalStateException("fail to load module-type.", e);
                        }
                    }
                };

                final DropDownChoice<String> moduleTypeList = new DropDownChoice<String>("moduleTypeList",
                        new PropertyModel<String>(this, "moduleType"), moduleChoices);
                editSlot.add(moduleTypeList);
                moduleTypeList.setOutputMarkupId(true);
                moduleTypeList.setEnabled(moduleType == null);
            } catch (IOException e) {
                throw new IllegalStateException("failed to load vendor metadata.", e);
            }

        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public SlotDto getSlot() {
        return this.slot;
    }

    public String getVendor() {
        NodeDto node = this.slot.getNode();
        return NodeRenderer.getVendorName(node);
    }

    public String getModuleType() {
        return moduleType;
    }

    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }

    public String getSlotType() {
        return slotType;
    }

    public void setSlotType(String slotType) {
        this.slotType = slotType;
    }

    public String getSlotNote() {
        return slotNote;
    }

    public void setSlotNote(String slotNote) {
        this.slotNote = slotNote;
    }

    public String getModuleNote() {
        return moduleNote;
    }

    public void setModuleNote(String moduleNote) {
        this.moduleNote = moduleNote;
    }
}