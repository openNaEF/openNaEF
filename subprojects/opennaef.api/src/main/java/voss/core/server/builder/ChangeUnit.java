package voss.core.server.builder;

import naef.dto.NaefDto;

public interface ChangeUnit {
    void setPublic(boolean value);

    boolean isPublic();

    NaefDto getTarget();

    String getTargetAbsoluteName();

    String getKey();

    boolean isChanged();

    boolean isFor(NaefDto dto);

    String getPreChangedValue();

    String getChangedValue();

    ChangeUnitType getChangeUnitType();

    String simpleToString();

    public static enum ChangeUnitType {
        SIMPLE,
        COLLECTION,
        MAP,;
    }
}