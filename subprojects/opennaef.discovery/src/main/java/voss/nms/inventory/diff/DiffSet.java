package voss.nms.inventory.diff;

import voss.core.server.builder.CommandUtil;
import voss.core.server.builder.Commands;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class DiffSet implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sourceSystem;
    private final List<DiffUnit> diffUnits = new ArrayList<DiffUnit>();
    private Date creationDate = new Date();

    public DiffSet(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getSourceSystem() {
        return this.sourceSystem;
    }

    public void setCreationDate(Date d) {
        this.creationDate = d;
    }

    public Date getCreationDate() {
        return this.creationDate;
    }

    public List<DiffUnit> getDiffUnits() {
        Collections.sort(this.diffUnits);
        return this.diffUnits;
    }

    public boolean hasDiffUnit(String name) {
        for (DiffUnit unit : this.diffUnits) {
            if (unit.getInventoryID().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void addDiffUnit(DiffUnit unit) throws IOException, DuplicationException {
        if (hasDiffUnit(unit.getInventoryID())) {
            throw new DuplicationException("duplicated diff-unit:" + unit.getInventoryID());
        }
        this.diffUnits.add(unit);
        for (Commands cmd : unit.getShellCommands()) {
            CommandUtil.logCommands(cmd);
        }
    }

    public void setDiffUnits(List<DiffUnit> units) throws DuplicationException {
        Set<String> read = new HashSet<String>();
        for (DiffUnit unit : units) {
            if (read.contains(unit.getInventoryID())) {
                throw new DuplicationException();
            }
            read.add(unit.getInventoryID());
        }
        this.diffUnits.clear();
        this.diffUnits.addAll(units);
    }

    public void sortDiffUnit() {
        Collections.sort(this.diffUnits);
    }
}