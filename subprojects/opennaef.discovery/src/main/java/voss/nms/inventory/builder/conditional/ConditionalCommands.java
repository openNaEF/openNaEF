package voss.nms.inventory.builder.conditional;

import naef.dto.NaefDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.ui.NaefDtoFacade;
import naef.ui.NaefDtoFacade.SearchMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.*;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

public abstract class ConditionalCommands<T extends NaefDto> implements Commands {
    private static final long serialVersionUID = 1L;
    private final List<String> warnings = new ArrayList<String>();
    private final String editorName;
    private transient T target;
    private final String targetMvoId;
    private final String targetVersion;
    private BuildResult result = null;
    private boolean versionCheck = true;
    private boolean valueCheck = false;
    private final List<String> rawCommands = new ArrayList<String>();
    private final List<String> assertions = new ArrayList<String>();
    private Map<String, String> changed = new HashMap<String, String>();
    private Map<String, String> prechanged = new HashMap<String, String>();
    private transient Logger log = null;

    public ConditionalCommands(String editorName) {
        this.target = null;
        this.targetMvoId = null;
        this.targetVersion = null;
        this.editorName = editorName;
    }

    public ConditionalCommands(T dto, String editorName) {
        if (dto == null) {
            throw new IllegalArgumentException("dto is null.");
        }
        this.target = dto;
        this.targetMvoId = DtoUtil.getMvoId(dto).toString();
        this.targetVersion = DtoUtil.getMvoVersion(dto).toString();
        this.editorName = editorName;
    }

    protected T getDto(Class<T> cls) throws IOException, InventoryException, ExternalServiceException {
        if (this.target != null) {
            return this.target;
        } else if (this.targetMvoId == null) {
            return null;
        }
        InventoryConnector conn = InventoryConnector.getInstance();
        return conn.getMvoDto(targetMvoId, targetVersion, cls);
    }

    protected ShellCommands createShellCommands() {
        return new ShellCommands(this.editorName);
    }

    @Override
    public List<String> getCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add("# - built by " + getClass().getSimpleName());
        if (isVersionCheckRequired()) {
            commands.add("# - assertion");
            commands.addAll(getAssertions());
        }
        commands.add("# - command");
        commands.addAll(getRawCommands());
        return commands;
    }

    @Override
    public List<String> getRawCommands() {
        return this.rawCommands;
    }

    protected void setRawCommands(List<String> cmd) {
        this.rawCommands.clear();
        this.rawCommands.addAll(cmd);
    }

    @Override
    public void evaluate() {
        if (this.target != null) {
            this.target.renew();
        }
        reset();
        evaluateDiff();
        log().debug("evaluated.");
    }

    protected void evaluateDiff() {
        ShellCommands cmd = createShellCommands();
        evaluateDiffInner(cmd);
        setRawCommands(cmd.getCommands());
    }

    public void reset() {
        this.assertions.clear();
        this.rawCommands.clear();
        this.warnings.clear();
        this.changed.clear();
        this.prechanged.clear();
    }

    @Override
    public List<String> getAssertions() {
        return this.assertions;
    }

    protected void clearAssertions() {
        this.assertions.clear();
    }

    protected void setAssertions(List<String> cmd) {
        this.assertions.clear();
        this.assertions.addAll(cmd);
    }

    protected void addAssertion(NaefDto dto) {
        if (dto == null) {
            return;
        }
        if (isVersionCheckRequired()) {
            this.assertions.add(InventoryBuilder.getMvoVersionCheckCommand(dto));
        }
    }

    abstract protected void evaluateDiffInner(ShellCommands cmd);

    @Override
    public void setVersionCheck(boolean versionCheckRequired) {
        this.versionCheck = versionCheckRequired;
    }

    @Override
    public void setValueCheck(boolean valueCheckRequired) {
        this.valueCheck = valueCheckRequired;
    }

    public boolean isVersionCheckRequired() {
        return this.versionCheck;
    }

    public boolean isValueCheckRequired() {
        return this.valueCheck;
    }

    protected void setBuildResult(BuildResult result) {
        this.result = result;
    }

    public BuildResult getBuildResult() {
        return this.result;
    }

    @Override
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    @Override
    public List<String> getWarnings() {
        return this.warnings;
    }

    protected void recordChange(String attrName, String oldValue, String newValue) {
        this.changed.put(attrName, newValue);
        this.prechanged.put(attrName, oldValue);
    }

    public Map<String, String> getAttributes() {
        Map<String, String> result = new HashMap<String, String>();
        result.putAll(this.changed);
        return result;
    }

    public Map<String, String> getPrechangeAttributes() {
        Map<String, String> result = new HashMap<String, String>();
        result.putAll(this.prechanged);
        return result;
    }

    @Override
    public void setValueCheckContents(List<ChangeUnit> units) {
    }

    protected List<PortDto> getAddedPortsByIfName(Collection<String> toAdd, NodeDto node, NaefDtoFacade facade)
            throws RemoteException {
        List<PortDto> addedPorts = new ArrayList<PortDto>();
        for (String toAddIfName : toAdd) {
            Set<PortDto> ports = facade.selectNodeElements(node,
                    PortDto.class, SearchMethod.EXACT_MATCH, MPLSNMS_ATTR.IFNAME, toAddIfName);
            if (ports == null || ports.size() == 0) {
                log().warn("port[" + toAddIfName + "] not found. (maybe port-diff not applied.)");
                continue;
            }
            for (PortDto port : ports) {
                addedPorts.add(port);
            }
        }
        return addedPorts;
    }

    protected void getAttachmentPortDiff(Set<PortDto> attachmentPorts, Collection<String> ifNames, List<String> toAdd, List<PortDto> removedPorts) {
        log().debug("discovered-port=" + ifNames);
        for (PortDto port : attachmentPorts) {
            String ifName = DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.IFNAME);
            if (ifName == null) {
                throw new IllegalStateException("no ifName: " + port.getAbsoluteName());
            }
            if (ifNames.contains(ifName)) {
                toAdd.remove(ifName);
                log().debug("- keep: " + ifName);
            } else {
                removedPorts.add(port);
                log().debug("- remove: " + ifName);
            }
        }
        log().debug("- to-added-ports=" + toAdd);
    }

    public String getEditorName() {
        return this.editorName;
    }

    @Override
    public boolean isConditional() {
        return true;
    }

    protected synchronized Logger log() {
        if (this.log == null) {
            this.log = LoggerFactory.getLogger(this.getClass());
        }
        return this.log;
    }
}