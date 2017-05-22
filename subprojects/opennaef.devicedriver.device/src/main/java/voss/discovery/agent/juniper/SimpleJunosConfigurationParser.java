package voss.discovery.agent.juniper;

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

public class SimpleJunosConfigurationParser extends AbstractSimpleConfigurationParser {
    private static final Logger log = LoggerFactory.getLogger(SimpleJunosConfigurationParser.class);

    public SimpleJunosConfigurationParser(String config) {
        super(config);
    }

    @Override
    public void parse() throws IOException {
        BlockingDeque<ConfigElement> stack = new LinkedBlockingDeque<ConfigElement>();
        ConfigElement root = new ConfigElement("root");
        this.config = root;
        stack.addLast(root);
        ConfigElement current = root;
        StringBuilder sb = new StringBuilder();
        readConfigurationText(sb);
        List<String> tokens = tokenize(sb);
        int i = 1;
        for (String token : tokens) {
            if (log.isTraceEnabled()) {
                log.trace("token " + i++ + "/" + tokens.size() + ":" + token);
            }
            JunosElementType thisType = getType(token);
            if (thisType == JunosElementType.LAYER_ENTER) {
                String id = token.replaceAll("[ ]+\\{", "");
                ConfigElement e = new ConfigElement(id);
                current.addElement(e);
                stack.addLast(e);
                current = e;
            } else if (thisType.equals(JunosElementType.LAYER_LEAVE)) {
                stack.removeLast();
                current = stack.peekLast();
            } else if (thisType.equals(JunosElementType.ATTRIBUTE)) {
                token = token.substring(0, token.length() - 1);
                current.addAttribute(token);
            }
        }
    }

    private void readConfigurationText(StringBuilder sb) throws IOException {
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
                sb.append(line).append(" ");
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private List<String> tokenize(StringBuilder sb) throws IOException {
        List<String> tokens = new ArrayList<String>();
        StringBuilder cmd = new StringBuilder();
        StringReader sr = new StringReader(sb.toString());
        int ch = -1;
        boolean quoted = false;
        while ((ch = sr.read()) != -1) {
            if (quoted) {
                if ('\"' == ch) {
                    quoted = false;
                }
                cmd.append((char) ch);
                continue;
            }
            if (cmd.length() == 0 && ' ' == ch) {
                continue;
            }
            if ('{' == ch || '}' == ch || ';' == ch) {
                cmd.append((char) ch);
                tokens.add(cmd.toString());
                cmd = new StringBuilder();
                continue;
            }
            if ('\"' == ch) {
                quoted = true;
            }
            cmd.append((char) ch);
        }
        if (cmd.length() > 0) {
            String _cmd = cmd.toString().trim();
            if (_cmd.length() > 0) {
                tokens.add(cmd.toString());
            }
        }
        return tokens;
    }

    public JunosElementType getType(String s) {
        assert s != null;
        s = s.trim();
        s = deleteComment(s);
        if (s.endsWith("{")) {
            return JunosElementType.LAYER_ENTER;
        } else if (s.endsWith("}")) {
            return JunosElementType.LAYER_LEAVE;
        } else if (s.endsWith(";")) {
            return JunosElementType.ATTRIBUTE;
        } else {
            return null;
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