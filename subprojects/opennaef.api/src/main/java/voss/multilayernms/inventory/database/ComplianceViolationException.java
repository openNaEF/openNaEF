package voss.multilayernms.inventory.database;

import java.util.ArrayList;
import java.util.List;

public class ComplianceViolationException extends Exception {
    private static final long serialVersionUID = 1L;
    private final ArrayList<String> violations = new ArrayList<String>();

    public ComplianceViolationException(String msg) {
        super(msg);
    }

    public ComplianceViolationException(Throwable cause) {
        super(cause);
    }

    public ComplianceViolationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public void addViolation(String msg) {
        this.violations.add(msg);
    }

    public void setViolations(List<String> msg) {
        this.violations.addAll(msg);
    }

    public List<String> getViolations() {
        List<String> result = new ArrayList<String>();
        result.addAll(this.violations);
        return result;
    }

}