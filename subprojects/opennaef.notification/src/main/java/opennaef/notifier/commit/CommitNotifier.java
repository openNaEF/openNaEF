package opennaef.notifier.commit;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.JSONException;
import opennaef.notifier.NaefSubscriber;
import opennaef.notifier.Notifier;
import opennaef.notifier.Notify;
import opennaef.notifier.NotifyListener;
import opennaef.notifier.filter.FilterQuery;
import opennaef.notifier.filter.Filters;
import opennaef.notifier.util.ISO8601;
import opennaef.notifier.util.JsonUtils;
import opennaef.notifier.util.Logs;
import tef.DateTime;
import tef.TransactionId;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NaEF に transaction が commit されたことを通知する Notifier
 */
@ServerEndpoint(value = "/commit")
public class CommitNotifier {
    private static Map<Session, FilterQuery> _sessions = new ConcurrentHashMap<>();

    public static final NotifyListener LISTENER = (txId, dtoChanges) -> {
        Formatter msg = new Formatter().format(
                "[%s] time = %s, new = %s, changed = %s",
                dtoChanges.getTargetVersion().toString(),
                ISO8601.format(dtoChanges.getTargetTime()),
                dtoChanges.getNewObjects().size(),
                dtoChanges.getChangedObjects().size());
        Logs.commit.info(msg.toString());
        try {
            TransactionId.W targetTx = (TransactionId.W) txId;
            DateTime targetTime = new DateTime(dtoChanges.getTargetTime());
            Map<String, Object> json = Notifier.getDtoChangesJson(targetTx, targetTime);
            Notify n = new Notify(
                    Notify.Type.commit,
                    System.currentTimeMillis(),
                    dtoChanges,
                    Notifier.getDtoChangesUri(targetTx, targetTime),
                    json);
            sendMessage(n);
        } catch (IOException e) {
            Logs.common.error("fetch dto-changes", e);
        }
    };

    static {
        try {
            NaefSubscriber.instance().addListener(LISTENER);
            Logs.common.info("CommitNotifier installed.");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        Logs.commit.debug("onOpen: {}, connection: ", session.getId(), _sessions.size());
        _sessions.put(session, FilterQuery.NULL_FILTER);
    }

    @OnMessage
    public String onMessage(String query, Session session) {
        try {
            FilterQuery filter = JSON.decode(query, FilterQuery.class);
            Logs.filter.debug("update filter[{}]: {}", session.getId(), JSON.encode(filter));
            _sessions.put(session, filter);
        } catch (JSONException e) {
            return "failed " + e.getMessage();
        }
        return "updated";
    }

    @OnMessage
    public void onPong(PongMessage pong, Session session) {
//       Logs.common.trace("pong: {}", new String(pong.getApplicationData().array()));
    }

    @OnError
    public void onError(Session session, Throwable t) {
        Logs.commit.trace("onError: {}, session: {}", t.getMessage(), session.getId(), t);
    }

    @OnClose
    public void onClose(Session session) {
        Logs.commit.debug("onClose: {}, connection: ", session.getId(), _sessions.size());
        _sessions.remove(session);
    }

    public static void sendMessage(final Notify notify) {
        _sessions.entrySet().parallelStream()
                .filter(entry -> Filters.matches(entry.getValue(), notify.getRawDtoChanges()))
                .forEach(entry -> {
                    Logs.filter.trace("[notify] {} {} ", entry.getKey().getId(), JSON.encode(entry.getValue()));
                    entry.getKey().getAsyncRemote().sendText(JsonUtils.toJson(notify));
                });
    }

    public static void sendPing() {
        Logs.commit.trace("send Ping...");
        _sessions.entrySet().parallelStream()
                .forEach(entry -> {
                    try {
                        entry.getKey().getBasicRemote().sendPing(ByteBuffer.wrap("open-naef.notifier".getBytes()));
                    } catch (IOException e) {
                        Logs.commit.error("ping failed.", e);
                    }
                });
    }
}
