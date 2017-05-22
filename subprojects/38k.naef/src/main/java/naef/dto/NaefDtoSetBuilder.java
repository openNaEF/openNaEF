package naef.dto;

import naef.NaefTefService;
import naef.mvo.NaefMvoUtils;
import naef.mvo.Node;
import naef.mvo.P2pLink;
import naef.mvo.Port;
import tef.MVO;
import tef.skelton.dto.DtoOriginator;
import tef.skelton.dto.MvoDtoFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NaefDtoSetBuilder {

    private final MvoDtoFactory dtoFactory_;
    private final DtoOriginator originator_;
    private final Map<Node, NodeDto> nodes_ = new HashMap<Node, NodeDto>();

    public NaefDtoSetBuilder(DtoOriginator originator) {
        dtoFactory_ = NaefTefService.instance().getMvoDtoFactory();
        originator_ = originator;
    }

    public NodeDto getNode(Node node) {
        NodeDto result = nodes_.get(node);
        if (result == null) {
            result = dtoFactory_.<NodeDto>build(originator_, node);
            nodes_.put(node, result);
        }
        return result;
    }

    public Set<NodeDto> getNodes() {
        return new HashSet<NodeDto>(nodes_.values());
    }

    public InternodeTopology buildInternodeTopology(Class<? extends P2pLink> linkType) {
        Set<InternodeLinkDto> links = new HashSet<InternodeLinkDto>();
        for (Node mvoNode : Node.home.list()) {
            for (Port port : NaefMvoUtils.getCurrentSubElements(mvoNode, Port.class, true)) {
                Port peer = P2pLink.getPeer(linkType, port);
                if (peer != null) {
                    links.add(new InternodeLinkDto(getNode(mvoNode), getNode(peer.getNode())));
                }
            }
        }

        return new InternodeTopology(getNodes(), links);
    }

    public List<InterportLinkDto> buildInterportLinks(Node node1, Node node2, Class<? extends P2pLink> linkType) {
        List<InterportLinkDto> result = new ArrayList<InterportLinkDto>();
        for (Port port : NaefMvoUtils.getCurrentSubElements(node1, Port.class, true)) {
            Port peer = P2pLink.getPeer(linkType, port);
            if (peer != null && peer.getNode() == node2) {
                PortDto port1Dto = dtoFactory_.<PortDto>build(originator_, (MVO) port);
                PortDto port2Dto = dtoFactory_.<PortDto>build(originator_, (MVO) peer);
                result.add(new InterportLinkDto(port1Dto, port2Dto));
            }
        }
        return result;
    }

}
