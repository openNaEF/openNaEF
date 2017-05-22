package opennaef.rest.api.helper;

import opennaef.rest.api.CRUD;
import opennaef.rest.api.config.api.ApiConfig;
import opennaef.rest.api.response.ApiException;
import opennaef.rest.api.response.InternalServerError;
import naef.dto.LinkDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vlan.VlanSegmentDto;
import opennaef.rest.workaround._VlanLinkCommandBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static opennaef.rest.api.helper.CmdBuilderProcessor.toValue;

public class VlanLinkBuildHelper implements BuildHelper {
    private static final Logger log = LoggerFactory.getLogger(VlanLinkBuildHelper.class);

    @Override
    public List<CommandBuilder> help(CRUD crud, ApiConfig conf, Map<String, Object> reqArgs) throws ApiException {
        log.debug("help " + crud + " " + conf.builderConfig.builder.getSimpleName() + " " + reqArgs.get("$id"));

        _VlanLinkCommandBuilder builder;
        try {
            VlanSegmentDto targetDto = (VlanSegmentDto) CmdBuilderProcessor.toValue(reqArgs.get("$id"), VlanSegmentDto.class.getTypeName());

            // TODO header へ認証情報(エディター名)を入れる必要がある
            switch (crud) {
                case CREATE:
                    // このコンストラクタを使うとエラーが投げられるため使用できない
                    // java.lang.IllegalStateException: vlan mismatch: vif1.vlan=vlan.id-pool.dot1q;vlans,id;30, vif2=vlan.id-pool.dot1q;vlans,id;30
                    // builderConfig = new VlanLinkCommandBuilder(vif1, vif2, lower, "editor");


                    List<String> vifIds = CmdBuilderProcessor.toValue(reqArgs.get("member_ports"), List.class);
                    if (vifIds.size() != 2) {
                        throw new IllegalStateException("vlan-link には vlan-if が2つ必要です。");
                    }

                    List<String> lowerIds = CmdBuilderProcessor.toValue(reqArgs.get("lower_layers"), List.class);
                    if (lowerIds.size() != 1) {
                        throw new IllegalStateException("vlan-link は eth-link に stack される必要があります。");
                    }

                    VlanIfDto vif1 = (VlanIfDto) CmdBuilderProcessor.toValue(vifIds.get(0), VlanIfDto.class.getTypeName());
                    VlanIfDto vif2 = (VlanIfDto) CmdBuilderProcessor.toValue(vifIds.get(1), VlanIfDto.class.getTypeName());
                    LinkDto lower = (LinkDto) CmdBuilderProcessor.toValue(lowerIds.get(0), LinkDto.class.getTypeName());

                    builder = new _VlanLinkCommandBuilder(vif1.getTrafficDomain(), vif1.getAbsoluteName(), vif2.getAbsoluteName(), lower, "editor");
                    break;
                case UPDATE:
                case DELETE:
                    builder = new _VlanLinkCommandBuilder(targetDto, "editor");
                    break;
                default:
                    // CmdBuilderProcessor でcrudのチェックをしているため、ここは実行されない
                    throw new IllegalStateException();
            }
        } catch (ClassNotFoundException e) {
            // parameterizedTypeが間違っている場合に起こるException
            // ハードコードしている部分か、設定ファイルが原因
            // Internal Server Error
            log.error("builderConfig", e);
            throw new InternalServerError("API-02501", "型定義が一致しませんでした. " + e.getMessage(), e);
        }
        return Collections.singletonList(builder);
    }
}
