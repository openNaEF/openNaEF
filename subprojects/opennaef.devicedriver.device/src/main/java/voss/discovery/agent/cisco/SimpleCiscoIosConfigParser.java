package voss.discovery.agent.cisco;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.AbstractSimpleConfigurationParser;
import voss.discovery.agent.common.ConfigElement;
import voss.discovery.agent.common.ContextEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class SimpleCiscoIosConfigParser extends AbstractSimpleConfigurationParser {
    private final static Logger log = LoggerFactory
            .getLogger(SimpleCiscoIosConfigParser.class);

    public SimpleCiscoIosConfigParser(String config) {
        super(config);
    }

    public void parse() {
        Set<String> ignoreLines = new HashSet<String>();
        ignoreLines.add("");
        ignoreLines.add(".*#sho?w? run[a-z-]*");
        ignoreLines.add("Building configuration.*");
        ignoreLines.add("Current configuration.*");

        List<String> lines = new ArrayList<String>();
        BufferedReader configReader = null;
        try {
            configReader = new BufferedReader(new StringReader(this.textConfiguration));
            String line = null;
            LINE:
            while ((line = configReader.readLine()) != null) {
                for (String ignore : ignoreLines) {
                    if (line.matches(ignore)) {
                        log.trace("ignored: " + line);
                        continue LINE;
                    }
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
            ContextEvent c = getContext(lines, pos);
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

    private ContextEvent getContext(List<String> lines, int pos) {
        assert lines.size() > 0;
        assert pos >= 0;
        assert pos < lines.size();

        if (pos == 0) {
            return ContextEvent.CONTINUE;
        }
        if (pos == (lines.size() - 1)) {
            return ContextEvent.END;
        }

        String prev = lines.get(pos - 1);
        String curr = lines.get(pos);
        String next = lines.get(pos + 1);

        if (curr.equals("!")) {
            return ContextEvent.LEAVE_SUBCONTEXT;
        }

        if (curr.startsWith(" ")) {
            return ContextEvent.CONTINUE;
        } else {
            if (prev.startsWith(" ") && next.startsWith(" ")) {
                return ContextEvent.LEAVE_AND_ENTER_SUBCONTEXT;
            }

            if (prev.startsWith(" ") && !next.startsWith(" ")) {
                return ContextEvent.LEAVE_SUBCONTEXT;
            }

            if (!prev.startsWith(" ") && next.startsWith(" ")) {
                return ContextEvent.ENTER_SUBCONTEXT;
            }

            if (!prev.startsWith(" ") && !next.startsWith(" ")) {
                return ContextEvent.CONTINUE;
            }
        }

        return ContextEvent.CONTINUE;
    }
}