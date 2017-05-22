package naef.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InternodeTopology implements java.io.Serializable {

    private final Set<NodeDto> nodes_;
    private final Set<InternodeLinkDto> links_;
    private final Map<NodeDto, Set<InternodeLinkDto>> nodeLinks_;

    public InternodeTopology(Collection<NodeDto> nodes, Collection<InternodeLinkDto> links) {
        nodes_ = new HashSet<NodeDto>(nodes);
        links_ = new HashSet<InternodeLinkDto>(links);
        nodeLinks_ = new HashMap<NodeDto, Set<InternodeLinkDto>>();
        for (InternodeLinkDto link : links_) {
            putNodeLink(link.getNode1(), link);
            putNodeLink(link.getNode2(), link);
        }
    }

    private void putNodeLink(NodeDto node, InternodeLinkDto link) {
        Set<InternodeLinkDto> links = nodeLinks_.get(node);
        if (links == null) {
            links = new HashSet<InternodeLinkDto>();
            nodeLinks_.put(node, links);
        }
        links.add(link);
    }

    public Set<InternodeLinkDto> getLinks() {
        return Collections.<InternodeLinkDto>unmodifiableSet(links_);
    }

    public Set<NodeDto> getNodes() {
        return Collections.<NodeDto>unmodifiableSet(nodes_);
    }

    public InternodeLinkDto getLink(NodeDto node1, NodeDto node2) {
        for (InternodeLinkDto link : getLinks()) {
            if ((DtoUtils.isSameEntity(link.getNode1(), node1) && DtoUtils.isSameEntity(link.getNode2(), node2))
                || (DtoUtils.isSameEntity(link.getNode1(), node2) && DtoUtils.isSameEntity(link.getNode2(), node1)))
            {
                return link;
            }
        }
        return null;
    }

    public Set<NodeDto> getNeighbors(NodeDto node) {
        Set<NodeDto> result = new HashSet<NodeDto>();
        Set<InternodeLinkDto> links = nodeLinks_.get(node);
        if (links != null) {
            for (InternodeLinkDto link : links) {
                result.add(DtoUtils.isSameEntity(link.getNode1(), node)
                    ? link.getNode2()
                    : link.getNode1());
            }
        }
        return result;
    }
}
