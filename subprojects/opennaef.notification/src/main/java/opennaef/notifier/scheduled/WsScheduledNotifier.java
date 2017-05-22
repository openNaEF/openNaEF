package opennaef.notifier.scheduled;

import opennaef.notifier.NaefSubscriber;
import opennaef.notifier.NotifyListener;
import opennaef.notifier.util.ISO8601;
import opennaef.notifier.util.Logs;
import tef.DateTime;
import tef.skelton.dto.DtoChanges;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.rmi.RemoteException;
import java.util.Formatter;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * NaEF の transaction で指定した時間が来たことを通知する Notifier
 */
@ServerEndpoint(value = "/scheduled")
public class WsScheduledNotifier {
    private static Set<Session> sessions = new CopyOnWriteArraySet<>();

    public static final NotifyListener LISTENER = (txId, dtoChanges) -> {
        DateTime time = new DateTime(dtoChanges.getTargetTime());
        ScheduledNotifier.instance().add(time, dtoChanges);
    };

    static {
        try {
            NaefSubscriber.instance().addListener(LISTENER);
            Logs.common.info("ScheduledNotifier installed.");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        Logs.scheduled.debug("onOpen: {}, connection: ", session.getId(), sessions.size());
        sessions.add(session);
    }

    @OnMessage
    public void onMessage(String msg) {
    }

    @OnError
    public void onError(Session session, Throwable t) {
        Logs.scheduled.trace("onError: {}, session: {}", t.getMessage(), session.getId(), t);
    }

    @OnClose
    public void onClose(Session session) {
        Logs.scheduled.debug("onClose: {}, connection: ", session.getId(), sessions.size());
        sessions.remove(session);
    }

    public static void sendMessage(DtoChanges dtoChanges) {
        for (Session ses : sessions) {
            Formatter formatter = new Formatter();
            Formatter msg = formatter.format(
                    "[%s] time = %s, new = %s, changed = %s",
                    dtoChanges.getTargetVersion().toString(),
                    ISO8601.format(dtoChanges.getTargetTime()),
                    dtoChanges.getNewObjects().size(),
                    dtoChanges.getChangedObjects().size());
            ses.getAsyncRemote().sendText(msg.toString());
        }
    }
}
