package voss.nms.inventory.builder.complement;

import naef.dto.ip.IpIfDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.ShellCommands;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.builder.conditional.ConditionalCommands;

public class IpSubnetAddressComplementCommands extends ConditionalCommands<IpIfDto> {
    private static final long serialVersionUID = 1L;

    public IpSubnetAddressComplementCommands(String editorName) {
        super(editorName);
    }

    @Override
    protected void evaluateDiffInner(ShellCommands cmd) {
        IpSubnetAddressComplementBuilder builder = new IpSubnetAddressComplementBuilder(getEditorName());
        try {
            BuildResult result = builder.buildCommand();
            if (result != BuildResult.SUCCESS) {
                return;
            }
            cmd.addBuilder(builder);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

}