package voss.discovery.agent.alcatel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.AbstractSimpleConfigurationParser;
import voss.discovery.agent.common.ConfigElement;
import voss.discovery.agent.common.ConfigurationStructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class SimpleAlcatelConfigParser extends AbstractSimpleConfigurationParser {

    private static final Logger log = LoggerFactory.getLogger(SimpleAlcatelConfigParser.class);

    public SimpleAlcatelConfigParser(String textConfig) {
        super(textConfig);
    }

    @Override
    public String getConfiguration() {
        return this.textConfiguration;
    }

    @Override
    public void parse() throws IOException {

        List<String> lines = new ArrayList<String>();
        BufferedReader configReader = null;
        try {
            configReader = new BufferedReader(new StringReader(this.textConfiguration));
            String line = null;
            while ((line = configReader.readLine()) != null) {
                String commentdeletedline = deleteComment(line);
                if (commentdeletedline != null && commentdeletedline.length() != 0) {
                    lines.add(commentdeletedline);
                }
            }
        } catch (IOException ioe) {
            log.error(ioe.toString());
        } finally {
            if (configReader != null) {
                try {
                    configReader.close();
                } catch (IOException ioe) {
                    log.warn(ioe.toString());
                }
            }
        }

        ConfigElement root = new ConfigElement("root");
        this.config = root;
        if (lines.size() == 0) {
            this.config = root;
            return;
        }

        ConfigElement current = root;
        int lastLevel = -1;

        int pos = 0;
        while (pos < lines.size() && lines.get(pos).matches("configure") == false) {
            pos++;
        }
        for (; pos < lines.size(); pos++) {
            String line = lines.get(pos);

            int level = 0;
            String indent = "    ";
            while (line.startsWith(indent)) {
                level++;
                indent = indent + "    ";
            }
            line = line.trim();

            if (line.startsWith("exit")) {
                continue;
            }

            for (int i = 0; i <= lastLevel - level; i++) {
                current = current.getParent();
            }

            ConfigElement sub = current.getElementById(line);
            if (sub == null) {
                sub = new ConfigElement(line);
                current.addElement(sub);
            }

            current = sub;

            lastLevel = level;
        }
    }

    @Override
    public ConfigurationStructure getConfigurationStructure() {
        return new ConfigurationStructure(config);
    }

    private String deleteComment(String s) {
        assert s != null;

        if (s.startsWith("echo ")) {
            return "";
        }

        StringReader reader = null;
        StringBuffer result = new StringBuffer();
        try {
            reader = new StringReader(s);
            int c = -1;
            boolean quoted = false;
            boolean terminated = false;
            while ((c = reader.read()) != -1) {
                if (terminated) {
                    continue;
                } else if (c == '#' && !quoted) {
                    terminated = true;
                } else if (c == '"') {
                    quoted = !quoted;
                    result.append((char) c);
                    continue;
                } else if (c == ';' && !quoted) {
                    terminated = true;
                    result.append((char) c);
                    break;
                } else if (!terminated) {
                    result.append((char) c);
                    continue;
                } else {
                    throw new IllegalStateException(
                            "unknown char: [" + (char) c + "] in [" + s + "], ["
                                    + result.toString() + "]");
                }
            }
            return result.toString();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return null;
    }
}