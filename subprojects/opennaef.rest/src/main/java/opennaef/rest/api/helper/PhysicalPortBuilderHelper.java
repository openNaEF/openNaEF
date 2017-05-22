package opennaef.rest.api.helper;

import opennaef.rest.api.CRUD;
import opennaef.rest.api.config.api.ApiConfig;
import opennaef.rest.api.response.ApiException;
import opennaef.rest.api.response.BadRequest;
import naef.dto.HardPortDto;
import naef.dto.NodeElementDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandBuilder;
import voss.nms.inventory.builder.PhysicalPortCommandBuilder;
import voss.nms.inventory.constants.PortType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static opennaef.rest.api.helper.CmdBuilderProcessor.toValue;


public class PhysicalPortBuilderHelper implements BuildHelper {
    private static final Logger log = LoggerFactory.getLogger(PhysicalPortBuilderHelper.class);

    @Override
    public List<CommandBuilder> help(CRUD crud, ApiConfig conf, Map<String, Object> reqArgs) throws ApiException {
        log.debug("help "
                + crud
                + " builderConfig is null. "
                + reqArgs.get("$id"));

        PhysicalPortCommandBuilder builder;
        try {
            HardPortDto targetDto = (HardPortDto) CmdBuilderProcessor.toValue(reqArgs.get("$id"), NodeElementDto.class.getTypeName());

            // TODO header へ認証情報(エディター名)を入れる必要がある
            NodeElementDto owner = (NodeElementDto) CmdBuilderProcessor.toValue(reqArgs.get("owner"), NodeElementDto.class.getTypeName());
            switch (crud) {
                case CREATE:

                    builder = new PhysicalPortCommandBuilder(owner, "editor");
                    break;
                case UPDATE:
                    builder = new PhysicalPortCommandBuilder(owner, targetDto, "editor");
                    break;
                case DELETE:
                    owner = targetDto.getOwner();
                    builder = new PhysicalPortCommandBuilder(owner, targetDto, "editor");
                    break;
                default:
                    // CmdBuilderProcessor でcrudのチェックをしているため、ここは実行されない
                    throw new IllegalStateException();
            }

            PortType portType = getPortType((String) reqArgs.get("port_type"));
            CmdBuilderProcessor.ifPresent(portType, builder::setPortType);

            ifPresent(reqArgs, "name", String.class, builder::setPortName);
            ifPresent(reqArgs, "if_name", String.class, builder::setIfName);
            ifPresent(reqArgs, "port_mode", String.class, builder::setPortMode);
            ifPresent(reqArgs, "switch_port_mode", String.class, builder::setSwitchPortMode);
            ifPresent(reqArgs, "bandwidth", Long.class, builder::setBandwidth);
            ifPresent(reqArgs, "oper_status", String.class, builder::setOperStatus);
            ifPresent(reqArgs, "purpose", String.class, builder::setPurpose);
            ifPresent(reqArgs, "note", String.class, builder::setNote);
        } catch (ClassNotFoundException e) {
            log.error("port build helper", e);
            throw new BadRequest("API-02400", "入力値が不正です. " + e.getMessage(), e);
        }

//        ifPresent(rawArgs, MPLSNMS_ATTR.PORTSPEED_ADMIN, String.class, builderConfig::setAdminBandwidth);
//        ifPresent(rawArgs, MPLSNMS_ATTR.DUPLEX_ADMIN, String.class, builderConfig::setAdminDuplex);
//        ifPresent(rawArgs, MPLSNMS_ATTR.ADMIN_STATUS, String.class, builderConfig::setAdminStatus);
//        ifPresent(rawArgs, MPLSNMS_ATTR.CONFIG_NAME, String.class, builderConfig::setConfigName);
//        ifPresent(rawArgs, MPLSNMS_ATTR.END_USER, String.class, builderConfig::setEndUserName);
//        ifPresent(rawArgs, MPLSNMS_ATTR.IGP_COST, Integer.class, builderConfig::setIgpCost);
//        ifPresent(rawArgs, MPLSNMS_ATTR.IMPLICIT, Boolean.class, builderConfig::setImplicit);
//        ifPresent(rawArgs, MPLSNMS_ATTR.DUPLEX_OPER, String.class, builderConfig::setOperDuplex);
//        ifPresent(rawArgs, MPLSNMS_ATTR.DESCRIPTION, String.class, builderConfig::setPortDescription);
//        ifPresent(rawArgs, MPLSNMS_ATTR.SOURCE, String.class, builderConfig::setSource);
//        ifPresent(rawArgs, MPLSNMS_ATTR.STORMCONTROL_BROADCAST_LEVEL, String.class, builderConfig::setStormControlBroadcastLevel);


//        TODO builderConfig 内での処理分岐のためのフラグの指定方法
//        b.setAllowIpDuplication(true);
//        b.setAssociateIpSubnetAddress(true);
//        b.setIndependentIP(true);
//        b.setIpNeeds(true);
//        b.setNewIpAddress("vpn", "0.0.0.0", "24");
//        b.setPreCheckEnable(false);

        return Collections.singletonList(builder);
    }


    public static <T> void ifPresent(Map<String, Object> rawArgs, String attrName, Class<T> type, Consumer<T> consumer) throws ClassNotFoundException, ApiException {
        T value = CmdBuilderProcessor.toValue(rawArgs.get(attrName), type);
        if (value != null) {
            log.debug("helper set {} = {}", attrName, value);
        }
        CmdBuilderProcessor.ifPresent(value, consumer);
    }

    private static PortType getPortType(String portTypeStr) {
        for (PortType t : PortType.values()) {
            if (t.getCaption().equalsIgnoreCase(portTypeStr)) return t;
        }
        return null;
    }
}
