package opennaef.notifier;

import net.arnx.jsonic.JSON;
import opennaef.notifier.commit.CommitNotifier;
import opennaef.notifier.config.NotifierConfig;
import opennaef.notifier.util.Logs;
import opennaef.notifier.webhook.Webhooks;
import tef.DateTime;
import tef.TransactionId;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Formatter;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Notifier
 */
@WebListener
public class Notifier implements ServletContextListener {
    public static final String KEY_ROOT_DIR = "notification.dir";
    public static final String DEFAULT_ROOT_DIR_NAME = "./";
    public static final String BASE_DIR = System.getProperty(KEY_ROOT_DIR, DEFAULT_ROOT_DIR_NAME);

    private static Notifier _instance;

    public Notifier() {
    }

    public static synchronized void start() {
        if (_instance != null) return;
        Logs.common.info("Notifier starting...");
        try {
            NaefRmiConnector conn = NaefRmiConnector.instance();
            conn.connect();
            conn.dtoFacade().addDtoChangeListener(NaefSubscriber.LISTENER_NAME, NaefSubscriber.instance());
            _instance = new Notifier();
        } catch (Exception e) {
            Logs.common.error("startup failed", e);
            throw new RuntimeException("notifier startup failed", e);
        }
    }

    public static void main(String[] args) throws Exception {
        start();
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Notifier.start();

        // scheduled-notifier start
//        new WsScheduledNotifier();

        // webhook start
        Webhooks.instance();

        try {
            long pingDelay = NotifierConfig.instance().pingDelay();
            if (pingDelay > 0) {
                ScheduledExecutorService pinging = Executors.newSingleThreadScheduledExecutor();
                pinging.scheduleWithFixedDelay(
                        CommitNotifier::sendPing,
                        10L,
                        pingDelay,
                        TimeUnit.SECONDS);
            }
        } catch (IOException e) {
            Logs.common.error("ping", e);
        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    /**
     * 設定ファイルを元に NaEF Restful API の DtoChanges 取得 API の URL を生成する
     * <p>
     * http://{naef-addr}:{naef-rest-api-port}/api/{naef-rest-api-version}/dto-changes?version={tx}&time={time}
     *
     * @param tx   ターゲットとなるトランザクション ID
     * @param time ターゲットとなる時間
     * @return NaEF Restful API DtoChanges URL
     */
    public static URL getDtoChangesUri(TransactionId.W tx, DateTime time) throws IOException {
        NotifierConfig conf = NotifierConfig.instance();
        String urlStr = new Formatter().format(
                "http://%s:%s/api/%s/dto-changes",
                conf.naefAddr(),
                conf.naefRestApiPort(),
                conf.naefRestApiVersion())
                .toString();

        StringJoiner queries = new StringJoiner("&");
        if (tx != null) {
            queries.add("version=" + tx.toString());
        }
        if (time != null) {
            queries.add("time=" + time.getValue());
        }

        if (queries.length() > 0) {
            urlStr += "?" + queries.toString();
        }

        return new URL(urlStr);
    }

    /**
     * NaEF Restful API から DtoChanges JSON を取得する
     *
     * @param tx   ターゲットとなるトランザクション ID
     * @param time ターゲットとなる時間
     * @return DtoChanges JSON
     */
    public static Map<String, Object> getDtoChangesJson(TransactionId.W tx, DateTime time) throws IOException {
        URL url = getDtoChangesUri(tx, time);

        Map<String, Object> res;
        URLConnection conn = url.openConnection();
        try (InputStream is = conn.getInputStream()) {
            res = JSON.decode(is);
        }
        return res;
    }
}
