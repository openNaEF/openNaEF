package opennaef.rest.api.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class Config<T> {
    public static final String CONFIG_DIR = "config";
    private final String _fileName;
    private final Class<T> _type;
    private T _config;

    public Config(String fileName, Class<T> type) {
        _fileName = fileName;
        _type = type;
    }

    protected synchronized void load(boolean reload) throws ConfigurationException {
        if (reload || _config == null) {
            Path path = Paths.get(CONFIG_DIR, _fileName);
            try (InputStream is = Files.newInputStream(path)) {
                Yaml yaml = new Yaml();
                T flesh = loadInner(yaml, is);
                this._config = flesh;
            } catch (IOException ioe) {
                throw new ConfigurationException("設定ファイルの読み込みに失敗しました. file: " + path.getFileName(), ioe);
            }
        }
    }

    protected synchronized T loadInner(Yaml yaml, InputStream is) throws ConfigurationException {
        T flesh = yaml.loadAs(is, configType());
        return flesh;
    }

    protected synchronized void load() throws ConfigurationException {
        load(false);
    }

    public synchronized void reload() throws ConfigurationException {
        load(true);
    }

    protected T config() {
        return this._config;
    }

    protected Class<T> configType() {
        return this._type;
    }
}
