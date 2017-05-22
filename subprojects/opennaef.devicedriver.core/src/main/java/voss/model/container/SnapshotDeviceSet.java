package voss.model.container;

import voss.model.Description;
import voss.model.Device;

import java.io.Serializable;


@SuppressWarnings("serial")
public class SnapshotDeviceSet implements Serializable {
    public final Description description;
    public final Device revision1Device;
    public final Device revision2Device;

    public SnapshotDeviceSet(Description descr, Device revision1Device, Device revision2Device) {
        this.description = descr;
        this.revision1Device = revision1Device;
        this.revision2Device = revision2Device;
    }
}