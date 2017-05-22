package voss.multilayernms.inventory.web.search;

import naef.dto.LocationDto;
import naef.dto.NaefDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.MVO.MvoId;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.database.LocationType;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.LocationRenderer;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.web.location.LocationUtil;
import voss.multilayernms.inventory.web.location.LocationViewPage;
import voss.multilayernms.inventory.web.node.NodeEditPage;
import voss.multilayernms.inventory.web.node.NodePageUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.UrlUtil;

import java.io.Serializable;
import java.util.*;
import java.util.regex.PatternSyntaxException;

public class NodeSearchPage extends WebPage {
    public static final String OPERATION_NAME = "NodeSearch";
    public static final String KEY_QUERY = "query";
    public static final String KEY_TARGET = "target";
    public static final String KEY_TYPE = "type";
    private final List<SearchAttribute> targets = new ArrayList<SearchAttribute>();
    private SearchAttribute target = null;
    private String query = null;
    private SearchType type = SearchType.PARTIAL;

    public static final String KEY_QUERY2 = "query2";
    public static final String KEY_TARGET2 = "target2";
    public static final String KEY_TYPE2 = "type2";
    private final List<SearchAttribute> targets2 = new ArrayList<SearchAttribute>();
    private SearchAttribute target2 = null;
    private String query2 = null;
    private SearchType type2 = SearchType.PARTIAL;

    public NodeSearchPage() {
        this(new PageParameters());
    }

    public NodeSearchPage(PageParameters param) {
        this(param, new ArrayList<FoundEntry>());
    }

    public NodeSearchPage(final PageParameters param, List<FoundEntry> entries) {
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);

            add(UrlUtil.getLink("refresh", "search"));

            Link<Void> newNodeButton = new Link<Void>("newNode") {
                private static final long serialVersionUID = 1L;

                public void onClick() {
                    try {
                        LocationDto top = LocationUtil.getRootLocation();
                        WebPage page = new NodeEditPage(NodeSearchPage.this, null, top);
                        setResponsePage(page);
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            add(newNodeButton);

            populateAttr();
            populateAttr2();

            this.query = Util.decodeUTF8(param.getString(KEY_QUERY));
            if (param.getString(KEY_TARGET) != null) {
                String index = param.getString(KEY_TARGET);
                int idx = Integer.parseInt(index);
                this.target = targets.get(idx);
            } else {
                this.target = targets.get(0);
            }
            if (param.getString(KEY_TYPE) != null) {
                this.type = SearchType.valueOf(param.getString(KEY_TYPE));
            } else {
                this.type = SearchType.PARTIAL;
            }
            Logger log = LoggerFactory.getLogger(NodeSearchPage.class);
            log.debug("query=" + query + ", target=" + target.name
                    + ", type=" + type);

            this.query2 = Util.decodeUTF8(param.getString(KEY_QUERY2));
            if (param.getString(KEY_TARGET2) != null) {
                String index = param.getString(KEY_TARGET2);
                int idx = Integer.parseInt(index);
                this.target2 = targets2.get(idx);
            } else {
                this.target2 = targets2.get(0);
            }
            if (param.getString(KEY_TYPE2) != null) {
                this.type2 = SearchType.valueOf(param.getString(KEY_TYPE2));
            } else {
                this.type2 = SearchType.PARTIAL;
            }
            log.debug("query2=" + query2 + ", target2=" + target2.name
                    + ", type2=" + type2);

            add(UrlUtil.getTopLink("top"));

            DropDownChoice<SearchAttribute> choiceTarget =
                    new DropDownChoice<SearchAttribute>("attr",
                            new PropertyModel<SearchAttribute>(this, "target"),
                            this.targets,
                            new ChoiceRenderer<SearchAttribute>("name"));
            choiceTarget.setNullValid(true);

            TextField<String> queryField = new TextField<String>("query",
                    new PropertyModel<String>(this, "query"));

            DropDownChoice<SearchType> choiceType =
                    new DropDownChoice<SearchType>("type",
                            new PropertyModel<SearchType>(this, "type"),
                            Arrays.asList(SearchType.values()),
                            new ChoiceRenderer<SearchType>("caption"));
            choiceType.setNullValid(true);

            Form<Void> form = new Form<Void>("searchForm") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        List<FoundEntry> founds = findNodes();
                        PageParameters param = new PageParameters();
                        param.add(KEY_QUERY, Util.encodeUTF8(query));
                        param.add(KEY_TARGET, String.valueOf(target.sortOrder - 1));
                        param.add(KEY_TYPE, type.name());
                        param.add(KEY_QUERY2, Util.encodeUTF8(query2));
                        param.add(KEY_TARGET2, String.valueOf(target2.sortOrder - 1));
                        param.add(KEY_TYPE2, type2.name());
                        NodeSearchPage page = new NodeSearchPage(param, founds);
                        setResponsePage(page);
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            form.add(choiceTarget);
            form.add(queryField);
            form.add(choiceType);
            add(form);

            DropDownChoice<SearchAttribute> choiceTarget2 =
                    new DropDownChoice<SearchAttribute>("attr2",
                            new PropertyModel<SearchAttribute>(this, "target2"),
                            this.targets2,
                            new ChoiceRenderer<SearchAttribute>("name"));
            choiceTarget2.setNullValid(true);

            TextField<String> queryField2 = new TextField<String>("query2",
                    new PropertyModel<String>(this, "query2"));

            DropDownChoice<SearchType> choiceType2 =
                    new DropDownChoice<SearchType>("type2",
                            new PropertyModel<SearchType>(this, "type2"),
                            Arrays.asList(SearchType.values()),
                            new ChoiceRenderer<SearchType>("caption"));
            choiceType2.setNullValid(true);

            form.add(choiceTarget2);
            form.add(queryField2);
            form.add(choiceType2);

            ListView<FoundEntry> list = new ListView<FoundEntry>("nodes", entries) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<FoundEntry> item) {
                    final FoundEntry entry = item.getModelObject();
                    final NodeDto node = entry.node;
                    Link<Void> nodeLink = NodePageUtil.createNodeLink("nodeLink", node);
                    item.add(nodeLink);
                    LocationDto pop = LocationUtil.getLocation(node, LocationType.FLOOR);
                    PageParameters param;
                    String popName;
                    if (pop != null) {
                        param = LocationUtil.getParameters(pop);
                        popName = LocationRenderer.getPopName(pop);
                    } else {
                        param = new PageParameters();
                        popName = "N/A";
                    }
                    /*Link<Void> popLink = new BookmarkablePageLink<Void>("pop", LocationViewPage.class, param);
                    Label popLabel = new Label("name", Model.of(popName));
                    popLink.add(popLabel);
                    popLink.setEnabled(pop != null);
                    item.add(popLink);*/
                    Label vendorName = new Label("vendorName", Model.of(NodeRenderer.getVendorName(node)));
                    item.add(vendorName);
                    Label nodeType = new Label("nodeType", Model.of(NodeRenderer.getNodeType(node)));
                    item.add(nodeType);
                    String ipAddress;
                    if (target.name.equals(MPLSNMS_ATTR.IP_ADDRESS)) {
                        ipAddress = entry.line;
                    } else {
                        ipAddress = NodeRenderer.getManagementIpAddress(node);
                    }
                    Label ipAddressLabel = new Label("ipAddress", Model.of(ipAddress));
                    item.add(ipAddressLabel);
                    Label purpose = new Label("purpose", NodeRenderer.getPurpose(node));
                    item.add(purpose);
                    Label osType = new Label("osType", Model.of(NodeRenderer.getOsType(node)));
                    item.add(osType);
                    Label osVersion = new Label("osVersion", Model.of(NodeRenderer.getOsVersion(node)));
                    item.add(osVersion);
                }
            };
            add(list);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private List<FoundEntry> findNodes() throws InventoryException {
        List<FoundEntry> result = new ArrayList<FoundEntry>();
        try {
            NodeFilter filter = getNodeFilter();
            if (filter == null) {
                return this.findNodes2();
            }
            List<NodeDto> nodes = new ArrayList<NodeDto>();
            if (this.query2 != null && !this.query2.equals("")) {
                List<FoundEntry> foundEntrys = this.findNodes2();
                for (FoundEntry foundEntry : foundEntrys) {
                    nodes.add(foundEntry.node);
                }
            } else {
                MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
                nodes.addAll(conn.getNodes());
            }

            for (NodeDto node : nodes) {
                if (LocationUtil.isTrash(LocationUtil.getLocation(node))) {
                    continue;
                }
                List<FoundEntry> matchedEntry = findNode(filter, node);
                if (matchedEntry != null) {
                    result.addAll(matchedEntry);
                }
            }
            Collections.sort(result, new FoundEntryComparator());
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
        return result;
    }

    private List<FoundEntry> findNode(NodeFilter filter, NodeDto node) throws InventoryException {
        List<FoundEntry> entries = new ArrayList<FoundEntry>();
        if (filter.match(node)) {
            for (String key : filter.getResult()) {
                List<NaefDto> targets = filter.getTargets().get(key);
                for (NaefDto target : targets) {
                    FoundEntry entry = new FoundEntry(node);
                    entry.line = key;
                    entry.target = target;
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    private List<FoundEntry> findNodes2() throws InventoryException {
        List<FoundEntry> result = new ArrayList<FoundEntry>();
        try {
            if (Util.isNull(target2, type2, query2)) {
                return result;
            }
            String type = target2.name;
            List<LocationDto> locations;
            if (type.equals("Area")) {
                locations = LocationUtil.getLocationsByType(LocationType.AREA);
            } else if (type.equals("Country")) {
                locations = LocationUtil.getLocationsByType(LocationType.COUNTRY);
            } else if (type.equals("City")) {
                locations = LocationUtil.getLocationsByType(LocationType.CITY);
            } else if (type.equals("Building")) {
                locations = LocationUtil.getLocationsByType(LocationType.BUILDING);
            } else if (type.equals("Floor")) {
                locations = LocationUtil.getLocationsByType(LocationType.FLOOR);
            } else if (type.equals("Rack")) {
                locations = LocationUtil.getLocationsByType(LocationType.RACK);
            } else {
                throw new IllegalStateException();
            }

            List<LocationDto> candidates = new ArrayList<LocationDto>();
            switch (type2) {
                case PARTIAL:
                    for (LocationDto location : locations) {
                        if (LocationUtil.getCaption(location).contains(query2)) {
                            candidates.add(location);
                        }
                    }
                    break;
                case REGEX:
                    for (LocationDto location : locations) {
                        if (LocationUtil.getCaption(location).matches(query2)) {
                            candidates.add(location);
                        }
                    }
                    break;
                case STRICT:
                    for (LocationDto location : locations) {
                        if (LocationUtil.getCaption(location).equals(query2)) {
                            candidates.add(location);
                        }
                    }
                    break;
            }
            List<MvoId> racks = new ArrayList<MvoId>();
            Set<MvoId> known = new HashSet<MvoId>();
            for (LocationDto candidate : candidates) {
                populateRack(candidate, racks, known);
            }
            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            List<NodeDto> nodes = conn.getActiveNodes();
            for (NodeDto node : nodes) {
                LocationDto accomodationRack = LocationUtil.getLocation(node);
                if (racks.contains(DtoUtil.getMvoId(accomodationRack))) {
                    FoundEntry entry = new FoundEntry(node);
                    result.add(entry);
                }
            }
            Collections.sort(result, new FoundEntryComparator());
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
        return result;
    }

    public String getQuery() {
        return this.query;
    }

    public String getQuery2() {
        return this.query2;
    }

    private void populateAttr() {
        this.targets.clear();
        this.targets.add(new SearchAttribute("Node Name", 2));
        this.targets.add(new SearchAttribute("Vendor", 3));
        this.targets.add(new SearchAttribute("Node Type", 4));
        this.targets.add(new SearchAttribute("IP Address", 5));
        this.targets.add(new SearchAttribute("OS Type", 6));
        this.targets.add(new SearchAttribute("OS Version", 7));
        this.targets.add(new SearchAttribute("Purpose", 8));
    }

    private void populateAttr2() {
        this.targets2.clear();
        this.targets2.add(new SearchAttribute("Area", 1));
        this.targets2.add(new SearchAttribute("Country", 2));
        this.targets2.add(new SearchAttribute("City", 3));
        this.targets2.add(new SearchAttribute("Building", 4));
        this.targets2.add(new SearchAttribute("Floor", 5));
        this.targets2.add(new SearchAttribute("Rack", 6));
    }

    private NodeFilter getNodeFilter() {
        if (Util.isNull(target, query, type)) {
            return null;
        }
        String attr = this.target.name;
        if (attr.equals("Node Name")) {
            NodeFilter filter = new NodeFilter(query, type) {
                private static final long serialVersionUID = 1L;

                public boolean match(NodeDto node) throws InventoryException {
                    getResult().clear();
                    getTargets().clear();
                    String nodeName = node.getName();
                    boolean result = match(nodeName);
                    if (result) {
                        addResult(nodeName, null);
                    }
                    return result;
                }
            };
            return filter;
        } else if (attr.equals("Vendor")) {
            NodeFilter filter = new NodeFilter(query, type) {
                private static final long serialVersionUID = 1L;

                public boolean match(NodeDto node) throws InventoryException {
                    getResult().clear();
                    getTargets().clear();
                    String name = NodeRenderer.getVendorName(node);
                    boolean result = match(name);
                    if (result) {
                        addResult(name, null);
                    }
                    return result;
                }
            };
            return filter;
        } else if (attr.equals("Node Type")) {
            NodeFilter filter = new NodeFilter(query, type) {
                private static final long serialVersionUID = 1L;

                public boolean match(NodeDto node) throws InventoryException {
                    getResult().clear();
                    getTargets().clear();
                    String name = NodeRenderer.getNodeType(node);
                    boolean result = match(name);
                    if (result) {
                        addResult(name, null);
                    }
                    return result;
                }
            };
            return filter;
        } else if (attr.equals("Purpose")) {
            NodeFilter filter = new NodeFilter(query, type) {
                private static final long serialVersionUID = 1L;

                public boolean match(NodeDto node) throws InventoryException {
                    getResult().clear();
                    getTargets().clear();
                    String name = NodeRenderer.getPurpose(node);
                    boolean result = match(name);
                    if (result) {
                        addResult(name, null);
                    }
                    return result;
                }
            };
            return filter;
        } else if (attr.equals("IP Address")) {
            NodeFilter filter = new NodeFilter(query, type) {
                private static final long serialVersionUID = 1L;

                public boolean match(NodeDto node) throws InventoryException {
                    return regularMatch(node);
                }

                private boolean regularMatch(NodeDto node) throws InventoryException {
                    getResult().clear();
                    getTargets().clear();
                    String name = NodeRenderer.getManagementIpAddress(node);
                    boolean match = match(name);
                    boolean result = false;
                    if (match) {
                        addResult(name, node);
                        result = true;
                    }
                    for (PortDto port : node.getPorts()) {
                        String addr = PortRenderer.getIpAddress(port);
                        boolean found = match(addr);
                        if (found) {
                            addResult(addr, port);
                            result = true;
                        }
                    }
                    return result;
                }
            };
            return filter;
        } else if (attr.equals("OS Type")) {
            NodeFilter filter = new NodeFilter(query, type) {
                private static final long serialVersionUID = 1L;

                public boolean match(NodeDto node) throws InventoryException {
                    getResult().clear();
                    getTargets().clear();
                    String name = NodeRenderer.getOsType(node);
                    boolean result = match(name);
                    if (result) {
                        addResult(name, null);
                    }
                    return result;
                }
            };
            return filter;
        } else if (attr.equals("OS Version")) {
            NodeFilter filter = new NodeFilter(query, type) {
                private static final long serialVersionUID = 1L;

                public boolean match(NodeDto node) throws InventoryException {
                    getResult().clear();
                    getTargets().clear();
                    String name = NodeRenderer.getOsVersion(node);
                    boolean result = match(name);
                    if (result) {
                        addResult(name, null);
                    }
                    return result;
                }
            };
            return filter;
        } else {
            throw new IllegalArgumentException("unknown attr: [" + attr + "]");
        }
    }

    private static class SearchAttribute implements Serializable {
        private static final long serialVersionUID = 1L;
        public String name;
        public int sortOrder = -1;

        public SearchAttribute(String name, int order) {
            this.name = name;
            this.sortOrder = order;
        }
    }

    public abstract static class NodeFilter implements Serializable {
        private static final long serialVersionUID = 1L;
        private final SearchType type;
        private final String query;
        private final List<String> lines = new ArrayList<String>();
        private final Map<String, List<NaefDto>> targets = new HashMap<String, List<NaefDto>>();

        public NodeFilter(String s, SearchType type) {
            this.type = type;
            this.query = s;
        }

        public abstract boolean match(NodeDto node) throws InventoryException;

        public List<String> getResult() {
            return this.lines;
        }

        public Map<String, List<NaefDto>> getTargets() {
            return this.targets;
        }

        protected boolean match(String s) throws InventoryException {
            if (s == null || s.length() == 0) {
                return false;
            }
            boolean matched = false;
            switch (type) {
                case PARTIAL:
                    matched = partialMatch(s);
                    break;
                case REGEX:
                    matched = regexMatch(s);
                    break;
                case STRICT:
                    matched = strictMatch(s);
                    break;
                default:
                    throw new InventoryException("unknown search-type: " + type);
            }
            return matched;
        }

        protected void addResult(String line, NaefDto target) {
            if (!this.lines.contains(line)) {
                this.lines.add(line);
            }
            List<NaefDto> list = this.targets.get(line);
            if (list == null) {
                list = new ArrayList<NaefDto>();
                this.targets.put(line, list);
            }
            boolean found = false;
            for (NaefDto elem : list) {
                if (elem != null && DtoUtil.mvoEquals(elem, target)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                list.add(target);
            }
        }

        private boolean strictMatch(String s1) {
            s1 = Util.stringToNull(s1);
            if (s1 == null && query == null) {
                return true;
            } else if (s1 == null || query == null) {
                return false;
            }
            return s1.toLowerCase().equals(query.toLowerCase());
        }

        private boolean partialMatch(String s1) {
            s1 = Util.stringToNull(s1);
            if (s1 == null && query == null) {
                return true;
            } else if (s1 == null || query == null) {
                return false;
            }
            return s1.toLowerCase().contains(query.toLowerCase());
        }

        private boolean regexMatch(String s1) {
            s1 = Util.stringToNull(s1);
            if (s1 == null && query == null) {
                return true;
            } else if (s1 == null || query == null) {
                return false;
            }
            try {
                return s1.matches(query);
            } catch (PatternSyntaxException e) {
                throw new IllegalStateException("Illegal Regular Expression: " + query);
            }
        }
    }

    private static enum SearchType {
        PARTIAL("Partial Match"),
        STRICT("Strict Match"),
        REGEX("Regular Expression Match"),
        ;

        private final String caption;

        private SearchType(String caption) {
            this.caption = caption;
        }

        @SuppressWarnings("unused")
        public String getCaption() {
            return this.caption;
        }
    }

    private static class FoundEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        public NodeDto node;
        public String line;
        @SuppressWarnings("unused")
        public NaefDto target;

        public FoundEntry(NodeDto node) {
            if (node == null) {
                throw new IllegalArgumentException("node is null.");
            }
            this.node = node;
        }
    }

    private static class FoundEntryComparator implements Comparator<FoundEntry> {

        @Override
        public int compare(FoundEntry o1, FoundEntry o2) {
            return o1.node.getName().compareTo(o2.node.getName());
        }
    }

    private void populateRack(LocationDto location, List<MvoId> result, Set<MvoId> known) {
        if (LocationUtil.getLocationType(location) == LocationType.RACK) {
            result.add(DtoUtil.getMvoId(location));
            known.add(DtoUtil.getMvoId(location));
        }
        for (LocationDto child : location.getChildren()) {
            if (known.contains(DtoUtil.getMvoId(child))) {
                continue;
            }
            populateRack(child, result, known);
        }
    }
}