package voss.discovery.agent.netscreen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.AbstractSimpleConfigurationParser;
import voss.discovery.agent.common.ConfigElement;
import voss.discovery.agent.common.ContextEvent;
import voss.discovery.agent.util.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class SimpleScreenOSCommandParser extends AbstractSimpleConfigurationParser {
    private static final Logger log = LoggerFactory.getLogger(SimpleScreenOSCommandParser.class);

    public SimpleScreenOSCommandParser(String textConfig) {
        super(textConfig);
    }

    @Override
    public void parse() throws IOException {
        Set<String> ignoreLines = new HashSet<String>();
        ignoreLines.add("");
        ignoreLines.add("Total Config size.*");

        List<String> lines = new ArrayList<String>();
        BufferedReader configReader = null;
        try {
            configReader = new BufferedReader(new StringReader(this.textConfiguration));
            String line = null;
            while ((line = configReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                lines.add(line);
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
        lines = Utils.filter(lines, ignoreLines);

        BlockingDeque<ConfigElement> stack = new LinkedBlockingDeque<ConfigElement>();
        ConfigElement root = new ConfigElement("root");
        stack.addLast(root);

        if (lines.size() == 0) {
            this.config = root;
            return;
        }

        ConfigElement current = root;

        PARSECONFIG:
        for (int pos = 0; pos < lines.size(); pos++) {
            String line = lines.get(pos);
            ContextEvent c = getContext(line);
            ConfigElement sub = null;
            switch (c) {
                case CONTINUE:
                    current.addAttribute(line.trim());
                    break;
                case ENTER_SUBCONTEXT:
                    sub = new ConfigElement(line);
                    sub.addAttribute(line.trim());
                    current.addElement(sub);
                    current = sub;
                    stack.addLast(sub);
                    break;
                case LEAVE_SUBCONTEXT:
                    if (stack.peekLast() != root) {
                        stack.removeLast();
                        current = stack.peekLast();
                    }
                    break;
                case LEAVE_AND_ENTER_SUBCONTEXT:
                    if (stack.peekLast() != root) {
                        stack.removeLast();
                        current = stack.peekLast();
                    }
                    sub = new ConfigElement(line);
                    sub.addAttribute(line.trim());
                    current.addElement(sub);
                    current = sub;
                    stack.addLast(sub);
                    break;
                case END:
                    break PARSECONFIG;
            }
        }
        this.config = root;
    }

    private ContextEvent getContext(String line) {
        if (line.equals("set vrouter trust-vr sharable")) {
            return ContextEvent.CONTINUE;
        } else if (line.startsWith("set vrouter name")) {
            return ContextEvent.CONTINUE;
        } else if (line.startsWith("set vrouter ")) {
            return ContextEvent.ENTER_SUBCONTEXT;
        } else if (line.equals("exit")) {
            return ContextEvent.LEAVE_SUBCONTEXT;
        }
        return ContextEvent.CONTINUE;
    }
}