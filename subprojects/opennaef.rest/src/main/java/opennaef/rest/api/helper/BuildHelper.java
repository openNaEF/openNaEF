package opennaef.rest.api.helper;

import opennaef.rest.api.CRUD;
import opennaef.rest.api.config.api.ApiConfig;
import opennaef.rest.api.response.ApiException;
import voss.core.server.builder.CommandBuilder;

import java.util.List;
import java.util.Map;

public interface BuildHelper {
    /**
     * Builderを生成、実行する
     *
     * @param crud
     * @param conf
     * @param reqArgs
     * @return Builderのリスト
     * @throws ApiException
     */
    List<CommandBuilder> help(CRUD crud, ApiConfig conf, Map<String, Object> reqArgs) throws ApiException;
}
