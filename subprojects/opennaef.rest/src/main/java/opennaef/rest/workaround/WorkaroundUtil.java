package opennaef.rest.workaround;

import naef.dto.NaefDto;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.VlanBuilderUtil;

import java.util.List;

/**
 * ワークアラウンド用 メソッド
 */
public class WorkaroundUtil {

    /**
     * InventoryBuilder.changeContext のワークアラウンド
     * absolute-name ではなく mvo-id を直接指定する
     *
     * @param cmd
     * @param dto
     */
    public static void changeContext(ShellCommands cmd, NaefDto dto) {
        cmd.addCommand(InventoryBuilder.translate("context \"_NAME_\"", new String[]{"_NAME_", dto.getOid().toString()}));
    }

    public static void createLink(ShellCommands cmd, String vif1AbsoluteName, String vif2AbsoluteName) {
        _VlanBuilderUtil._createLink(cmd, vif1AbsoluteName, vif2AbsoluteName);
    }


    private static class _VlanBuilderUtil extends VlanBuilderUtil {
        public static void _createLink(ShellCommands cmd, String vif1AbsoluteName, String vif2AbsoluteName) {
            List names = Util.sortNames(new String[]{vif1AbsoluteName, vif2AbsoluteName});
            String name1 = (String) names.get(0);
            String name2 = (String) names.get(1);
            cmd.log("createLink");
            InventoryBuilder.translate(cmd, "configure-link connect \"_TYPE_\" \"_FQN1_\" \"_FQN2_\"", new String[]{"_TYPE_", "vlan-link", "_FQN1_", name1, "_FQN2_", name2});
        }
    }
}
