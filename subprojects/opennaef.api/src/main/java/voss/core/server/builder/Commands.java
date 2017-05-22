package voss.core.server.builder;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public interface Commands extends Serializable {
    List<String> getCommands() throws IOException;

    List<String> getRawCommands() throws IOException;

    List<String> getAssertions() throws IOException;

    void addWarning(String warning);

    List<String> getWarnings();

    void setVersionCheck(boolean versionCheckRequired);

    void setValueCheck(boolean versionCheckRequired);

    void setValueCheckContents(List<ChangeUnit> changeUnits);

    boolean isConditional();

    void evaluate();
}