package voss.core.server.util;

import naef.dto.NaefDto;

import java.io.Serializable;

@SuppressWarnings("serial")
public class NaefDiffUnit implements Serializable {
    private final String key;
    private final String absoluteName;
    private final NaefDto dto;

    public NaefDiffUnit(String key, String absoluteName) {
        if (Util.isNull(key, absoluteName)) {
            throw new IllegalArgumentException("null arg is not allowed: key=" + key + ", absoluteName=" + absoluteName);
        }
        this.key = key;
        this.absoluteName = absoluteName;
        this.dto = null;
    }

    public NaefDiffUnit(String key, NaefDto dto) {
        if (Util.isNull(key, dto)) {
            throw new IllegalArgumentException("null arg is not allowed: key=" + key + ", dto=" + dto);
        }
        this.key = key;
        this.absoluteName = null;
        this.dto = dto;
    }

    public String getKey() {
        return this.key;
    }

    public boolean hasAbsoluteName() {
        return this.absoluteName != null;
    }

    public String getRawAbsoluteName() {
        return this.absoluteName;
    }

    public boolean hasDto() {
        return this.dto != null;
    }

    public NaefDto getDto() {
        return this.dto;
    }

    public String getAbsoluteName() {
        if (this.dto != null) {
            return this.dto.getAbsoluteName();
        } else {
            return this.absoluteName;
        }
    }

    @Override
    public boolean equals(Object o) {
        Boolean result = Util.isSame(this, o);
        if (result != null) {
            return result.booleanValue();
        }
        return Util.equals(this.key, ((NaefDiffUnit) o).key);
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("key=").append(this.key);
        if (this.dto != null) {
            sb.append(", dto=").append(this.dto.getAbsoluteName());
        } else {
            sb.append(", absolute-name=").append(this.absoluteName);
        }
        return sb.toString();
    }
}