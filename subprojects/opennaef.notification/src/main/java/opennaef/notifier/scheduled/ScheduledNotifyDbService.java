package opennaef.notifier.scheduled;

import opennaef.notifier.config.NotifierConfig;
import opennaef.notifier.util.Logs;
import tef.TransactionId;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * scheduled-notifier の DB
 */
public class ScheduledNotifyDbService {
    private static final String SELECT = "SELECT * FROM CHANGES ORDER BY TIME ASC";
    private static final String INSERT = "INSERT INTO CHANGES (TXID, TIME, DONE) VALUES (?, ?, false)";
    private static final String NOTIFIED = "UPDATE CHANGES SET DONE=true WHERE TXID=? AND TIME=?";

    private static final ScheduledNotifyDbService _instance = new ScheduledNotifyDbService();
    private Connection _conn;

    private ScheduledNotifyDbService() {
    }

    public static ScheduledNotifyDbService instance() {
        return ScheduledNotifyDbService._instance;
    }

    public Connection connect() throws SQLException {
        if (_conn != null && !_conn.isClosed()) {
            return _conn;
        }

        NotifierConfig conf = NotifierConfig.instance();
        try {
            _conn = DriverManager.getConnection("jdbc:derby:" + conf.scheduledNotifierDB() + ";create=true");
            if (_conn.getWarnings() == null) {
                // db が新規作成された
                Logs.scheduled.info("db create.");
                init(_conn);
            }
        } catch (SQLException se) {
            for (SQLException e = se; e != null; e = e.getNextException()) {
                Logs.scheduled.error("%s: %s", e.getSQLState(), e.getMessage());
            }
            throw se;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return _conn;
    }

    /**
     * dbの初期設定を行う
     */
    private static void init(Connection conn) throws SQLException {
        Statement stat = conn.createStatement();
        stat.execute("CREATE TABLE CHANGES(" +
                "ID INT NOT NULL GENERATED ALWAYS AS IDENTITY," +
                "TXID VARCHAR (256) NOT NULL," +
                "TIME DATE NOT NULL," +
                "DONE BOOLEAN NOT NULL" +
                ")");
    }

    public NotifyItem insert(TransactionId.W tx, Date time) throws SQLException {
        executeUpdate(connect(), INSERT, tx.getIdString(), time);
        Logs.scheduled.info("[insert] success. " + tx + " " + time);
        return new NotifyItem(tx, time);
    }

    public NotifyItem notified(NotifyItem item) throws SQLException {
        return notified((TransactionId.W) item.tx(), item.time());
    }

    public NotifyItem notified(TransactionId.W tx, Date time) throws SQLException {
        executeUpdate(connect(), NOTIFIED, tx.getIdString(), time);
        Logs.scheduled.info("[notified] success. " + tx + " " + time);
        return new NotifyItem(tx, time, true);
    }

    private static void executeUpdate(Connection conn, String sql, String tx, Date time) {
        PreparedStatement stat;
        try {
            stat = conn.prepareStatement(sql);
            stat.setString(1, tx);
            stat.setDate(2, new java.sql.Date(time.getTime()));

            stat.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<NotifyItem> getItems() throws SQLException {
        Connection conn = connect();
        Statement stat = conn.createStatement();
        ResultSet result = stat.executeQuery(SELECT);

        List<NotifyItem> items = new ArrayList<>();
        while (result.next()) {
            boolean isDone = result.getBoolean("DONE");
            if (isDone) {
                continue;
            }

            String txStr = result.getString("TXID");
            Date date = result.getDate("TIME");
            NotifyItem item = new NotifyItem(TransactionId.getInstance(txStr), date);
            items.add(item);
        }
        return items;
    }

    private void close() throws SQLException {
        if (_conn != null) {
            _conn.close();
        }
    }

    private void shutdown() throws SQLException {
        try {
            close();
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
            throw new SQLException("[fail] db shutdown.");
        } catch (SQLException se) {
            if ("XJ015".equals(se.getSQLState())) {
                //正常にシャットダウンされた
//                se.printStackTrace();
            } else {
                //シャットダウン失敗
                throw se;
            }
        }
    }
}
