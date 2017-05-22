package voss.model;

import java.util.List;


public interface APS<T extends PhysicalPort> {
    void addMemberPort(T member);

    List<T> getMemberPort();

    void resetMemberPort();

    T getWorkingPort();

    void setWorkingPort(T workingPort);
}