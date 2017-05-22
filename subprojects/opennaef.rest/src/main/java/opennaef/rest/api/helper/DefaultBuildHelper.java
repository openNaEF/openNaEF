package opennaef.rest.api.helper;

import opennaef.rest.api.CRUD;
import opennaef.rest.api.config.api.*;
import opennaef.rest.api.response.ApiException;
import opennaef.rest.api.response.BadRequest;
import opennaef.rest.api.response.InternalServerError;
import opennaef.rest.api.response.MethodNotAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefaultBuildHelper implements BuildHelper {
    private static final Logger log = LoggerFactory.getLogger(DefaultBuildHelper.class);
    public static final DefaultBuildHelper INSTANCE = new DefaultBuildHelper();

    /**
     * CmdBuilderMappingConfigから設定を読み出し、CommandBuilderへ値をセットする
     * CommandBuilderへの値のセットはreflectionを使用している
     *
     * @param crud
     * @param conf
     * @param reqArgs
     * @return CommandBuilder
     * @throws ApiException
     */
    @Override
    public List<CommandBuilder> help(CRUD crud, ApiConfig conf, Map<String, Object> reqArgs) throws ApiException {
        if (!conf.hasBuilderConfig()) {
            // builderの設定がない場合はこのクラスを使用することはできない
            log.debug("[help BUILDER_CONFIG does not exist] {} {} {}", crud, reqArgs.get("$id"));
            throw new MethodNotAllowed();
        }
        final BuilderConfig builderConf = conf.builderConfig;
        log.debug("[help] " + crud + " " + builderConf.builder.getSimpleName() + " " + reqArgs.get("$id"));

        // CommandBuilderのコンストラクタの設定を取得する

        final ConstructorConfig constructorConf;
        switch (crud) {
            case CREATE:
                constructorConf = builderConf.createConstructor;
                break;
            case UPDATE:
                constructorConf = builderConf.updateConstructor;
                break;
            case DELETE:
                constructorConf = builderConf.deleteConstructor;
                break;
            default:
                // CmdBuilderProcessor でcrudのチェックをしているため、ここは実行されない
                throw new IllegalStateException();
        }

        // コンストラクタの設定が存在していない場合は、 405 Method Not Allowed を返す
        if (constructorConf == null) {
            // 設定が存在しない == サポートしていない
            // 405 Method Not Allowed.
            log.info("constructor not found. {} {}", crud, builderConf.builder.getSimpleName());
            throw new MethodNotAllowed();
        }

        // コンストラクタの引数の設定をもとに値を取り出す
        Object[] args;
        CommandBuilder builder;
        try {
            args = new Object[constructorConf.args.size()];
            List<ArgConfig> consConf = constructorConf.args;
            for (int i = 0; i < consConf.size(); i++) {
                String attrName = consConf.get(i).attrName;
                if (attrName == null) {
                    log.warn("attr-name is null.");
                }

                ArgConfig argConf = consConf.get(i);
                Object arg = CmdBuilderProcessor.getArg(reqArgs, argConf);

                // FIXME ワークアラウンド editor-nameは認証情報から取得する
                if (argConf.attrName.equals("editor-name")) {
                    arg = "editor";
                }

                args[i] = arg;
            }

            // CommandBuilderを生成する
            builder = constructorConf.constructor.newInstance(args);

            // メソッドの設定を取り出し、CommandBuilderへ値をセットする
            // 値がnullの場合は値のセットを行わない
            boolean isExecutable;
            for (MethodConfig methodConf : builderConf.methods) {
                isExecutable = true;
                Method method = methodConf.method;

                Object[] methodArgs = new Object[methodConf.args.size()];
                List<ArgConfig> methodArgConf = methodConf.args;
                for (int i = 0; i < methodArgConf.size(); i++) {
                    Object value = CmdBuilderProcessor.getArg(reqArgs, methodArgConf.get(i));
                    methodArgs[i] = value;
                    if (value == null) isExecutable = false;
                }

                if (isExecutable) {
                    log.debug("[help] set {} = {}", method.getName(), methodArgs);
                    method.invoke(builder, methodArgs);
                }
            }
        } catch (InstantiationException e) {
            // 設定ファイルとCommandBuilderのコンストラクタの引数が一致しなかった場合に起こるException
            // 入力値が原因
            // Bad Request
            log.info("new instance", e);
            throw new BadRequest("API-02400", "入力値が不正です. " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            // アクセスしてはいけないメソッド, コンストラクタを呼び出そうとした(private, protected など)
            // 設定ファイルが原因
            // Internal Server Error
            log.error("builderConfig setter IllegalAccessException", e);
            throw new InternalServerError("API-024500", "コマンドの生成に失敗しました. " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            // コンストラクタ, メソッド内でExceptionが投げられた
            // IllegalArgumentException は入力値が原因
            // Bad Request
            // その他はInternal ServerError
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                log.info("builderConfig setter", e);
                throw new BadRequest("API-02400", "入力値が不正です. " + cause.getMessage(), cause);
            }
            log.error("builderConfig setter", e);
            throw new InternalServerError("API-024500", "コマンドの生成に失敗しました. " + cause.getMessage(), cause);
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
