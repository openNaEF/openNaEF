package voss.nms.inventory.builder.complement;

import naef.dto.ip.IpIfDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.ShellCommands;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.builder.conditional.ConditionalCommands;
import voss.nms.inventory.config.DiffConfiguration;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.util.Comparators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VlanLinkComplementCommands extends ConditionalCommands<IpIfDto> {
    private static final long serialVersionUID = 1L;

    public VlanLinkComplementCommands(String editorName) {
        super(editorName);
    }

    @Override
    protected void evaluateDiffInner(ShellCommands cmd) {
        for (VlanDto vlan : getVlans()) {
            VlanLinkMaintenanceBuilder builder = new VlanLinkMaintenanceBuilder(vlan, getEditorName());
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

    private List<VlanDto> getVlans() {
        try {
            InventoryConnector conn = InventoryConnector.getInstance();
            String vlanPoolName = DiffConfiguration.getInstance().getDiffPolicy().getDefaultVlanPoolName();
            VlanIdPoolDto pool = conn.getVlanPool(vlanPoolName);
            List<VlanDto> list = new ArrayList<VlanDto>();
            if (pool != null) {
                list.addAll(pool.getUsers());
            }
            Collections.sort(list, Comparators.getDtoComparator());
            return list;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

}