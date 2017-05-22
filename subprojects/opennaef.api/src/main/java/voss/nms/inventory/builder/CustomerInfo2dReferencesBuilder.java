package voss.nms.inventory.builder;

import pasaran.naef.dto.CustomerInfo2dDto;
import tef.DateTime;
import tef.skelton.dto.EntityDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CustomerInfo2dReferencesBuilder extends AbstractCommandBuilder {
    private static final String _COMMAND = "customerinfo-2d \"_OP_\" \"_OBJECT_\" \"_CUSTOMER-INFO_\"";
    private static final String _OP = "_OP_";
    private static final String _OBJECT = "_OBJECT_";
    private static final String _CUSTOMER_INFO = "_CUSTOMER-INFO_";

    private final String _customerInfoAbsoluteName;
    private final List<Reference> _references = new ArrayList<>();


    public CustomerInfo2dReferencesBuilder(CustomerInfo2dDto customerInfo, String editorName) {
        this(customerInfo, null, editorName);
    }

    public CustomerInfo2dReferencesBuilder(String customerInfoAbsoluteName, String editorName) {
        this(null, customerInfoAbsoluteName, editorName);
    }

    private CustomerInfo2dReferencesBuilder(CustomerInfo2dDto dto, String absoluteName, String editorName) {
        super(CustomerInfo2dDto.class, dto, editorName);
        if (dto == null && absoluteName == null) {
            throw new IllegalArgumentException("dto, absolute-name is null.");
        }
        if (dto != null) {
            _customerInfoAbsoluteName = DtoUtil.getAbsoluteName(dto);
        } else {
            _customerInfoAbsoluteName = absoluteName;
        }
    }

    public void addReference(DateTime time, EntityDto dto) {
        addReference(time, OP.add, dto);
    }

    public void addReference(DateTime time, String absoluteName) {
        addReference(time, OP.add, absoluteName);
    }

    public void removeReference(DateTime time, EntityDto dto) {
        addReference(time, OP.remove, dto);
    }

    public void removeReference(DateTime time, String absoluteName) {
        addReference(time, OP.remove, absoluteName);
    }

    private void addReference(DateTime time, OP op, EntityDto dto) {
        if (dto == null) throw new IllegalArgumentException("dto is null.");
        addReference(time, op, DtoUtil.getAbsoluteName(dto));
    }

    private void addReference(DateTime time, OP op, String absoluteName) {
        if (absoluteName == null) throw new IllegalArgumentException("absoluteName is null.");
        Reference ref = new Reference(time, op, absoluteName);
        _references.add(ref);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, ExternalServiceException, InventoryException {
        if (_references.size() == 0) {
            return BuildResult.NO_CHANGES;
        }
        _references.forEach(ref -> cmd.addCommands(ref.commands(_customerInfoAbsoluteName)));
        recordChange("REFERENCE_2D", "", "");
        InventoryBuilder.changeContext(cmd, _customerInfoAbsoluteName);
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, ExternalServiceException, InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getObjectType() {
        return DiffObjectType.CUSTOMER_INFO.getCaption();
    }

    private static class Reference {
        final DateTime time;
        final OP op;
        final String targetAbsoluteName;

        Reference(DateTime time, OP op, EntityDto dto) {
            this(time, op, DtoUtil.getMvoIdString(dto));
        }

        Reference(DateTime time, OP op, String absoluteName) {
            this.time = time;
            this.op = op;
            this.targetAbsoluteName = absoluteName;
        }

        public List<String> commands(String customerInfo) {
            List<String> cmds = new ArrayList<>();
            cmds.add("time " + time.value);
            String cmd = InventoryBuilder.translate(_COMMAND,
                    _OP, op.name(),
                    _OBJECT, targetAbsoluteName,
                    _CUSTOMER_INFO, customerInfo);
            cmds.add(cmd);
            return cmds;
        }
    }

    private static enum OP {
        add, remove;
    }
}
