package voss.discovery.runner.simple;

import voss.model.Device;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;


public class ObjectSaveView implements SwitchView {
    private final Device device;

    public ObjectSaveView(Device d) {
        this.device = d;
    }

    @Override
    public void view() {
        ObjectOutputStream oos = null;
        File file = new File(this.device.getDeviceName() + ".bin");
        try {
            if (file.exists()) {
                throw new IllegalStateException("file exist: " + file.getName());
            }
            FileOutputStream fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(this.device);
            System.out.println("saved to " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (Exception e) {
                }
            }
        }
    }

}