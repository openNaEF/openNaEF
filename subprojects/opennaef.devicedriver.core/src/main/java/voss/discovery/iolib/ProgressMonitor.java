package voss.discovery.iolib;

import voss.discovery.iolib.console.ConsoleException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProgressMonitor {

    public static class ChangeNotifier {

        private List<ChangeObserver> observers = new ArrayList<ChangeObserver>();
        private String currentValue;

        public void addObserver(ChangeObserver observer) {
            this.observers.add(observer);
        }

        public void removeObserver(ChangeObserver observer) {
            this.observers.remove(observer);
        }

        public void notifyChange(String value) {
            this.currentValue = value;
            for (ChangeObserver observer : this.observers) {
                observer.update(value);
            }
        }

        public String getCurrentValue() {
            return this.currentValue;
        }
    }

    public interface ChangeObserver {
        public void update(String value);
    }

    private static ChangeNotifier notifier = new ChangeNotifier();
    private Set<Access> accesses = new HashSet<Access>();
    private boolean running = true;
    private boolean abortable = false;

    public void start() {
        this.running = true;
    }

    public void abort() {
        this.running = false;
    }

    public boolean isRunning() {
        return this.running;
    }

    public void addAccess(Access access) {
        this.accesses.add(access);
    }

    public void setAbortable(boolean abortable) {
        this.abortable = abortable;
    }

    public void abortForcely() throws IOException, ConsoleException {
        if (!abortable) {
            return;
        }
        for (Access access : this.accesses) {
            access.close();
        }
    }

    public static void message(String message) {
        notifier.notifyChange(message);
    }

    public static void addMessageObserver(ChangeObserver messageObserver) {
        notifier.addObserver(messageObserver);
    }

    public static void removeMessageObserver(ChangeObserver messageObserver) {
        notifier.removeObserver(messageObserver);
    }
}