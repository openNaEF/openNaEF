package opennaef.notifier.webhook;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import opennaef.notifier.Notifier;
import opennaef.notifier.Notify;
import opennaef.notifier.util.JsonUtils;
import opennaef.notifier.util.Logs;
import org.glassfish.jersey.client.ClientProperties;
import tef.DateTime;
import tef.TransactionId;
import tef.skelton.dto.DtoChanges;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Webhook の通知情報を集計・管理する
 */
public class HookAggregater {
    private static final RetryPolicy RETRY_POLICY;
    private static final Client CLIENT;

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    static {
        CLIENT = ClientBuilder.newClient();
        CLIENT.property(ClientProperties.CONNECT_TIMEOUT, 3000);
        CLIENT.property(ClientProperties.READ_TIMEOUT, 3000);

        RETRY_POLICY = new RetryPolicy()
                .<Response>retryIf(res -> res != null && (res.getStatus() / 100) != 2)
                .withDelay(5, TimeUnit.SECONDS)
                .withMaxRetries(3);
    }

    /**
     * DtoChanges を POST する
     *
     * @param dtoChanges DtoChanges
     * @param hooks      hooks
     * @throws IOException
     */
    public static void sendDtoChanges(DtoChanges dtoChanges, List<Hook> hooks) throws IOException {
        // dto-changes -> JSON
        TransactionId.W targetTx = dtoChanges.getTargetVersion();
        DateTime targetTime = new DateTime(dtoChanges.getTargetTime());
        Map<String, Object> json = Notifier.getDtoChangesJson(targetTx, targetTime);
        Notify item = new Notify(
                Notify.Type.commit,
                System.currentTimeMillis(),
                dtoChanges,
                Notifier.getDtoChangesUri(targetTx, targetTime),
                json);
        String message = JsonUtils.toJson(item);

        hooks.parallelStream()
                .filter(Hook::isActive)
                .forEach(hook -> EXECUTOR_SERVICE.submit(new Poster(hook, message)));
    }

    /**
     * PING を送信する
     *
     * @param hook hook
     * @return ping に成功した場合に true
     */
    public static boolean sendPing(Hook hook) {
        Map<String, Object> mes = new HashMap<>();
        mes.put("type", Notify.Type.ping);
        mes.put("hook", hook);

        Invocation post = CLIENT
                .target(hook.getCallbackURL().toString())
                .request()
                .buildPost(Entity.entity(JsonUtils.toJson(mes), MediaType.APPLICATION_JSON_TYPE));

        try {
            Response res = Failsafe.with(RETRY_POLICY).get(() -> post.invoke());
            if (res.getStatus() / 100 == 2) {
                Logs.hook.debug("PING success. {}: {}", hook.getId(), hook.getCallbackURL());
                return true;
            } else {
                Logs.hook.info("PING fail. {}: {}", hook.getId(), hook.getCallbackURL());
                return false;
            }
        } catch (ProcessingException e) {
            // request 失敗, ホストが見つからない等
            Logs.hook.info("PING fail. {}: {}", hook.getId(), hook.getCallbackURL());
            return false;
        } catch (Exception e) {
            Logs.hook.error("POST fail", e);
            return false;
        }
    }

    private static class Poster implements Runnable {
        private final Hook _hook;   // 危ない
        private final String _callbackURL;
        private final String _message;

        Poster(Hook hook, String reqBody) {
            _hook = hook;
            _callbackURL = _hook.getCallbackURL().toString();
            _message = reqBody;
        }

        @Override
        public void run() {
            Invocation post = CLIENT
                    .target(_callbackURL)
                    .request()
                    .buildPost(Entity.entity(_message, MediaType.APPLICATION_JSON_TYPE));

            try {
                Response res = Failsafe.with(RETRY_POLICY).get(() -> {
                    Logs.hook.debug("send.. {}", _callbackURL);
                    return post.invoke();
                });

                if (res.getStatus() / 100 == 2) {
                    Webhooks.instance().notifySuccess(_hook);
                } else {
                    Logs.hook.info("POST fail. {}: {}", _hook.getId(), _hook.getCallbackURL());
                    Webhooks.instance().notifyFailed(_hook);
                }
            } catch (ProcessingException e) {
                // request 失敗, ホストが見つからない等
                Logs.hook.info("POST fail. {}: {}", _hook.getId(), _hook.getCallbackURL());
                Webhooks.instance().notifyFailed(_hook);
            } catch (Exception e) {
                Logs.hook.error("POST fail", e);
                Webhooks.instance().notifyFailed(_hook);
            }
        }
    }

}
