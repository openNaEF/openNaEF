package voss.multilayernms.inventory.web.parts;

import naef.dto.PortDto;

import java.io.Serializable;

public class InterfaceContainer implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean enabled = true;
    private boolean checked = false;
    private boolean exist = false;
    private final PortDto port;

    public InterfaceContainer(PortDto p) {
        if (p == null) {
            throw new IllegalArgumentException();
        }
        this.port = p;
    }

    public PortDto getPort() {
        return this.port;
    }

    public boolean isEnabled() {
        if (checked) {
            return true;
        }
        return this.enabled;
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    public boolean isChecked() {
        return this.checked;
    }

    public void setChecked(boolean value) {
        this.checked = value;
    }

    public boolean isExist() {
        return this.exist;
    }

    public void setExist(boolean value) {
        this.exist = value;
    }

    public boolean isNew() {
        return !this.exist && this.checked;
    }

    public boolean isObsolete() {
        return this.exist && !this.checked;
    }
}