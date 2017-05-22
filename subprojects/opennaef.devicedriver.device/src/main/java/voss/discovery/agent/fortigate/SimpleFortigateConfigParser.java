package voss.discovery.agent.fortigate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.AbstractSimpleConfigurationParser;
import voss.discovery.agent.common.ConfigElement;
import voss.discovery.agent.common.ContextEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class SimpleFortigateConfigParser extends AbstractSimpleConfigurationParser {
    private final static Logger log = LoggerFactory.getLogger(SimpleFortigateConfigParser.class);
    public static final String TOKEN_CONFIG = "config";
    public static final String TOKEN_EDIT = "edit";
    public static final String TOKEN_NEXT = "next";
    public static final String TOKEN_END = "end";

    public SimpleFortigateConfigParser(String config) {
        super(config);
    }

    public void parse() {
        BlockingDeque<ConfigElement> stack = new LinkedBlockingDeque<ConfigElement>();
        ConfigElement root = new FortigateConfigElement(false, "root");
        stack.addLast(root);
        ConfigElement current = root;
        this.config = root;

        BufferedReader configReader = null;
        try {
            configReader = new BufferedReader(new StringReader(this.textConfiguration));
            String line = null;
            int lines = 0;
            int depth = 0;
            while ((line = configReader.readLine()) != null) {
                line = line.trim();
                lines++;
                log.trace("[" + depth + ":" + lines + "]" + line);
                if (isIgnorable(line)) {
                    continue;
                }
                boolean isEditType = line.startsWith("edit");
                ContextEvent c = getContext(line);
                log.trace("- " + c.name());
                ConfigElement sub = null;
                switch (c) {
                    case CONTINUE:
                        current.addAttribute(line.trim());
                        break;
                    case ENTER_SUBCONTEXT:
                        sub = new FortigateConfigElement(isEditType, line);
                        sub.addAttribute(line);
                        current.addElement(sub);
                        current = sub;
                        stack.addLast(sub);
                        depth++;
                        break;
                    case LEAVE_SUBCONTEXT:
                        if (stack.peekLast() == root) {
                            throw new IllegalStateException();
                        }
                        stack.removeLast();
                        current = stack.peekLast();
                        depth--;
                        break;
                    case END_SUBCONTEXT:
                        if (stack.peekLast() == root) {
                            throw new IllegalStateException();
                        }
                        if (isEditType(current)) {
                            stack.removeLast();
                            current = stack.peekLast();
                            depth--;
                        }
                        stack.removeLast();
                        current = stack.peekLast();
                        depth--;
                        break;
                    default:
                        throw new IllegalStateException("unexpected: " + c);
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
    }

    private boolean isIgnorable(String line) {
        if (line.startsWith("foo bar baz")) {
            return true;
        }
        return false;
    }

    private ContextEvent getContext(String line) {
        if (line.startsWith("config ")) {
            return ContextEvent.ENTER_SUBCONTEXT;
        } else if (line.startsWith("edit ")) {
            return ContextEvent.ENTER_SUBCONTEXT;
        } else if (line.equals("next")) {
            return ContextEvent.LEAVE_SUBCONTEXT;
        } else if (line.equals("end")) {
            return ContextEvent.END_SUBCONTEXT;
        }
        return ContextEvent.CONTINUE;
    }

    private boolean isEditType(ConfigElement element) {
        if (FortigateConfigElement.class.isInstance(element)) {
            return ((FortigateConfigElement) element).isEditType();
        } else {
            return false;
        }
    }

    private static class FortigateConfigElement extends ConfigElement {
        private final boolean isEditType;
        public FortigateConfigElement(boolean isEditType, String id) {
            super(id);
            this.isEditType = isEditType;
        }

        public boolean isEditType() {
            return this.isEditType;
        }
    }
}