package opennaef.rest.api.helper;

import opennaef.rest.api.CRUD;
import opennaef.rest.api.config.api.ApiConfig;
import opennaef.rest.api.response.ApiException;
import opennaef.rest.api.response.InternalServerError;
import naef.dto.NodeDto;
import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.dto.EntityDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.builder.VlanIfCommandBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static opennaef.rest.api.helper.CmdBuilderProcessor.toValue;


public class VlanIfBuildHelper implements BuildHelper {
    private static final Logger log = LoggerFactory.getLogger(VlanIfBuildHelper.class);

    @Override
    public List<CommandBuilder> help(CRUD crud, ApiConfig conf, Map<String, Object> reqArgs) throws ApiException {
        log.debug("help " + crud + " builderConfig is null.  " + reqArgs.get("$id"));

        VlanIfCommandBuilder builder;
        try {
            // vlan-if の親は node か port
            // 親が node の場合は Switch Vlan、
            // 親が port の場合は Router Vlan と呼ぶ
            VlanIfDto targetDto = (VlanIfDto) CmdBuilderProcessor.toValue(reqArgs.get("$id"), VlanIfDto.class.getTypeName());

            // TODO header へ認証情報(エディター名)を入れる必要がある
            NodeElementDto owner = (NodeElementDto) CmdBuilderProcessor.toValue(reqArgs.get("owner"), NodeElementDto.class.getTypeName());
            switch (crud) {
                case CREATE:
                    if (owner instanceof NodeDto) {
                        builder = new VlanIfCommandBuilder((NodeDto) owner, "editor");
                    } else if (owner instanceof PortDto) {
                        builder = new VlanIfCommandBuilder((PortDto) owner, "editor");
                    } else {
                        throw new IllegalStateException();
                    }
                    break;
                case UPDATE:
                case DELETE:
                    if (owner instanceof NodeDto) {
                        builder = new VlanIfCommandBuilder((NodeDto) owner, targetDto, "editor");
                    } else if (owner instanceof PortDto) {
                        builder = new VlanIfCommandBuilder((PortDto) owner, targetDto, "editor");
                    } else {
                        throw new IllegalStateException();
                    }
                    break;
                default:
                    // CmdBuilderProcessor でcrudのチェックをしているため、ここは実行されない
                    throw new IllegalStateException();
            }

            VlanDto vlan = (VlanDto) CmdBuilderProcessor.toValue(reqArgs.get("vlan"), VlanDto.class.getTypeName());

            // vlan
            builder.setVlan(vlan);
            // vlan-id
            builder.setVlanId(vlan.getVlanId());
            // if-name
            ifPresent(reqArgs, "if_name", String.class, builder::setIfName);

            // tagged "naef.dto.vlan-if.tagged-ports"
            List<String> taggedPortMvoIds = (List<String>) CmdBuilderProcessor.toValue(reqArgs.get("tagged_ports"), "java.util.List");
            builder.setTaggedPorts(getAbsoluteNames(taggedPortMvoIds), Collections.emptyList());

            // untagged "naef.dto.vlan-if.untagged-ports"
            List<String> untaggedPortMvoIds = (List<String>) CmdBuilderProcessor.toValue(reqArgs.get("untagged_ports"), "java.util.List");
            builder.setUntaggedPorts(getAbsoluteNames(untaggedPortMvoIds), Collections.emptyList());
        } catch (ClassNotFoundException e) {
            // parameterizedTypeが間違っている場合に起こるException
            // ハードコードしている部分か、設定ファイルが原因
            // Internal Server Error
            log.error("builderConfig", e);
            throw new InternalServerError("API-02501", "型定義が一致しませんでした. " + e.getMessage(), e);
        }

        return Collections.singletonList(builder);
    }

    public static List<String> getAbsoluteNames(List<String> mvoIds) throws ClassNotFoundException, ApiException {
        List<String> names = new ArrayList<>();
        for (String mvoId : mvoIds) {
            EntityDto dto = (EntityDto) CmdBuilderProcessor.toValue(mvoId, "tef.skelton.dto.EntityDto");
            names.add(DtoUtil.getAbsoluteName(dto));
        }

        return names;
    }

    public static <T> void ifPresent(Map<String, Object> rawArgs, String attrName, Class<T> type, Consumer<T> consumer) throws ClassNotFoundException, ApiException {
        T value = CmdBuilderProcessor.toValue(rawArgs.get(attrName), type);
        if (value != null) {
            log.debug("helper set {} = {}", attrName, value);
        }
        CmdBuilderProcessor.ifPresent(value, consumer);
    }
}
