package voss.nms.inventory.diff;

import voss.core.server.builder.ChangeUnit;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.builder.Commands;
import voss.nms.inventory.builder.conditional.ConditionalCommands;

import java.io.Serializable;
import java.util.*;

public class DiffUnit implements Serializable, Comparable<DiffUnit> {
    private static final long serialVersionUID = 1L;
    private final String mvoID;
    private final String inventoryID;
    private final Set<Diff> diffs = new HashSet<Diff>();
    private final DiffOperationType opType;
    private final List<Commands> commands = new ArrayList<Commands>();
    private DiffStatus status = null;
    private String nodeName;
    private String localName;
    private String typeName;
    private String description;
    private String resultDescription;
    private String sourceSystem;
    private int depth;

    public DiffUnit(String inventoryID, DiffOperationType opType) {
        if (opType == null) {
            throw new IllegalArgumentException();
        }
        this.mvoID = null;
        this.inventoryID = inventoryID;
        this.opType = opType;
    }

    public DiffUnit(String mvoID, String inventoryID, DiffOperationType opType) {
        this.mvoID = mvoID;
        this.inventoryID = inventoryID;
        this.opType = opType;
    }

    public String getMvoID() {
        return this.mvoID;
    }

    public String getInventoryID() {
        return this.inventoryID;
    }

    public Commands[] getShellCommands() {
        return this.commands.toArray(new Commands[0]);
    }

    public void addShellCommands(Commands command) {
        this.commands.add(command);
    }

    public void addBuilder(CommandBuilder builder) {
        addShellCommands(builder.getCommand());
        addDiffs(builder);
    }

    public void addDiffs(ConditionalCommands<?> command) {
        Map<String, String> befores = command.getPrechangeAttributes();
        Map<String, String> afters = command.getAttributes();
        for (String key : befores.keySet()) {
            String before = befores.get(key);
            String after = afters.get(key);
            Diff diff = new Diff(key, before, after);
            this.diffs.add(diff);
        }
    }

    public void addDiffs(CommandBuilder builder) {
        for (ChangeUnit unit : builder.getChangeUnits()) {
            if (!unit.isPublic()) {
                continue;
            }
            Diff diff = new Diff(unit);
            this.diffs.add(diff);
            diff.setParentUnit(this);
        }
    }

    public void addDiff(Diff diff) throws DuplicationException {
        if (diff == null) {
            return;
        }
        if (this.diffs.contains(diff)) {
            throw new DuplicationException();
        }
        this.diffs.add(diff);
        diff.setParentUnit(this);
    }

    public Set<Diff> getDiffs() {
        return this.diffs;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DiffStatus getStatus() {
        return status;
    }

    public void setStatus(DiffStatus status) {
        this.status = status;
    }

    public String getResultDescription() {
        String msg = this.resultDescription;
        if (msg != null) {
            msg = msg.replaceAll("オブジェクトがみつかりません", "Object Not Found");
        }
        return msg;
    }

    public void setResultDescription(String resultDescription) {
        this.resultDescription = resultDescription;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public DiffOperationType getOpType() {
        return opType;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int compareTo(DiffUnit other) {
        if (other == null) {
            return -1;
        } else if (other == this) {
            return 0;
        }
        int diff = getOpValue(this.opType) - getOpValue(other.opType);
        if (diff == 0) {
            if (this.opType == DiffOperationType.ADD || this.opType == DiffOperationType.UPDATE) {
                diff = this.depth - other.depth;
            } else {
                diff = other.depth - this.depth;
            }
        }
        return diff;
    }

    private int getOpValue(DiffOperationType type) {
        int opValue = 4;
        if (type == null) {
            throw new IllegalStateException();
        } else if (type == DiffOperationType.UPDATE) {
            opValue = 3;
        } else if (type == DiffOperationType.ADD) {
            opValue = 2;
        } else if (type == DiffOperationType.REMOVE) {
            opValue = 1;
        }
        return opValue;
    }

    public String getDiffContentDescription() {
        StringBuilder sb = new StringBuilder();
        for (Diff diff : this.diffs) {
            sb.append(diff.toString());
            sb.append("; ");
        }
        return sb.toString();
    }
}