package tef;

import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SystemProperties {

    public static class PropertyChangeVetoException extends RuntimeException {

        public PropertyChangeVetoException(String message) {
            super(message);
        }

        public PropertyChangeVetoException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class PropertyChangedEvent {

        public final String propertyName;
        public final String value;

        PropertyChangedEvent(String propertyName, String value) {
            this.propertyName = propertyName;
            this.value = value;
        }
    }

    public static abstract class PropertyNameMatcher {

        public static class Strict extends PropertyNameMatcher {

            private final String targetPropertyName_;

            public Strict(String targetPropertyName) {
                this.targetPropertyName_ = targetPropertyName;
            }

            @Override
            boolean isMatch(String propertyName) {
                return propertyName.equals(targetPropertyName_);
            }
        }

        public static class Loose extends PropertyNameMatcher {

            private final String targetPropertyNameRegexp_;

            public Loose(String targetPropertyNameRegexp) {
                targetPropertyNameRegexp_ = targetPropertyNameRegexp;
            }

            @Override
            boolean isMatch(String propertyName) {
                return propertyName.matches(targetPropertyNameRegexp_);
            }
        }

        abstract boolean isMatch(String propertyName);
    }

    public static interface ChangeListener {

        public static abstract class Sync implements ChangeListener {

            protected Sync() {
            }

            public void enable() {
            }

            public void disable() {
            }

            public void checkValue(PropertyChangedEvent e) throws PropertyChangeVetoException {
            }
        }

        public static abstract class Async extends Thread implements ChangeListener {

            private final BlockingQueue<PropertyChangedEvent> propertyChangedEvents_
                    = new LinkedBlockingQueue<PropertyChangedEvent>();
            private volatile boolean enabled_;

            protected Async() {
            }

            public final void enable() {
                enabled_ = true;
                start();
            }

            public final void disable() {
                enabled_ = false;
                interrupt();
            }

            @Override
            public void run() {
                while (enabled_) {
                    try {
                        propertyChanged(propertyChangedEvents_.take());
                    } catch (InterruptedException ie) {
                    }
                }
            }

            public final void notifyPropertyChanged(PropertyChangedEvent e) {
                propertyChangedEvents_.add(e);
            }

            public void checkValue(PropertyChangedEvent e) throws PropertyChangeVetoException {
            }

            protected abstract void propertyChanged(PropertyChangedEvent e);
        }

        public void enable();

        public void disable();

        public void checkValue(PropertyChangedEvent e) throws PropertyChangeVetoException;

        public void notifyPropertyChanged(PropertyChangedEvent e);
    }

    private final TefService tefService_;
    private final Map<String, String> properties_ = new HashMap<String, String>();
    private final Map<ChangeListener, PropertyNameMatcher> changeListeners_
            = new LinkedHashMap<ChangeListener, PropertyNameMatcher>();

    SystemProperties(TefService tefService) {
        tefService_ = tefService;
    }

    public synchronized void addChangeListener(String propertyName, ChangeListener listener) {
        addChangeListener(new PropertyNameMatcher.Strict(propertyName), listener);
    }

    public synchronized void addChangeListener
            (PropertyNameMatcher propertyNameMatcher, ChangeListener listener) {
        changeListeners_.put(listener, propertyNameMatcher);
        listener.enable();
    }

    public synchronized void removeChangeListener(ChangeListener listener) {
        changeListeners_.remove(listener);
        listener.disable();
    }

    public synchronized void set(String propertyName, String value)
            throws PropertyChangeVetoException {
        List<ChangeListener> listeners = getAdaptiveListeners(propertyName);
        PropertyChangedEvent e = new PropertyChangedEvent(propertyName, value);
        for (ChangeListener listener : listeners) {
            listener.checkValue(e);
        }

        tefService_.logMessage("[system-property]" + propertyName + "\t" + value);
        properties_.put(propertyName, value);

        for (ChangeListener listener : listeners) {
            listener.notifyPropertyChanged(e);
        }
    }

    private List<ChangeListener> getAdaptiveListeners(String propertyName) {
        List<ChangeListener> result = new ArrayList<ChangeListener>();
        for (ChangeListener listener : changeListeners_.keySet()) {
            if (changeListeners_.get(listener).isMatch(propertyName)) {
                result.add(listener);
            }
        }
        return result;
    }

    public synchronized String get(String propertyName) {
        return properties_.get(propertyName);
    }

    public synchronized Set<String> getPropertyNames() {
        return new TreeSet<String>(properties_.keySet());
    }
}
