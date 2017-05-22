package opennaef.rest.notifier;

import opennaef.rest.DateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pasaran.naef.DtoChangesUtil;
import tef.DateTime;
import tef.TransactionId;
import tef.skelton.dto.DtoChanges;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * naef の Transaction で指定した時間が来たら通知する
 */
public class ScheduledNotifier {
    private static final Logger log = LoggerFactory.getLogger(ScheduledNotifier.class);

    private static final ScheduledNotifier _instance = new ScheduledNotifier();
    private final ScheduledExecutorService _pool;
    private final Set<ScheduledNotifyListener> _listener = new CopyOnWriteArraySet<>();

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
            List<NotifyItem> items = NotifyDbService.instance().getItems();
            items.parallelStream().filter(item -> !item.isDone()).forEach(this::addNotifyTask);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<NotifyItem> getItems() throws SQLException {
        return NotifyDbService.instance().getItems();
    }

    public NotifyItem add(DateTime time, DtoChanges dtoChanges) {
        log.info("[add] " + dtoChanges.getTargetVersion() + " " + time);
        TransactionId.W tx = dtoChanges.getTargetVersion();
        Date date = time.toJavaDate();

        NotifyItem item = null;
        try {
            item = NotifyDbService.instance().insert(tx, date);
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
            log.info("scheduled runnable." + DateFormat.format(item.time()) + " " + item.tx());

            try {
                DtoChanges dtoChanges = DtoChangesUtil.getDtoChanges(item.timeMillis(), (TransactionId.W) item.tx());
                _listener.parallelStream().forEach(listener -> {
                    listener.notify(dtoChanges);
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            log.debug("dtoChanges success." + DateFormat.format(item.time()) + " " + item.tx());


            log.debug("notify success." + DateFormat.format(item.time()) + " " + item.tx());
            try {
                NotifyDbService.instance().notified(item);
            } catch (SQLException e) {
                log.error("[db write error] " + DateFormat.format(item.time()) + " " + item.tx());
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    public ScheduledNotifyListener addListener(ScheduledNotifyListener listener) {
        boolean added = _listener.add(listener);
        return added ? listener : null;
    }

    public ScheduledNotifyListener removeListener(ScheduledNotifyListener listener) {
        boolean removed = _listener.remove(listener);
        return removed ? listener : null;
    }
}
