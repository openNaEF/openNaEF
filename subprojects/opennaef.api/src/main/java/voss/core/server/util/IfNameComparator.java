package voss.core.server.util;

import java.util.*;

public class IfNameComparator implements Comparator<String> {
    private final Map<String, List<NameElementChain>> cache = new HashMap<String, List<NameElementChain>>();

    @Override
    public int compare(String ifName1, String ifName2) {
        if (Util.isNull(ifName1, ifName2)) {
            throw new IllegalStateException("IfName is null: ifName1=" + ifName1 + ", ifName2=" + ifName2);
        }
        List<NameElementChain> chains1 = cache.get(ifName1);
        if (chains1 == null) {
            chains1 = parseIfName(ifName1);
            cache.put(ifName1, chains1);
        }
        List<NameElementChain> chains2 = cache.get(ifName2);
        if (chains2 == null) {
            chains2 = parseIfName(ifName2);
            cache.put(ifName2, chains2);
        }
        int depth = Math.min(chains1.size(), chains2.size());
        for (int i = 0; i < depth; i++) {
            int r = chains1.get(i).compare(chains2.get(i));
            if (r != 0) {
                return r;
            }
        }
        return chains1.size() - chains2.size();
    }

    private List<NameElementChain> parseIfName(String ifName) {
        List<NameElementChain> chains = new ArrayList<NameElementChain>();
        String[] elements = ifName.split("[/.:]");
        for (String element : elements) {
            NameElementChain chain = new NameElementChain();
            parseElement(chain, element);
            chains.add(chain);
        }
        return chains;
    }

    private static enum ElementType {
        NUMBER,
        STRING,;
    }

    private void parseElement(NameElementChain chain, String name) {
        if (name == null) {
            chain.addElement(new StringNameElement(""));
            return;
        }
        StringBuilder sb = new StringBuilder();
        ElementType type = null;
        ElementType prev = null;
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            prev = type;
            type = getType(ch);
            if (isTypeChanged(prev, type)) {
                chain.addElement(getElement(prev, sb.toString()));
                sb = new StringBuilder();
            }
            sb.append(ch);
        }
        if (sb.length() > 0) {
            chain.addElement(getElement(type, sb.toString()));
        }
    }

    private boolean isTypeChanged(ElementType type1, ElementType type2) {
        if (type1 == null) {
            return false;
        }
        return type1 != type2;
    }

    private NameElement getElement(ElementType type, String s) {
        NameElement e;
        switch (type) {
            case NUMBER:
                long num = Long.parseLong(s);
                e = new NumberNameElement(num);
                break;
            case STRING:
                e = new StringNameElement(s);
                break;
            default:
                throw new IllegalArgumentException();
        }
        return e;
    }

    private ElementType getType(char c) {
        if ('0' <= c && c <= '9') {
            return ElementType.NUMBER;
        } else {
            return ElementType.STRING;
        }
    }

    private static class NameElementChain {
        private final List<NameElement> chain = new ArrayList<NameElement>();

        void addElement(NameElement e) {
            chain.add(e);
        }

        int compare(NameElementChain other) {
            int max = Math.min(this.chain.size(), other.chain.size());
            for (int i = 0; i < max; i++) {
                int r = this.chain.get(i).compare(other.chain.get(i));
                if (r != 0) {
                    return r;
                }
            }
            return this.chain.size() - other.chain.size();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (NameElement e : chain) {
                if (sb.length() > 0) {
                    sb.append("-");
                }
                sb.append(e.toString());
            }
            return sb.toString();
        }
    }

    private static interface NameElement {
        int compare(NameElement e);

        boolean isNumber();
    }

    private static class NumberNameElement implements NameElement {
        private final long num;

        public NumberNameElement(long num) {
            this.num = num;
        }

        public int compare(NameElement e) {
            if (!e.isNumber()) {
                return -1;
            } else {
                return Util.compareLong(num, ((NumberNameElement) e).num);
            }
        }

        public boolean isNumber() {
            return true;
        }

        @Override
        public String toString() {
            return "[i:" + num + "]";
        }
    }

    private static class StringNameElement implements NameElement {
        private final String name;

        public StringNameElement(String name) {
            this.name = name;
        }

        public int compare(NameElement e) {
            if (e.isNumber()) {
                return 1;
            } else {
                return this.name.compareTo(((StringNameElement) e).name);
            }
        }

        public boolean isNumber() {
            return false;
        }

        @Override
        public String toString() {
            return "[s:" + name + "]";
        }
    }
}