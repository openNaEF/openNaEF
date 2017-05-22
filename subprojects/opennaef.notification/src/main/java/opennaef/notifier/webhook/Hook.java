package opennaef.notifier.webhook;

import opennaef.notifier.filter.FilterQuery;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Webhook のパラメータを保持する
 */
@Entity
@Table(name = "HOOKS")
@NamedQuery(name = "hook.all", query = "SELECT h FROM Hook h")
public class Hook {
    public static final String CALLBACK_URL = "callback_url";
    public static final String ACTIVE = "active";
    public static final String FILTER = "filter";
    public static final String FAILED = "failed";
    public static final String MESSAGE = "message";

    /**
     * id
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    private long _id;

    /**
     * Event を送信するURL
     */
    @Column(name = CALLBACK_URL)
    private URL _callbackURL;

    /**
     * この値が true の場合に Event を送信する
     */
    @Column(name = ACTIVE)
    private boolean _isActive = true;

    /**
     * 通知条件
     */
    @Column(name = FILTER, nullable = false)
    @Embedded
    private FilterQuery _filter = new FilterQuery();

    /**
     * 通知に失敗した場合に true
     */
    @Column(name = FAILED)
    private boolean failed = false;

    /**
     * メッセージ
     */
    @Column(name = MESSAGE)
    private String message;

    public Hook() {
    }

    public Hook(URL callbackURL) {
        if (callbackURL == null) throw new IllegalArgumentException("callback url is null.");
        _callbackURL = callbackURL;
    }

    /**
     * @return id
     */
    public long getId() {
        return _id;
    }

    /**
     * @return Event を送信するURL
     */
    public URL getCallbackURL() {
        try {
            return new URL(_callbackURL.toString());
        } catch (MalformedURLException e) {
            // この Exception は発生しないはず
            throw new RuntimeException(e);
        }
    }

    public URL setCallbackURL(URL callbackURL) {
        if (callbackURL != null) {
            _callbackURL = callbackURL;
        }
        return _callbackURL;
    }

    /**
     * @return Event を送信する場合に true
     */
    public boolean isActive() {
        return _isActive;
    }

    /**
     * Event を送信するかの設定を行う
     *
     * @param isActive active
     * @return active
     */
    public boolean setActive(boolean isActive) {
        _isActive = isActive;
        return isActive();
    }

    public boolean isFailed() {
        return failed;
    }

    /**
     * 通知に失敗した場合(failed == true) は active に false をセットする
     *
     * @param failed notify
     */
    public void setFailed(boolean failed) {
        this.failed = failed;
        if (failed) {
            setActive(true);
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public FilterQuery getFilter() {
        return _filter;
    }

    public void setFilter(FilterQuery filter) {
        _filter = filter;
    }
}
