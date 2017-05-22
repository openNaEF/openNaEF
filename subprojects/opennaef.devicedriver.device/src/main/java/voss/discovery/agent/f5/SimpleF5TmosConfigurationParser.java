package voss.discovery.agent.f5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.AbstractSimpleConfigurationParser;
import voss.discovery.agent.common.ConfigElement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class SimpleF5TmosConfigurationParser extends AbstractSimpleConfigurationParser {
    private static final Logger log = LoggerFactory.getLogger(SimpleF5TmosConfigurationParser.class);

    public SimpleF5TmosConfigurationParser(String config) {
        super(config);
    }

    @Override
    public void parse() throws IOException {
        BlockingDeque<ConfigElement> stack = new LinkedBlockingDeque<ConfigElement>();
        ConfigElement root = new ConfigElement("root");
        this.config = root;
        stack.addLast(root);
        ConfigElement current = root;
        List<String> tokens = tokenize();
        int i = 1;
        for (String token : tokens) {
            if (log.isTraceEnabled()) {
                log.trace("token " + i++ + "/" + tokens.size() + ":" + token);
            }
            F5TmosElementType thisType = getType(token);
            if (log.isTraceEnabled()) {
                log.trace("token " + i++ + "/" + tokens.size() + "->" + thisType);
            }
            if (thisType == F5TmosElementType.LAYER_ENTER) {
                String id = token.replaceAll("[ ]+\\{", "");
                ConfigElement e = new ConfigElement(id);
                current.addElement(e);
                stack.addLast(e);
                current = e;
            } else if (thisType.equals(F5TmosElementType.LAYER_LEAVE)) {
                stack.removeLast();
                current = stack.peekLast();
            } else if (thisType.equals(F5TmosElementType.LAYER_EMPTY)) {
                String id = token.replaceAll("[ ]+\\{[ ]+\\}", "");
                ConfigElement e = new ConfigElement(id);
                current.addElement(e);
            } else if (thisType.equals(F5TmosElementType.ATTRIBUTE)) {
                current.addAttribute(token);
            }
        }
    }

    private List<String> tokenize() throws IOException {
        List<String> tokens = new ArrayList<String>();
        BufferedReader reader = null;
        String line = null;
        try {
            reader = new BufferedReader(new StringReader(this.textConfiguration));
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line == null || line.equals("")) {
                    continue;
                }
                line = deleteComment(line);
                if (line == null || line.equals("")) {
                    continue;
                }
                tokens.add(line);
            }
            return tokens;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public F5TmosElementType getType(String s) {
        assert s != null;
        s = s.trim();
        s = deleteComment(s);
        if (s.matches(".* \\{ .+ \\}")) {
            return F5TmosElementType.ATTRIBUTE;
        } else if (s.matches(".* \\{[ ]+\\}")) {
            return F5TmosElementType.LAYER_EMPTY;
        } else if (s.endsWith("{")) {
            return F5TmosElementType.LAYER_ENTER;
        } else if (s.endsWith("}")) {
            return F5TmosElementType.LAYER_LEAVE;
        } else {
            return F5TmosElementType.ATTRIBUTE;
        }
    }

    private String deleteComment(String s) {
        assert s != null;
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