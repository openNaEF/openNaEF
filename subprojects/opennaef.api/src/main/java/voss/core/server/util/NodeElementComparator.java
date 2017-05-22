package voss.core.server.util;

import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import voss.core.server.database.ATTR;

import java.util.*;

public class NodeElementComparator implements Comparator<NodeElementDto> {
    private final Map<NodeElementDto, List<Object>> pathCache = new HashMap<NodeElementDto, List<Object>>();
    private final TypeBasedPortComparator portComparator = new TypeBasedPortComparator();

    @Override
    public int compare(NodeElementDto o1, NodeElementDto o2) {
        if (o1 instanceof PortDto && o2 instanceof PortDto) {
            return portComparator.compare((PortDto) o1, (PortDto) o2);
        }

        List<Object> path1 = getPath(o1);
        List<Object> path2 = getPath(o2);
        int depth = Math.min(path1.size(), path2.size());
        for (int i = 0; i < depth; i++) {
            Object s1 = path1.get(i);
            Object s2 = path2.get(i);
            if (s1 instanceof Long && s2 instanceof Long) {
                long l1 = ((Long) s1).longValue();
                long l2 = ((Long) s2).longValue();
                if (l1 > l2) {
                    return 1;
                } else if (l1 < l2) {
                    return -1;
                }
            } else {
                int diff = s1.toString().compareTo(s2.toString());
                if (diff != 0) {
                    return diff;
                }
            }
        }
        return path1.size() - path2.size();
    }

    private List<Object> getPath(NodeElementDto dto) {
        List<Object> path = pathCache.get(dto);
        if (path == null) {
            path = new ArrayList<Object>();
            path.add(getValue(dto));
            NodeElementDto owner = dto.getOwner();
            while (owner != null) {
                path.add(getValue(owner));
                owner = owner.getOwner();
            }
            Collections.reverse(path);
            pathCache.put(dto, path);
        }
        return path;
    }

    private Object getValue(NodeElementDto dto) {
        String name = DtoUtil.getStringOrNull(dto, ATTR.SUFFIX);
        if (name == null) {
            name = dto.getName();
        }
        try {
            long l = Long.parseLong(name);
            return Long.valueOf(l);
        } catch (NumberFormatException e) {
            return name;
        }
    }
}