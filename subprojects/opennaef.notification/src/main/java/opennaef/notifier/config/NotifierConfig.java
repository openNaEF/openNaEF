package opennaef.notifier.config;

import opennaef.notifier.Notifier;
import opennaef.notifier.util.Logs;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

/**
 * Notifier の設定を管理する
 * config/notification.yaml
 */
public class NotifierConfig {
    private static final String CONFIG_DIR = "config";
    public static final String CONFIG_FILE = "notification.yaml";
    private static final NotifierConfig _instance = new NotifierConfig();

    public static NotifierConfig instance() {
        return NotifierConfig._instance;
    }

    private Map<String, Object> _conf;

    private NotifierConfig() {
    }

    public void load() throws IOException {
        load(false);
    }

    /**
     * コンフィグファイルを読み込む
     * <p>
     * reload == true の場合はコンフィグファイルをリロードする
     *
     * @param reload リロード
     */
    @SuppressWarnings("unchecked")
    public void load(boolean reload) throws IOException {
        if (_conf != null && !reload) return;

        Path path = Paths.get(Notifier.BASE_DIR, CONFIG_DIR, CONFIG_FILE);
        Logs.common.debug("config reload: {}", path.toAbsolutePath());

        try (InputStream is = Files.newInputStream(path)) {
            Yaml yaml = new Yaml();
            _conf = (Map<String, Object>) yaml.load(is);
        } catch (IOException ioe) {
            Logs.common.error("config load failed", ioe);
            throw new IOException("設定ファイルの読み込みに失敗しました. file: " + path.toAbsolutePath().toString(), ioe);
        }
    }

    /**
     * @return NaEF address
     */
    public String naefAddr() throws IOException {
        load();
        return Objects.toString(_conf.get("naef-addr"), "localhost");
    }

    /**
     * @return NaEF port
     */
    public String naefPort() throws IOException {
        load();
        return Objects.toString(_conf.get("naef-port"), "38100");
    }

    /**
     * @return NaEF service name
     */
    public String naefServiceName() throws IOException {
        load();
        return Objects.toString(_conf.get("naef-service-name"), "");
    }

    /**
     * @return NaEF RMC port
     */
    public int naefRmcPort() throws IOException {
        load();
        return Integer.parseInt(Objects.toString(_conf.get("naef-rmc-port"), "38105"));
    }

    /**
     * @return NaEF Restful API port
     */
    public int naefRestApiPort() throws IOException {
        load();
        return Integer.parseInt(Objects.toString(_conf.get("naef-rest-api-port"), "2510"));
    }

    /**
     * @return NaEF Restful API version
     */
    public String naefRestApiVersion() throws IOException {
        load();
        return Objects.toString(_conf.get("naef-rest-api-version"), "v1");
    }

    /**
     * @return NaEF RMC port
     */
    @SuppressWarnings("unchecked")
    public String scheduledNotifierDB() throws IOException {
        load();
        Map<String, String> snConf = (Map<String, String>) _conf.get("scheduled-notifier");
        return snConf == null ? null : snConf.get("db");
    }

    /**
     * @return Ping delay
     */
    public long pingDelay() throws IOException {
        load();
        return Long.parseLong(Objects.toString(_conf.get("ping-delay"), "3"));
    }
}
