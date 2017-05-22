package voss.nms.inventory.element;

import naef.dto.*;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vrf.VrfDto;
import naef.dto.vrf.VrfIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.MVO.MvoId;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.*;

import java.io.Serializable;
import java.util.*;

public class AffectedUserList implements Serializable {
    private static final long serialVersionUID = 1L;
    private final NaefDto dto;
    private final List<AffectedUser> users = new ArrayList<AffectedUser>();
    private final NodeElementFilter filter;

    public AffectedUserList(NaefDto dto, NodeElementFilter filter) {
        this.dto = dto;
        this.filter = filter;
    }

    public synchronized void analyze() throws InventoryException, ExternalServiceException {
        analyze(true);
    }

    public synchronized void analyze(boolean populate) throws InventoryException, ExternalServiceException {
        if (this.dto == null) {
            return;
        }
        log().trace("analyze(): target=" + dto.getAbsoluteName());
        long time = System.currentTimeMillis();
        PerfLog.info(time, time, "analyze(): begin. target=" + this.dto.getAbsoluteName());
        this.users.clear();
        this.dto.renew();
        List<AffectedUser> elements;
        if (dto instanceof NodeElementDto) {
            elements = analyzeNodeElement((NodeElementDto) dto, populate);
        } else if (dto instanceof IdPoolDto<?, ?, ?>) {
            if (filter.isEnablePool()) {
                elements = analyzeIdPool((IdPoolDto<?, ?, ?>) dto);
            } else {
                elements = new ArrayList<AffectedUser>();
            }
        } else if (dto instanceof NetworkDto) {
            elements = analyzeNetwork((NetworkDto) dto);
        } else if (dto instanceof RsvpLspDto) {
            elements = analyzeRsvpLsp((RsvpLspDto) dto);
        } else if (dto instanceof LinkDto) {
            if (filter.isEnableLink()) {
                elements = analyzeLink((LinkDto) dto);
            } else {
                elements = new ArrayList<AffectedUser>();
            }
        } else {
            throw new IllegalStateException("unknown dto:"
                    + dto.getClass().getName()
                    + ", " + dto.getAbsoluteName());
        }
        long current = System.currentTimeMillis();
        PerfLog.info(time, current, "analyze(): elements.size=" + elements.size());
        filter(elements);
    }

    private void filter(List<AffectedUser> elements) {
        long start = System.currentTimeMillis();
        long prev = start;
        long time = start;
        PerfLog.debug(start, start, "filter start");
        List<AffectedUser> temp = new ArrayList<AffectedUser>();
        for (AffectedUser element : elements) {
            if (filter.match(element.getTarget())) {
                log().trace("filtered: target:" + element.getTarget().getAbsoluteName());
                continue;
            }
            if (element.getRelatedResource() != null) {
                if (filter.match(element.getRelatedResource())) {
                    log().trace("filtered: related:" + element.getRelatedResource().getAbsoluteName());
                    continue;
                }
            }
            temp.add(element);
            log().trace("added1:" + element);
            time = System.currentTimeMillis();
            PerfLog.debug(prev, time, element.toString());
            prev = time;
        }
        Set<MvoId> readMvos = new HashSet<MvoId>();
        for (AffectedUser element : temp) {
            if (element.getRelatedResource() == null) {
                if (readMvos.contains(DtoUtil.getMvoId(element.getTarget()))) {
                    log().trace("already read: target:" + element.toString());
                    continue;
                }
                readMvos.add(DtoUtil.getMvoId(element.getTarget()));
            }
            this.users.add(element);
            log().trace("added2:" + element);
            time = System.currentTimeMillis();
            PerfLog.debug(prev, time, element.toString());
            prev = time;
        }
        PerfLog.debug(start, time, "filter end");
    }

    private List<AffectedUser> analyzeNodeElement(NodeElementDto ne, boolean populate) throws InventoryException,
            ExternalServiceException {
        List<AffectedUser> result = new ArrayList<AffectedUser>();
        try {
            List<NodeElementDto> subElements = new ArrayList<NodeElementDto>();
            if (ne instanceof NodeElementDto) {
                if (populate) {
                    NodeUtil.getSubNodeElements(subElements, (NodeElementDto) ne);
                }
            } else {
                throw new IllegalStateException("unknown dto:"
                        + dto.getClass().getName()
                        + ", " + dto.getAbsoluteName());
            }
            AffectedUser user = AffectedUser.create(ne);
            result.add(user);
            log().trace("." + user.toString());
            for (NodeElementDto subElement : subElements) {
                AffectedUser sub2 = AffectedUser.create(subElement);
                result.add(sub2);
                log().trace("+" + sub2.toString());
            }
            result = populateSoftPortToNetwork(result);
            result = removeUserDuplication(result);
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    private List<AffectedUser> removeUserDuplication(List<AffectedUser> elements) {
        List<AffectedUser> noDuplicates = new ArrayList<AffectedUser>();
        Set<String> seen = new HashSet<String>();
        for (AffectedUser element : elements) {
            NaefDto target = element.getTarget();
            NaefDto owner = element.getRelatedResource();
            String key = DtoUtil.getMvoId(target).toString() + (owner == null ? "" : DtoUtil.getMvoId(owner).toString());
            if (seen.contains(key)) {
                continue;
            }
            noDuplicates.add(element);
            seen.add(key);
        }
        return noDuplicates;
    }

    private List<AffectedUser> populateSoftPortToNetwork(List<AffectedUser> elements)
            throws InventoryException {
        List<AffectedUser> result = new ArrayList<AffectedUser>(elements);
        for (AffectedUser element : elements) {
            NaefDto target = element.getTarget();
            log().trace("target=" + (target != null ? target.getAbsoluteName() : "-"));
            if (target != null && target instanceof PortDto) {
                result.addAll(populatePortToRelatedNetwork((PortDto) target));
            }
        }
        return result;
    }

    private List<AffectedUser> populatePortToRelatedNetwork(PortDto port) {
        List<AffectedUser> result = new ArrayList<AffectedUser>();
        if (port == null) {
            log().trace("-- null");
            return result;
        }
        PortDto basePort = port;
        if (VlanUtil.isBridgePort(port)) {
            log().trace("port [" + port.getAbsoluteName() + "] is bridge port. " +
                    "use target as bridged port [" + basePort.getAbsoluteName() + "]");
            basePort = (PortDto) port.getOwner();
        }
        NetworkDto domainDto = DtoUtil.getNetwork(port);
        if (domainDto != null) {
            result.add(AffectedUser.create(basePort, domainDto));
            log().trace("++1" + basePort.getAbsoluteName() + " >> " + domainDto.getAbsoluteName());
        }
        Collection<NetworkDto> networks = port.getNetworks();
        if (networks == null) {
            log().trace("-- no networks");
            return result;
        }
        log().trace("-- networks:" + networks.size());
        for (NetworkDto network : networks) {
            log().debug("** enable router-vlan? = " + filter.isEnableRouterVlanIf()
                    + ", router vlan? = " + isVlanOnRouterVlanPort(port, network));
            if (filter.isEnableRouterVlanIf() && isVlanOnRouterVlanPort(port, network)) {
                continue;
            }
            result.add(AffectedUser.create(basePort, network));
            if (log().isTraceEnabled()) {
                log().trace("++2" + basePort.getAbsoluteName() + " >> " + network.getAbsoluteName());
            }
        }
        List<RsvpLspDto> lsps = RsvpLspUtil.getRsvpLspsOn(port);
        for (RsvpLspDto lsp : lsps) {
            if (filter.isEnableRsvpLsp()) {
                log().debug("basePort=" + basePort.getAbsoluteName() + ", lsp=" + lsp.getName());
                result.add(AffectedUser.create(basePort, lsp));
                List<PseudowireDto> pws = RsvpLspUtil.getPseudowiresOn(lsp);
                for (PseudowireDto pw : pws) {
                    result.add(AffectedUser.create(basePort, pw));
                    log().debug("basePort=" + basePort.getAbsoluteName() + ", pw=" + pw.getAbsoluteName());
                }
            }
        }
        return result;
    }

    private boolean isVlanOnRouterVlanPort(NaefDto target, NetworkDto network) {
        if (!(target instanceof PortDto)) {
            return false;
        }
        if (!(network instanceof VlanDto)) {
            return false;
        }
        PortDto port = (PortDto) target;
        return VlanUtil.isRouterVlanEnabledPort(port);
    }

    private List<AffectedUser> analyzeIdPool(IdPoolDto<?, ?, ?> pool) {
        throw new IllegalStateException("It is out of scope");
    }

    private List<AffectedUser> analyzeNetwork(NetworkDto network) {
        List<AffectedUser> elements = new ArrayList<AffectedUser>();
        elements.add(AffectedUser.create(network));
        if (network instanceof VlanDto) {
            if (filter.isEnableVlan()) {
                VlanDto vlan = (VlanDto) network;
                for (VlanIfDto vlanIf : vlan.getMemberVlanifs()) {
                    elements.add(AffectedUser.create(network, vlanIf));
                    for (PortDto port : vlanIf.getTaggedVlans()) {
                        elements.add(AffectedUser.create(network, port));
                    }
                    for (PortDto port : vlanIf.getUntaggedVlans()) {
                        elements.add(AffectedUser.create(network, port));
                    }
                }
            }
        } else if (network instanceof VplsDto) {
            if (filter.isEnableVpls()) {
                VplsDto vpls = (VplsDto) network;
                for (VplsIfDto vplsIf : vpls.getMemberVplsifs()) {
                    elements.add(AffectedUser.create(network, vplsIf));
                    for (PortDto port : vplsIf.getAttachedPorts()) {
                        elements.add(AffectedUser.create(network, port));
                    }
                }
            }
        } else if (network instanceof VrfDto) {
            if (filter.isEnableVrf()) {
                VrfDto vrf = (VrfDto) network;
                for (VrfIfDto vrfIf : vrf.getMemberVrfifs()) {
                    elements.add(AffectedUser.create(network, vrfIf));
                    for (PortDto port : vrfIf.getAttachedPorts()) {
                        elements.add(AffectedUser.create(network, port));
                    }
                }
            }
        } else if (network instanceof PseudowireDto) {
            if (filter.isEnablePseudoWire()) {
                PseudowireDto pw = (PseudowireDto) network;
                if (pw.getAc1() != null) {
                    elements.add(AffectedUser.create(network, pw.getAc1()));
                }
                if (pw.getAc2() != null) {
                    elements.add(AffectedUser.create(network, pw.getAc2()));
                }
            }
        } else {
            throw new IllegalStateException("unknown dto:"
                    + dto.getClass().getName()
                    + ", " + dto.getAbsoluteName());
        }
        return elements;
    }

    private List<AffectedUser> analyzeRsvpLsp(RsvpLspDto lsp) {
        List<AffectedUser> users = new ArrayList<AffectedUser>();
        List<PseudowireDto> pws = RsvpLspUtil.getPseudowiresOn(lsp);
        for (PseudowireDto pw : pws) {
            users.add(AffectedUser.create(lsp, pw));
            if (pw.getAc1() != null) {
                users.add(AffectedUser.create(lsp, pw.getAc1()));
            }
            if (pw.getAc2() != null) {
                users.add(AffectedUser.create(lsp, pw.getAc2()));
            }
        }
        return users;
    }

    private List<AffectedUser> analyzeLink(LinkDto link) {
        throw new IllegalStateException("対象外");
    }

    public synchronized void sort() {
        long prev = System.currentTimeMillis();
        PerfLog.info(prev, prev, "sort start.");
        Collections.sort(this.users, new AffectedUserComparator());
        PerfLog.info(prev, System.currentTimeMillis(), "sort end.");
    }

    public synchronized int size() {
        return this.users.size();
    }

    public NaefDto getTarget() {
        return this.dto;
    }

    public synchronized AffectedUser getUser(int index) {
        if (users.size() <= index) {
            throw new IllegalStateException("illegal index:" + index + ", size=" + users.size());
        }
        return users.get(index);
    }

    public synchronized void dumpUsers() {
        Logger log = LoggerFactory.getLogger(AffectedUserList.class);
        log.debug("begin dump ----");
        for (AffectedUser user : this.users) {
            log.debug(user.toString());
        }
        log.debug("end dump ----");
    }

    public synchronized List<NaefDto> getElements() {
        List<NaefDto> result = new ArrayList<NaefDto>();
        for (AffectedUser user : this.users) {
            result.add(user.getTarget());
        }
        return result;
    }

    public synchronized List<AffectedUser> getUsers() {
        return this.users;
    }

    public synchronized List<AffectedUser> getUsers(NaefDto top) {
        List<AffectedUser> result = new ArrayList<AffectedUser>();
        if (top instanceof NetworkDto) {
            for (AffectedUser user : this.users) {
                if (user.target instanceof NetworkDto
                        && DtoUtil.isSameMvoEntity(top, user.getTarget())) {
                    result.add(user);
                } else if (user.owner instanceof NetworkDto
                        && DtoUtil.isSameMvoEntity(top, user.getTarget())) {
                    result.add(user);
                }
            }
        } else if (top instanceof NodeElementDto) {
            for (AffectedUser user : this.users) {
                if (user.target instanceof NodeElementDto
                        && NodeUtil.isSameOrSubElement((NodeElementDto) top, user.getTarget())) {
                    result.add(user);
                } else if (user.owner instanceof NodeElementDto
                        && NodeUtil.isSameOrSubElement((NodeElementDto) top, user.getTarget())) {
                    result.add(user);
                }
            }
        } else {
            log().warn("unknown top type:" + top.getAbsoluteName());
        }
        return result;
    }

    public synchronized AffectedUserList copyList(NaefDto newTarget, NodeElementFilter newFilter) {
        AffectedUserList newList = new AffectedUserList(newTarget, newFilter);
        newList.users.addAll(this.users);
        return newList;
    }

    private Logger log() {
        return LoggerFactory.getLogger(AffectedUserList.class);
    }

    public static class AffectedUser implements Serializable {
        private static final long serialVersionUID = 1L;
        private final NaefDto target;
        private NaefDto owner;

        public AffectedUser(NaefDto target) {
            if (target == null) {
                throw new IllegalArgumentException("target is null.");
            }
            this.target = target;
        }

        public NaefDto getTarget() {
            return target;
        }

        public NaefDto getRelatedResource() {
            return owner;
        }

        public void setOwner(NaefDto owner) {
            this.owner = owner;
        }

        public void renew() {
            if (this.target != null) {
                this.target.renew();
            }
            if (this.owner != null) {
                this.owner.renew();
            }
        }

        @Override
        public String toString() {
            return "AffectedUser:target=" + target.getAbsoluteName()
                    + (owner == null ? "" : " - " + owner.getAbsoluteName());
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        public static AffectedUser create(NaefDto dto) {
            AffectedUser user = new AffectedUser(dto);
            return user;
        }

        public static AffectedUser create(NaefDto target, NaefDto owner) {
            AffectedUser user = new AffectedUser(target);
            user.owner = owner;
            return user;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof AffectedUser)) {
                return false;
            } else if (o == this) {
                return true;
            }
            AffectedUser another = (AffectedUser) o;
            if (this.owner != null && another.owner == null) {
                return false;
            } else if (this.owner == null && another.owner != null) {
                return false;
            }
            boolean r = this.target.getAbsoluteName().equals(another.target.getAbsoluteName());
            if (this.owner == null && another.owner == null) {
                return r;
            } else {
                return r && this.owner.getAbsoluteName().equals(another.owner.getAbsoluteName());
            }
        }

        @SuppressWarnings("unchecked")
        public <T extends NaefDto> T getObjectOf(Class<T> class_) {
            if (this.target != null && class_.isAssignableFrom(this.target.getClass())) {
                return (T) this.target;
            } else if (this.owner != null && class_.isAssignableFrom(this.owner.getClass())) {
                return (T) this.owner;
            }
            return null;
        }
    }

    private static class AffectedUserComparator implements Comparator<AffectedUser> {
        private final DtoComparator comparator = new DtoComparator();

        @Override
        public int compare(AffectedUser o1, AffectedUser o2) {
            int result = comparator.compare(o1.target, o2.target);
            if (result == 0) {
                result = comparator.compare(o1.owner, o2.owner);
            }
            return result;
        }
    }
}