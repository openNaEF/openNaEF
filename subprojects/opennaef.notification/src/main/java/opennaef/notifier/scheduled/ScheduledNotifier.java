package opennaef.notifier.scheduled;

import lib38k.rmc.RmcClientService;
import opennaef.notifier.config.NotifierConfig;
import opennaef.notifier.util.ISO8601;
import opennaef.notifier.util.Logs;
import pasaran.naef.rmc.GetDtoChanegs;
import tef.DateTime;
import tef.TransactionId;
import tef.skelton.dto.DtoChanges;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * naef の Transaction で指定した時間が来たら通知する
 */
public class ScheduledNotifier {
    private static final ScheduledNotifier _instance = new ScheduledNotifier();
    private final ScheduledExecutorService _pool;

    private ScheduledNotifier() {
        _pool = Executors.newScheduledThreadPool(1);
        init();
    }

    public static ScheduledNotifier instance() {
        return ScheduledNotifier._instance;
    }

    /**
     * 起動時にDBからNotifyItemを取り出し、タスクを登録する
     */
    private void init() {
        try {
            List<NotifyItem> items = ScheduledNotifyDbService.instance().getItems();
            items.parallelStream().filter(item -> !item.isDone()).forEach(this::addNotifyTask);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<NotifyItem> getItems() throws SQLException {
        return ScheduledNotifyDbService.instance().getItems();
    }

    public NotifyItem add(DateTime time, DtoChanges dtoChanges) {
        Logs.scheduled.info("[add] {} {}", dtoChanges.getTargetVersion(), time);
        TransactionId.W tx = dtoChanges.getTargetVersion();
        Date date = time.toJavaDate();

        NotifyItem item = null;
        try {
            item = ScheduledNotifyDbService.instance().insert(tx, date);
            addNotifyTask(item);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return item;
    }


    private void addNotifyTask(NotifyItem item) {
        long now = System.currentTimeMillis();
        long delay = Math.max(item.timeMillis() - now, 0);

        // 現在時刻より以前のものは即時実行される
        _pool.schedule(() -> {
            Logs.scheduled.info("scheduled runnable. {} {}", ISO8601.format(item.time().getTime()), item.tx());

            // rmc を使って naef から DtoChanges を取得する
            NotifierConfig conf = NotifierConfig.instance();
            InetSocketAddress rmcAddr = null;
            try {
                rmcAddr = new InetSocketAddress(conf.naefAddr(), conf.naefRmcPort());
            } catch (IOException e) {
                Logs.scheduled.error("add notify task fail", e);
            }
            RmcClientService rmc = new RmcClientService.Remote(null, rmcAddr);
            GetDtoChanegs.Call call = new GetDtoChanegs.Call(item.timeMillis(), (TransactionId.W) item.tx());
            DtoChanges dtoChanges = rmc.call(call);

            Logs.scheduled.debug("dtoChanges success. {} {}", ISO8601.format(item.time().getTime()), item.tx());

            WsScheduledNotifier.sendMessage(dtoChanges);
            Logs.scheduled.debug("notify success. {} {}", ISO8601.format(item.time().getTime()), item.tx());

            try {
                ScheduledNotifyDbService.instance().notified(item);
            } catch (SQLException e) {
                Logs.scheduled.error("[db write error] {} {}", ISO8601.format(item.time().getTime()), item.tx());
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
}
